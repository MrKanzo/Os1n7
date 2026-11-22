package com.mrkanzo.os1n7

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Immediately visible, self-contained glowing OSINT tree on BOTH the home page
 * and inside the OSINT Tree screen. Always renders a complete static illustration
 * even if Android accessibility settings turn all animator scales off.
 */
class CyberTreeHeroView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cyan = Color.rgb(70, 230, 245)
    private val neon = Color.rgb(96, 240, 176)
    private val navy = Color.rgb(11, 27, 41)
    private val turquoise = Color.rgb(28, 125, 142)
    private val white = Color.WHITE
    private var animator: ValueAnimator? = null
    private var phase = 0f
    /** Optional selection handler for the six glowing navigation nodes. */
    var onBranchSelected: ((Int) -> Unit)? = null
    private val branches = listOf(
        Branch(0.16f, 0.24f, "ПОШУК", neon),
        Branch(0.82f, 0.21f, "МЕРЕЖІ", cyan),
        Branch(0.12f, 0.49f, "ПРОФІЛІ", cyan),
        Branch(0.85f, 0.49f, "ЗАХИСТ", neon),
        Branch(0.18f, 0.73f, "АНАЛІЗ", neon),
        Branch(0.81f, 0.75f, "ПРАВО", cyan)
    )
    private data class Branch(val x: Float, val y: Float, val name: String, val color: Int)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = (256f * resources.displayMetrics.density).toInt()
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
            resolveSize(h, heightMeasureSpec))
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startMotion()
    }
    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startMotion()
        else stopMotion()
    }
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) startMotion() else stopMotion()
    }
    private fun stopMotion() {
        animator?.cancel()
        animator = null
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val px = event.x * 360f / width.coerceAtLeast(1)
                val py = event.y * 256f / height.coerceAtLeast(1)
                val selected = branches.indices.minByOrNull { i ->
                    val b = branches[i]
                    hypot(px - b.x * 360f, py - b.y * 256f)
                }
                if (selected != null) {
                    val b = branches[selected]
                    if (hypot(px - b.x * 360f, py - b.y * 256f) <= 37f &&
                        onBranchSelected != null) {
                        onBranchSelected?.invoke(selected)
                        return true
                    }
                }
                return performClick()
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
    private fun startMotion() {
        animator?.cancel()
        animator = null
        if (!isAttachedToWindow || !isShown || !ValueAnimator.areAnimatorsEnabled()) {
            phase = 0f
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3600L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun setColor(color: Int, width: Float = 1f, stroke: Boolean = false, alpha: Int = 255) {
        ink.shader = null
        ink.color = color
        ink.alpha = alpha
        ink.style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
        ink.strokeWidth = width
        ink.strokeCap = Paint.Cap.ROUND
        ink.strokeJoin = Paint.Join.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w < 5f || h < 5f) return
        val unit = min(w / 360f, h / 256f)
        canvas.save()
        canvas.scale(w / 360f, h / 256f)
        drawBackground(canvas)
        drawTree(canvas, unit)
        drawText(canvas)
        canvas.restore()
    }

    private fun drawBackground(canvas: Canvas) {
        ink.style = Paint.Style.FILL
        ink.color = navy
        ink.shader = LinearGradient(0f, 0f, 360f, 256f,
            intArrayOf(Color.rgb(10, 32, 47), Color.rgb(12, 20, 35), Color.rgb(7, 46, 49)),
            null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(0f, 0f, 360f, 256f), 22f, 22f, ink)
        ink.shader = null
        setColor(turquoise, 0.7f, true, 64)
        for (i in 1..11) canvas.drawLine(i * 30f, 5f, i * 30f, 252f, ink)
        for (i in 1..8) canvas.drawLine(4f, i * 29f, 355f, i * 29f, ink)
        setColor(cyan, 1.3f, true, 150)
        canvas.drawRoundRect(RectF(3f, 3f, 357f, 253f), 20f, 20f, ink)
        for (i in 0..22) {
            val sx = (i * 71 % 333 + 12).toFloat()
            val sy = (i * 47 % 228 + 12).toFloat()
            val flicker = (0.7f + 0.3f * sin(2.0 * PI * (phase + i * 0.09f)).toFloat())
            setColor(if (i % 3 == 0) neon else cyan, alpha = (95 * flicker).toInt())
            canvas.drawCircle(sx, sy, if (i % 4 == 0) 1.8f else 0.9f, ink)
        }
    }

    private fun branchPath(index: Int): Path {
        val node = branches[index]
        val x = node.x * 360f
        val y = node.y * 256f
        val fromY = 143f + (y - 126f) * 0.17f
        return Path().apply {
            moveTo(178f, fromY)
            cubicTo(180f + (x - 180f) * 0.12f, fromY - 44f,
                x + (180f - x) * 0.12f, y + 37f, x, y)
        }
    }

    private fun drawTree(canvas: Canvas, unit: Float) {
        // Trunk and roots: multiple strokes create a glow without costly bitmap shaders.
        val trunk = Path().apply {
            moveTo(176f, 239f)
            cubicTo(176f, 212f, 171f, 178f, 180f, 133f)
            cubicTo(186f, 101f, 179f, 82f, 180f, 54f)
        }
        for ((thickness, alpha) in listOf(12f to 42, 7f to 86, 2.6f to 255)) {
            setColor(neon, thickness, true, alpha)
            canvas.drawPath(trunk, ink)
        }
        branches.forEachIndexed { index, branch ->
            val path = branchPath(index)
            for ((thickness, alpha) in listOf(11f to 27, 6f to 60, 2f to 220)) {
                setColor(branch.color, thickness, true, alpha)
                canvas.drawPath(path, ink)
            }
            val x = branch.x * 360f
            val y = branch.y * 256f
            // Large bright category hubs are visible even with motion disabled.
            setColor(branch.color, alpha = 45)
            canvas.drawCircle(x, y, 22f, ink)
            setColor(branch.color, alpha = 110)
            canvas.drawCircle(x, y, 16f, ink)
            setColor(Color.rgb(12, 34, 42))
            canvas.drawCircle(x, y, 12.5f, ink)
            setColor(branch.color)
            canvas.drawCircle(x, y, 10f, ink)
            setColor(white)
            canvas.drawCircle(x, y, 3f, ink)
            // Electric signal moves along the Bezier branch.
            val t = (phase + index * 0.23f) % 1f
            val p = cubicPoint(index, t)
            setColor(branch.color, alpha = 60)
            canvas.drawCircle(p.first, p.second, 10f, ink)
            setColor(white, alpha = 230)
            canvas.drawCircle(p.first, p.second, 3.5f, ink)
        }
        for (i in 0..4) {
            val v = ((phase + i * 0.21f) % 1f)
            val y = 235f - v * 165f
            val glow = 5f + 3f * sin(2.0 * PI * v).toFloat()
            setColor(cyan, alpha = 65)
            canvas.drawCircle(179f, y, glow * 2.1f, ink)
            setColor(white)
            canvas.drawCircle(179f, y, 3.1f, ink)
        }
        // Bright central glass orb, pulsing around its border.
        val pulse = if (ValueAnimator.areAnimatorsEnabled())
            sin(2.0 * PI * phase).toFloat() * 2f else 0f
        setColor(cyan, alpha = 34)
        canvas.drawCircle(180f, 128f, 47f + pulse, ink)
        setColor(neon, 3.2f, true)
        canvas.drawCircle(180f, 128f, 43f + pulse, ink)
        setColor(Color.rgb(12, 36, 45))
        canvas.drawCircle(180f, 128f, 40f, ink)
        setColor(cyan, 1.5f, true, 180)
        canvas.drawCircle(180f, 128f, 34f, ink)
    }

    private fun cubicPoint(index: Int, t: Float): Pair<Float, Float> {
        val b = branches[index]
        val x = b.x * 360f
        val y = b.y * 256f
        val sy = 143f + (y - 126f) * 0.17f
        val k = 1f - t
        val bx = k * k * k * 178f +
            3f * k * k * t * (180f + (x - 180f) * 0.12f) +
            3f * k * t * t * (x + (180f - x) * 0.12f) + t * t * t * x
        val by = k * k * k * sy +
            3f * k * k * t * (sy - 44f) +
            3f * k * t * t * (y + 37f) + t * t * t * y
        return bx to by
    }

    private fun drawText(canvas: Canvas) {
        setColor(white)
        ink.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        ink.textAlign = Paint.Align.CENTER
        ink.textSize = 20f
        canvas.drawText("OSINT", 180f, 125f, ink)
        ink.textSize = 8.2f
        ink.color = neon
        canvas.drawText("LIVE  NETWORK", 180f, 138f, ink)
        ink.typeface = Typeface.DEFAULT_BOLD
        ink.textSize = 9.7f
        branches.forEachIndexed { i, branch ->
            val x = branch.x * 360f
            val y = branch.y * 256f
            val labelY = if (i in 0..1) y - 30f else y + 32f
            setColor(white, alpha = 240)
            canvas.drawText(branch.name, x, labelY, ink)
        }
        setColor(neon, alpha = 200)
        ink.textSize = 9f
        canvas.drawText("13 НАПРЯМКІВ  •  НАТИСНИ НА КАРТУ", 180f, 248f, ink)
        ink.textAlign = Paint.Align.LEFT
    }
}
