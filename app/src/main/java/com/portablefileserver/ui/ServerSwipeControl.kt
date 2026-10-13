package com.portablefileserver.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import com.portablefileserver.server.ServerRuntime
import kotlin.math.max
import kotlin.math.min

/** منزلق سحب باتجاه اليسار لتشغيل الخادم أو إيقافه. */
class ServerSwipeControl(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shape = Path()
    private val arrowPath = Path()
    private val bounds = RectF()
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF7F7FF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = dp(2.2f)
    }

    private var serverState = ServerRuntime.State.STOPPED
    private var dragProgress = 0f
    private var dragging = false
    private var actionPending = false
    var onAction: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        contentDescription = "اسحب من اليمين إلى اليسار لبدء مشاركة الملفات"
    }

    fun updateState(state: ServerRuntime.State) {
        if (serverState != state) {
            serverState = state
            dragProgress = 0f
            dragging = false
            actionPending = false
            contentDescription = when (state) {
                ServerRuntime.State.RUNNING -> "اسحب من اليمين إلى اليسار لإيقاف مشاركة الملفات"
                ServerRuntime.State.STARTING -> "جارٍ تجهيز شبكة مشاركة الملفات"
                ServerRuntime.State.ERROR -> "اسحب من اليمين إلى اليسار لإعادة المحاولة"
                ServerRuntime.State.STOPPED -> "اسحب من اليمين إلى اليسار لبدء مشاركة الملفات"
            }
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = dp(78f).toInt()
        val measuredHeight = resolveSize(desiredHeight, heightMeasureSpec)
        val measuredWidth = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val inset = dp(4f)
        bounds.set(inset, inset, width.toFloat() - inset, height.toFloat() - inset)
        val radius = bounds.height() * 0.48f
        val corner = floatArrayOf(
            radius, radius, radius * 0.66f, radius * 0.66f,
            radius, radius, radius * 0.66f, radius * 0.66f
        )
        shape.reset()
        shape.addRoundRect(bounds, corner, Path.Direction.CW)

        val colors = stateColors()
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = colors.first()
        canvas.drawPath(shape, paint)

        if (dragProgress > 0f) {
            val minX = bounds.left + radius
            val maxX = bounds.right - radius
            val thumbX = maxX - dragProgress * (maxX - minX)
            canvas.save()
            canvas.clipPath(shape)
            paint.color = 0x2EFFFFFF
            canvas.drawRect(thumbX, bounds.top, bounds.right, bounds.bottom, paint)
            canvas.restore()
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1f)
        paint.color = 0x66FFFFFF
        canvas.drawPath(shape, paint)
        paint.style = Paint.Style.FILL

        val minX = bounds.left + radius
        val maxX = bounds.right - radius
        val thumbX = maxX - dragProgress * (maxX - minX)
        val centerY = bounds.centerY()
        val thumbRadius = radius * 0.76f

        paint.color = 0x35FFFFFF
        canvas.drawCircle(thumbX, centerY, thumbRadius + dp(3f), paint)
        paint.color = 0xFFF6F5FF.toInt()
        canvas.drawCircle(thumbX, centerY, thumbRadius, paint)

        arrowPaint.color = colors.last()
        arrowPath.reset()
        arrowPath.moveTo(thumbX + dp(4f), centerY - dp(7f))
        arrowPath.lineTo(thumbX - dp(4f), centerY)
        arrowPath.lineTo(thumbX + dp(4f), centerY + dp(7f))
        canvas.drawPath(arrowPath, arrowPaint)

        val label = when {
            serverState == ServerRuntime.State.STARTING -> "جارٍ تجهيز الشبكة…"
            actionPending -> "جارٍ تنفيذ الطلب…"
            serverState == ServerRuntime.State.RUNNING -> "اسحب يساراً لإيقاف الخادم"
            serverState == ServerRuntime.State.ERROR -> "اسحب يساراً لإعادة المحاولة"
            else -> "اسحب من اليمين لليسار للبدء"
        }
        val available = max(dp(80f), bounds.width() - thumbRadius * 2 - dp(32f))
        val fontSize = min(dp(15f), bounds.height() * 0.25f)
        labelPaint.textSize = fontSize
        while (labelPaint.measureText(label) > available && labelPaint.textSize > dp(10f)) {
            labelPaint.textSize -= dp(0.5f)
        }
        labelPaint.alpha = if (serverState == ServerRuntime.State.STARTING) 190 else 255
        canvas.drawTextRun(
            label, 0, label.length, 0, label.length, bounds.centerX() - dp(4f),
            centerY - (labelPaint.ascent() + labelPaint.descent()) / 2f,
            true, labelPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (serverState == ServerRuntime.State.STARTING || actionPending) return false
        val inset = dp(4f)
        val radius = (height.toFloat() - inset * 2f) * 0.48f
        val minX = inset + radius
        val maxX = width.toFloat() - inset - radius

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < maxX - radius * 1.25f) return false
                dragging = true
                dragProgress = ((maxX - event.x) / max(1f, maxX - minX)).coerceIn(0f, 1f)
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                dragProgress = ((maxX - event.x) / max(1f, maxX - minX)).coerceIn(0f, 1f)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) return false
                dragging = false
                val complete = dragProgress >= 0.62f
                if (complete) {
                    actionPending = true
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    performClick()
                    postDelayed({
                        actionPending = false
                        dragProgress = 0f
                        invalidate()
                    }, 1800L)
                } else {
                    dragProgress = 0f
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragging = false
                dragProgress = 0f
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onAction?.invoke()
        return true
    }

    private fun stateColors(): IntArray = if (actionPending) {
        intArrayOf(0xFF9B6519.toInt(), 0xFF7C4F11.toInt())
    } else when (serverState) {
        ServerRuntime.State.RUNNING -> intArrayOf(0xFF187B5B.toInt(), 0xFF116044.toInt())
        ServerRuntime.State.STARTING -> intArrayOf(0xFF9B6519.toInt(), 0xFF7C4F11.toInt())
        ServerRuntime.State.ERROR -> intArrayOf(0xFFB83D49.toInt(), 0xFF8F2D39.toInt())
        ServerRuntime.State.STOPPED -> intArrayOf(0xFF137B77.toInt(), 0xFF0F6663.toInt())
    }
}
