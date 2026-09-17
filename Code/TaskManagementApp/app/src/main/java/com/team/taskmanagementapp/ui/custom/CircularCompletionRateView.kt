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

/**
 * Custom Donut Progress Indicator for Completion Rate.
 * Displays circular background track, animated active sweep arc, and centered percentage.
 * Matching the Stitch TaskFlow UI/UX design.
 */
class CircularCompletionRateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentProgress: Int = 0
    private var animatedProgress: Float = 0f
    private var animator: ValueAnimator? = null

    private val strokeWidthPx = dpToPx(10f)
    private val arcBounds = RectF()

    private val trackColor: Int = Color.parseColor("#E8E8E8")
    private val activeColor: Int = ContextCompat.getColor(context, R.color.primary)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }

    private val activeArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = activeColor
        textSize = spToPx(22f)
        textAlign = Paint.Align.CENTER
        try {
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
        } catch (_: Exception) {
            // Fallback
        }
    }

    /**
     * Set progress percentage (0..100) with optional animation.
     */
    fun setProgress(progress: Int, animate: Boolean = true) {
        val target = progress.coerceIn(0, 100)
        if (!animate) {
            currentProgress = target
            animatedProgress = target.toFloat()
            invalidate()
            return
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedProgress, target.toFloat()).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                animatedProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
        currentProgress = target
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (size - strokeWidthPx) / 2f

        if (radius <= 0) return

        arcBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // 1. Draw background track circle
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // 2. Draw active progress sweep arc starting from top (-90 degrees)
        val sweepAngle = (animatedProgress / 100f) * 360f
        if (sweepAngle > 0f) {
            canvas.drawArc(arcBounds, -90f, sweepAngle, false, activeArcPaint)
        }

        // 3. Draw percentage text at the center
        val percentageText = "${Math.round(animatedProgress)}%"
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(percentageText, cx, textY, textPaint)
    }

    private fun dpToPx(dp: Float): Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics
    )

    private fun spToPx(sp: Float): Float = android.util.TypedValue.applyDimension(
        android.util.TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
    )
}
