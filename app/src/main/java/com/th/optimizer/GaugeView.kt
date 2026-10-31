package com.th.optimizer

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.DecelerateInterpolator

class GaugeView(c: Context) : View(c) {
    private var shown = 0f
    private var anim: ValueAnimator? = null
    var score = 0
        set(v) { field = v; anim?.cancel(); anim = ValueAnimator.ofFloat(shown, v.toFloat()).apply { duration = 900; interpolator = DecelerateInterpolator(); addUpdateListener { shown = it.animatedValue as Float; invalidate() }; start() } }
    var scanning = false
        set(v) { field = v; if (v) postInvalidateOnAnimation() else invalidate() }
    var caption = "SAÚDE"
        set(v) { field = v; invalidate() }

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 26f; color = Color.parseColor("#2A0A10"); strokeCap = Paint.Cap.ROUND }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 26f; strokeCap = Paint.Cap.ROUND }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 40f; strokeCap = Paint.Cap.ROUND; color = Color.parseColor("#66FF1744"); maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL) }
    private val scan = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.WHITE; strokeCap = Paint.Cap.ROUND }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 120f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 34f; textAlign = Paint.Align.CENTER; letterSpacing = 0.2f }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        arc.shader = SweepGradient(w / 2f, h / 2f, intArrayOf(Color.parseColor("#7F0020"), Color.parseColor("#FF1744"), Color.parseColor("#FF6D00")), floatArrayOf(0f, 0.45f, 0.75f))
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f; val cy = height / 2f
        val r = RectF(50f, 50f, width - 50f, height - 50f)
        c.drawArc(r, 135f, 270f, false, track)
        c.save(); c.rotate(135f, cx, cy)
        val sw = 270f * shown / 100f
        if (sw > 1f) { c.drawArc(r, 0f, sw, false, glow); c.drawArc(r, 0f, sw, false, arc) }
        c.restore()
        if (scanning) {
            val a = (System.currentTimeMillis() % 1200) / 1200f * 360f
            val inner = RectF(80f, 80f, width - 80f, height - 80f)
            c.drawArc(inner, a, 70f, false, scan)
            postInvalidateOnAnimation()
        }
        c.drawText("${shown.toInt()}", cx, cy + 35f, big)
        c.drawText(caption, cx, cy + 95f, small)
    }
}
