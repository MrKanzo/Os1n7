package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * Small vector mascot: static in classic home, gently animated in live home.
 * No image downloads, heavy assets, background threads or permanent services.
 */
class CompanionOrbView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var timer: ValueAnimator? = null
    private var phase = 0f
    var live: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            updateMotion()
        }
    var skin: String = "neon"
        set(value) {
            field = value
            invalidate()
        }

    // Purely visual evolution. All unlock checks happen in CompanionWardrobe.
    var evolution: Int = 1
        set(value) { field = value.coerceIn(1, 5); invalidate() }
    var headWear: String = "none"
        set(value) { field = value; invalidate() }
    var heldItem: String = "none"
        set(value) { field = value; invalidate() }
    var auraStyle: String = "none"
        set(value) { field = value; invalidate() }

    private val color: Int get() = when (skin) {
        "ghost" -> Color.rgb(149, 193, 255)
        "ember" -> Color.rgb(255, 174, 118)
        else -> Color.rgb(98, 230, 173)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMotion()
    }

    override fun onDetachedFromWindow() {
        timer?.cancel()
        timer = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        updateMotion()
    }

    private fun updateMotion() {
        timer?.cancel()
        timer = null
        phase = 0f
        if (!live || !isAttachedToWindow || !isShown || !ValueAnimator.areAnimatorsEnabled()) {
            invalidate()
            return
        }
        timer = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = minOf(width, height).toFloat()
        if (side <= 0f) return
        canvas.save()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / 64f, side / 64f)

        val hover = if (live) sin(phase.toDouble() * 2.0 * PI).toFloat() * 1.5f else 0f
        val glow = if (live) (0.65f + 0.30f * sin(phase.toDouble() * 2.0 * PI).toFloat()) else 0.70f
        val primary = color
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((42f * glow).toInt(), Color.red(primary), Color.green(primary), Color.blue(primary))
        canvas.drawCircle(32f, 34f + hover, 29f, paint)
        drawAura(canvas, primary, hover)

        paint.color = Color.rgb(13, 32, 43)
        canvas.drawRoundRect(RectF(10f, 13f + hover, 54f, 55f + hover), 17f, 17f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = primary
        canvas.drawRoundRect(RectF(10f, 13f + hover, 54f, 55f + hover), 17f, 17f, paint)
        paint.style = Paint.Style.FILL

        // Two small horns/antennae give the mascot a friendly 'чудо-юдо' silhouette.
        val left = Path().apply {
            moveTo(20f, 15f + hover); lineTo(17f, 6f + hover); lineTo(29f, 13f + hover); close()
        }
        val right = Path().apply {
            moveTo(44f, 15f + hover); lineTo(47f, 6f + hover); lineTo(35f, 13f + hover); close()
        }
        paint.color = primary
        canvas.drawPath(left, paint)
        canvas.drawPath(right, paint)

        paint.color = Color.rgb(5, 21, 32)
        canvas.drawRoundRect(RectF(15f, 23f + hover, 49f, 45f + hover), 9f, 9f, paint)
        paint.color = primary

        val blink = live && phase > 0.72f && phase < 0.78f
        if (blink) {
            paint.strokeWidth = 2f
            canvas.drawLine(21f, 33f + hover, 28f, 33f + hover, paint)
            canvas.drawLine(36f, 33f + hover, 43f, 33f + hover, paint)
        } else {
            canvas.drawRoundRect(RectF(21f, 28f + hover, 28f, 38f + hover), 3f, 3f, paint)
            canvas.drawRoundRect(RectF(36f, 28f + hover, 43f, 38f + hover), 3f, 3f, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.6f
        canvas.drawArc(RectF(28f, 36f + hover, 36f, 43f + hover), 20f, 140f, false, paint)
        paint.style = Paint.Style.FILL
        drawEvolution(canvas, primary, hover)
        drawOutfit(canvas, primary, hover)
        canvas.restore()
    }

    private fun drawAura(canvas: Canvas, primary: Int, hover: Float) {
        if (auraStyle == "none") return
        val alpha = if (live) (105f + 40f * sin(phase.toDouble() * 2.0 * PI)).toInt() else 120
        paint.color = Color.argb(alpha.coerceIn(0,255), Color.red(primary), Color.green(primary), Color.blue(primary))
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        when (auraStyle) {
            "pulse" -> canvas.drawCircle(32f, 33f + hover, 30.5f, paint)
            "orbit" -> {
                canvas.drawOval(RectF(1f, 18f + hover, 63f, 47f + hover), paint)
                canvas.drawCircle(6f, 20f + hover, 2.7f, paint)
            }
            "sparks" -> {
                for ((x, y) in listOf(5f to 11f, 58f to 12f, 4f to 48f, 60f to 49f)) {
                    canvas.drawLine(x-3f, y, x+3f, y, paint)
                    canvas.drawLine(x, y-3f, x, y+3f, paint)
                }
            }
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawEvolution(canvas: Canvas, primary: Int, hover: Float) {
        paint.style = Paint.Style.FILL
        if (evolution >= 2) {
            paint.color = Color.argb(130, Color.red(primary), Color.green(primary), Color.blue(primary))
            canvas.drawCircle(19f, 41f + hover, 1.6f, paint)
            canvas.drawCircle(45f, 41f + hover, 1.6f, paint)
        }
        if (evolution >= 3) {
            paint.color = primary
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.3f
            canvas.drawLine(12f, 32f+hover, 7f, 28f+hover, paint)
            canvas.drawLine(52f, 32f+hover, 57f, 28f+hover, paint)
            paint.style = Paint.Style.FILL
        }
        if (evolution >= 4) {
            paint.color = Color.argb(210, Color.red(primary), Color.green(primary), Color.blue(primary))
            val wingL = Path().apply {
                moveTo(12f, 46f+hover); lineTo(3f, 40f+hover); lineTo(11f, 53f+hover); close()
            }
            val wingR = Path().apply {
                moveTo(52f, 46f+hover); lineTo(61f, 40f+hover); lineTo(53f, 53f+hover); close()
            }
            canvas.drawPath(wingL, paint)
            canvas.drawPath(wingR, paint)
        }
        if (evolution >= 5) {
            paint.color = primary
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.6f
            canvas.drawArc(RectF(10f, 1f+hover, 54f, 22f+hover), 205f, 130f, false, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(32f, 1.5f+hover, 2f, paint)
        }
    }

    private fun drawOutfit(canvas: Canvas, primary: Int, hover: Float) {
        paint.style = Paint.Style.FILL
        when (headWear) {
            "cap" -> {
                paint.color = Color.rgb(30, 74, 86)
                canvas.drawRoundRect(RectF(17f, 12f+hover, 47f, 20f+hover), 5f, 5f, paint)
                paint.color = primary
                canvas.drawRoundRect(RectF(15f, 18f+hover, 52f, 21f+hover), 2f, 2f, paint)
            }
            "visor" -> {
                paint.color = Color.argb(145, Color.red(primary), Color.green(primary), Color.blue(primary))
                canvas.drawRoundRect(RectF(15f, 24f+hover, 49f, 31f+hover), 4f, 4f, paint)
                paint.color = Color.WHITE
                canvas.drawRoundRect(RectF(24f, 26f+hover, 39f, 27.5f+hover), 1f, 1f, paint)
            }
            "hood" -> {
                paint.color = primary
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawArc(RectF(9f, 12f+hover, 55f, 56f+hover), 140f, 260f, false, paint)
                paint.style = Paint.Style.FILL
            }
            "crown" -> {
                paint.color = Color.rgb(244, 196, 93)
                val crown = Path().apply {
                    moveTo(20f, 17f+hover)
                    lineTo(18f, 8f+hover); lineTo(25f, 13f+hover)
                    lineTo(32f, 4f+hover); lineTo(39f, 13f+hover)
                    lineTo(46f, 8f+hover); lineTo(44f, 17f+hover); close()
                }
                canvas.drawPath(crown, paint)
                paint.color = primary
                canvas.drawRoundRect(RectF(20f, 15f+hover, 44f, 18.5f+hover), 2f, 2f, paint)
            }
        }
        // A single small object on the right. Stays readable even at 48dp.
        when (heldItem) {
            "magnifier" -> {
                paint.color = primary
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2.5f
                canvas.drawCircle(48f, 50f+hover, 6.3f, paint)
                canvas.drawLine(52f, 54f+hover, 58f, 61f+hover, paint)
            }
            "badge" -> {
                paint.color = Color.rgb(244, 196, 93)
                canvas.drawCircle(49f, 51f+hover, 7f, paint)
                paint.color = Color.rgb(27, 49, 59)
                paint.textSize = 7f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("✓", 49f, 54f+hover, paint)
            }
            "tablet" -> {
                paint.color = Color.rgb(24, 49, 61)
                canvas.drawRoundRect(RectF(42f, 44f+hover, 59f, 62f+hover), 2.5f, 2.5f, paint)
                paint.style = Paint.Style.STROKE
                paint.color = primary
                paint.strokeWidth = 1.5f
                canvas.drawRoundRect(RectF(42f, 44f+hover, 59f, 62f+hover), 2.5f, 2.5f, paint)
                canvas.drawLine(45f, 50f+hover, 55f, 50f+hover, paint)
            }
            "shield" -> {
                paint.color = Color.rgb(32, 84, 93)
                val shield = Path().apply {
                    moveTo(50f, 43f+hover); lineTo(60f, 47f+hover)
                    lineTo(58f, 56f+hover); lineTo(50f, 62f+hover)
                    lineTo(42f, 56f+hover); lineTo(40f, 47f+hover); close()
                }
                canvas.drawPath(shield, paint)
                paint.color = primary
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.5f
                canvas.drawPath(shield, paint)
                canvas.drawLine(46f, 53f+hover, 49f, 56f+hover, paint)
                canvas.drawLine(49f, 56f+hover, 55f, 49f+hover, paint)
            }
        }
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
    }
}
