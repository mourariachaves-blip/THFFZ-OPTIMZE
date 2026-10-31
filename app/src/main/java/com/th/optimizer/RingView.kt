package com.th.optimizer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.DecelerateInterpolator

class RingView(c: Context) : View(c) {
    private val d = resources.displayMetrics.density
    private var shown = 0f
    private var anim: ValueAnimator? = null
    var progress = 0
        set(v) { field = v; anim?.cancel(); anim = ValueAnimator.ofFloat(shown, v.toFloat()).apply { duration = 900; interpolator = DecelerateInterpolator(); addUpdateListener { shown = it.animatedValue as Float; invalidate() }; start() } }
    var centerText = ""
        set(v) { field = v; invalidate() }
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5 * d; color = Color.parseColor("#2A0A10") }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5 * d; color = Color.parseColor("#FF1744"); strokeCap = Paint.Cap.ROUND }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8 * d; color = Color.parseColor("#66FF1744"); maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL) }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 15 * d; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(c: Canvas) {
        val p = 6 * d
        val r = RectF(p, p, width - p, height - p)
        c.drawArc(r, -90f, 360f, false, track)
        if (shown > 0.5f) { c.drawArc(r, -90f, 360f * shown / 100f, false, glow); c.drawArc(r, -90f, 360f * shown / 100f, false, arc) }
        c.drawText(centerText, width / 2f, height / 2f + txt.textSize / 3f, txt)
    }
}
