package com.base.find_phone_clap_detector.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.base.find_phone_clap_detector.R

class SeekArc : View {

    interface OnSeekArcChangeListener {
        fun onProgressChanged(seekArc: SeekArc?, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(seekArc: SeekArc?)
        fun onStopTrackingTouch(seekArc: SeekArc?)
    }

    private var mOnSeekArcChangeListener: OnSeekArcChangeListener? = null

    var max = 100
    private var mProgress = 0

    private var mArcWidth = 8f
    private var mProgressWidth = 8f

    private var mEnabled = true
    private var useGradient = true

    private var mArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mThumb: Drawable? = null

    private var lineStartX = 0f
    private var lineEndX = 0f
    private var centerY = 0f

    private var progressColor = Color.parseColor("#7F00FF")

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        val res = resources
        mThumb = res.getDrawable(R.drawable.thumb)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SeekArc)

            max = a.getInt(R.styleable.SeekArc_max, 100)
            mProgress = a.getInt(R.styleable.SeekArc_progress, 0)

            mArcWidth = a.getDimension(R.styleable.SeekArc_arcWidth, 8f)
            mProgressWidth = a.getDimension(R.styleable.SeekArc_progressWidth, 8f)

            mEnabled = a.getBoolean(R.styleable.SeekArc_enabled, true)

            progressColor = a.getColor(
                R.styleable.SeekArc_progressColor,
                Color.parseColor("#7F00FF")
            )

            val arcColor = a.getColor(
                R.styleable.SeekArc_arcColor,
                Color.LTGRAY
            )

            useGradient = true   // default true (you can add custom attr if needed)

            mArcPaint.color = arcColor

            a.recycle()
        }

        mArcPaint.style = Paint.Style.STROKE
        mArcPaint.strokeWidth = mProgressWidth
        mArcPaint.strokeCap = Paint.Cap.ROUND

        mProgressPaint.style = Paint.Style.STROKE
        mProgressPaint.strokeWidth = mProgressWidth
        mProgressPaint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        lineStartX = paddingLeft.toFloat()
        lineEndX = (w - paddingRight).toFloat()
        centerY = h / 2f

        if (useGradient) {
            mProgressPaint.shader = LinearGradient(
                lineStartX, 0f,
                lineEndX, 0f,
                Color.parseColor("#7F00FF"),
                Color.parseColor("#B800D0"),
                Shader.TileMode.CLAMP
            )
        } else {
            mProgressPaint.shader = null
            mProgressPaint.color = progressColor
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // background line
        canvas.drawLine(lineStartX, centerY, lineEndX, centerY, mArcPaint)

        // progress line
        val progressX = lineStartX + (lineEndX - lineStartX) * mProgress / max
        canvas.drawLine(lineStartX, centerY, progressX, centerY, mProgressPaint)

        // thumb
        mThumb?.let {
            val halfW = it.intrinsicWidth / 2
            val halfH = it.intrinsicHeight / 2
            it.setBounds(
                (progressX - halfW).toInt(),
                (centerY - halfH).toInt(),
                (progressX + halfW).toInt(),
                (centerY + halfH).toInt()
            )
            it.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mEnabled) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mOnSeekArcChangeListener?.onStartTrackingTouch(this)
                updateOnTouch(event.x)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateOnTouch(event.x)
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                mOnSeekArcChangeListener?.onStopTrackingTouch(this)
                return true
            }
        }
        return false
    }

    private fun updateOnTouch(x: Float) {
        val clampedX = x.coerceIn(lineStartX, lineEndX)
        val progress = ((clampedX - lineStartX) / (lineEndX - lineStartX) * max).toInt()
        updateProgress(progress, true)
    }

    private fun updateProgress(progress: Int, fromUser: Boolean) {
        mProgress = progress.coerceIn(0, max)
        mOnSeekArcChangeListener?.onProgressChanged(this, mProgress, fromUser)
        invalidate()
    }

    fun setOnSeekArcChangeListener(listener: OnSeekArcChangeListener?) {
        mOnSeekArcChangeListener = listener
    }

    var progress: Int
        get() = mProgress
        set(value) {
            updateProgress(value, false)
        }

    override fun isEnabled(): Boolean = mEnabled

    override fun setEnabled(enabled: Boolean) {
        mEnabled = enabled
        invalidate()
    }
}