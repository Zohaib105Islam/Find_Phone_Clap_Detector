package com.base.find_phone_clap_detector.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

class AnimatedChevronView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 3f * resources.displayMetrics.density
    }
    private val path = Path()
    private val chevronColors = intArrayOf(
        Color.parseColor("#BFDBFE"),
        Color.parseColor("#60A5FA"),
        Color.parseColor("#2563EB")
    )
    private var animationPhase = 0f

    private val animator = ValueAnimator.ofFloat(0f, 3f).apply {
        duration = 1100L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animationPhase = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val chevronHeight = height * 0.52f
        val chevronWidth = width * 0.16f
        val gap = width * 0.08f
        val groupWidth = chevronWidth * 3 + gap * 2
        val startX = (width - groupWidth) / 2f
        val centerY = height / 2f

        repeat(3) { index ->
            val pulse = ((sin((animationPhase - index) * 2f * PI / 3f) + 1f) / 2f).toFloat()
            paint.color = chevronColors[index]
            paint.alpha = (75 + pulse * 180).toInt()

            val x = startX + index * (chevronWidth + gap)
            path.reset()
            path.moveTo(x, centerY - chevronHeight / 2f)
            path.lineTo(x + chevronWidth, centerY)
            path.lineTo(x, centerY + chevronHeight / 2f)
            canvas.drawPath(path, paint)
        }
    }
}
