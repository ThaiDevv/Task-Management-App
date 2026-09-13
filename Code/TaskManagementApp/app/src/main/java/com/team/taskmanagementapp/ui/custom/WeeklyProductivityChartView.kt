package com.team.taskmanagementapp.ui.custom

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.team.taskmanagementapp.R
import com.team.taskmanagementapp.data.model.stats.DayProductivity
import com.team.taskmanagementapp.data.model.stats.WeeklyProductivity

/**
 * Custom Bar Chart View for Weekly Productivity.
 * Renders 7 vertical rounded bars, horizontal grid lines, and axes labels.
 * Matching the Stitch TaskFlow UI/UX design.
 */
class WeeklyProductivityChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var days: List<DayProductivity> = listOf(
        DayProductivity("Mon", 0, 0),
        DayProductivity("Tue", 0, 0),
        DayProductivity("Wed", 0, 0),
        DayProductivity("Thu", 0, 0),
        DayProductivity("Fri", 0, 0),
        DayProductivity("Sat", 0, 0),
        DayProductivity("Sun", 0, 0)
    )
    private var maxScale: Int = 12
    private var animFraction: Float = 1f
    private var animator: ValueAnimator? = null

    // Colors
    private val barColor: Int = ContextCompat.getColor(context, R.color.primary)
    private val gridLineColor: Int = Color.parseColor("#EBEBEB")
    private val labelTextColor: Int = Color.parseColor("#737786")

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = barColor
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gridLineColor
        strokeWidth = dpToPx(1f)
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelTextColor
        textSize = spToPx(11f)
        textAlign = Paint.Align.CENTER
        try {
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
        } catch (_: Exception) {
            // Fallback to default
        }
    }

    private val yAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelTextColor
        textSize = spToPx(11f)
        textAlign = Paint.Align.LEFT
        try {
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
        } catch (_: Exception) {
            // Fallback to default
        }
    }

    private val barRect = RectF()

    /**
     * Update chart data with smooth animation.
     */
    fun setData(productivity: WeeklyProductivity) {
        if (productivity.days.isNotEmpty()) {
            this.days = productivity.days
            this.maxScale = maxOf(12, productivity.maxCount)
        }
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                animFraction = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val leftPadding = dpToPx(24f)
        val rightPadding = dpToPx(12f)
        val topPadding = dpToPx(16f)
        val bottomPadding = dpToPx(24f)

        val chartLeft = paddingLeft + leftPadding
        val chartRight = width - paddingRight - rightPadding
        val chartTop = paddingTop + topPadding
        val chartBottom = height - paddingBottom - bottomPadding

        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        if (chartWidth <= 0 || chartHeight <= 0) return

        // 1. Draw 4 horizontal grid lines and Y-axis labels (0, 4, 8, 12, or dynamic steps)
        val steps = 3 // 0, 1/3, 2/3, 1
        for (i in 0..steps) {
            val fraction = i.toFloat() / steps
            val y = chartBottom - (fraction * chartHeight)
            val value = Math.round(fraction * maxScale)

            // Horizontal grid line
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)

            // Y-axis label (positioned slightly to the left of chartLeft)
            canvas.drawText(value.toString(), paddingLeft.toFloat(), y + dpToPx(4f), yAxisPaint)
        }

        // 2. Draw 7 vertical bars and X-axis day labels
        val barCount = days.size
        if (barCount == 0) return

        val slotWidth = chartWidth / barCount
        val barWidth = minOf(dpToPx(18f), slotWidth * 0.45f)
        val cornerRadius = dpToPx(4f)

        for (i in days.indices) {
            val day = days[i]
            val centerX = chartLeft + (i + 0.5f) * slotWidth

            // Draw Bar
            val countRatio = (day.completedCount.toFloat() / maxScale).coerceIn(0f, 1f)
            val barHeight = countRatio * chartHeight * animFraction

            if (barHeight > 0) {
                barRect.set(
                    centerX - (barWidth / 2),
                    chartBottom - barHeight,
                    centerX + (barWidth / 2),
                    chartBottom
                )
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }

            // Draw X-axis label
            canvas.drawText(
                day.dayName,
                centerX,
                chartBottom + dpToPx(16f),
                textPaint
            )
        }
    }

    private fun dpToPx(dp: Float): Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics
    )

    private fun spToPx(sp: Float): Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
    )
}
