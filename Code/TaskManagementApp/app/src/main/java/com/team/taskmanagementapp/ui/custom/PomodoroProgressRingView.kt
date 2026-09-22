package com.team.taskmanagementapp.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.team.taskmanagementapp.R

/**
 * Vòng tròn tiến độ cho Pomodoro Timer.
 *
 * View này **thuần hiển thị**: nó không đếm giờ, không tự cập nhật, không giữ timer.
 * Mỗi khi Fragment nhận state mới từ `PomodoroTimerController` (được Service tick mỗi giây)
 * thì gọi [setRemainingFraction] — nhờ vậy UI không bao giờ có nguồn thời gian thứ hai.
 */
class PomodoroProgressRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Tỉ lệ vòng cung còn lại được vẽ (0f..1f). */
    private var remainingFraction: Float = 0f

    private var ringColor: Int = ContextCompat.getColor(context, R.color.primary)

    private val strokeWidthPx = dpToPx(DEFAULT_STROKE_DP)
    private val bounds = RectF()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = DEFAULT_TRACK_COLOR
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ringColor
    }

    /**
     * @param fraction tỉ lệ vòng tròn cần vẽ, 0f = cạn hết, 1f = đầy.
     * Dùng "phần còn lại" để vòng tròn cạn dần theo countdown.
     */
    fun setRemainingFraction(fraction: Float) {
        val target = fraction.coerceIn(0f, 1f)
        if (target == remainingFraction) return
        remainingFraction = target
        invalidate()
    }

    /** Đổi màu vòng tiến độ theo loại phiên (Focus / nghỉ ngắn / nghỉ dài). */
    fun setRingColor(color: Int) {
        if (color == ringColor) return
        ringColor = color
        ringPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        // Lấy hình vuông lớn nhất nằm giữa view, trừ nửa nét vẽ để vòng không bị cắt.
        val size = minOf(width, height).toFloat() - strokeWidthPx
        val left = (width - size) / 2f
        val top = (height - size) / 2f
        bounds.set(left, top, left + size, top + size)

        canvas.drawArc(bounds, 0f, FULL_CIRCLE_DEGREES, false, trackPaint)

        if (remainingFraction > 0f) {
            // Bắt đầu từ đỉnh (12 giờ) và chạy theo chiều kim đồng hồ.
            canvas.drawArc(
                bounds,
                START_ANGLE_DEGREES,
                FULL_CIRCLE_DEGREES * remainingFraction,
                false,
                ringPaint
            )
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

    private companion object {
        const val DEFAULT_STROKE_DP = 14f
        const val FULL_CIRCLE_DEGREES = 360f
        const val START_ANGLE_DEGREES = -90f
        val DEFAULT_TRACK_COLOR: Int = Color.parseColor("#E2E2E2")
    }
}
