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

        val hover = if (live) sin(phase * 2f * PI).toFloat() * 1.5f else 0f
        val glow = if (live) (0.65f + 0.30f * sin(phase * 2f * PI).toFloat()) else 0.70f
        val primary = color
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((42f * glow).toInt(), Color.red(primary), Color.green(primary), Color.blue(primary))
        canvas.drawCircle(32f, 34f + hover, 29f, paint)

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
        canvas.restore()
    }
}
