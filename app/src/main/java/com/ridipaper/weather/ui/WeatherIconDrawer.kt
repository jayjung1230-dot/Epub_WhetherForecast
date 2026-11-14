package com.ridipaper.weather.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.sin

object WeatherIconDrawer {

    fun draw(canvas: Canvas, wmoCode: Int, cx: Float, cy: Float, scale: Float, strokePaint: Paint, fillPaint: Paint) {
        when (wmoCode) {
            0, 1 -> drawSun(canvas, cx, cy, 18f * scale, strokePaint)
            2 -> drawPartlyCloudy(canvas, cx, cy, scale, strokePaint, fillPaint)
            3, 45, 48 -> drawCloud(canvas, cx, cy, scale, strokePaint, fillPaint)
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99 ->
                drawRain(canvas, cx, cy, scale, strokePaint, fillPaint)
            71, 73, 75, 77, 85, 86 ->
                drawSnow(canvas, cx, cy, scale, strokePaint, fillPaint)
            else -> drawPartlyCloudy(canvas, cx, cy, scale, strokePaint, fillPaint)
        }
    }

    private fun drawSun(canvas: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        p.style = Paint.Style.STROKE
        canvas.drawCircle(cx, cy, r, p)
        for (i in 0 until 8) {
            val rad = Math.toRadians(i * 45.0)
            val x1 = cx + (r + 5f * (r / 18f)) * cos(rad).toFloat()
            val y1 = cy + (r + 5f * (r / 18f)) * sin(rad).toFloat()
            val x2 = cx + (r + 13f * (r / 18f)) * cos(rad).toFloat()
            val y2 = cy + (r + 13f * (r / 18f)) * sin(rad).toFloat()
            canvas.drawLine(x1, y1, x2, y2, p)
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, s: Float, stroke: Paint, fill: Paint) {
        fill.color = Color.WHITE
        fill.style = Paint.Style.FILL
        stroke.style = Paint.Style.STROKE

        val r1 = RectF(cx - 32f * s, cy - 10f * s, cx - 2f * s, cy + 18f * s)
        val r2 = RectF(cx - 16f * s, cy - 24f * s, cx + 18f * s, cy + 10f * s)
        val r3 = RectF(cx + 4f * s, cy - 12f * s, cx + 32f * s, cy + 18f * s)

        canvas.drawOval(r1, fill)
        canvas.drawOval(r2, fill)
        canvas.drawOval(r3, fill)
        canvas.drawOval(r1, stroke)
        canvas.drawOval(r2, stroke)
        canvas.drawOval(r3, stroke)

        canvas.drawRect(cx - 18f * s, cy - 2f * s, cx + 18f * s, cy + 17f * s, fill)
        canvas.drawLine(cx - 18f * s, cy + 18f * s, cx + 18f * s, cy + 18f * s, stroke)
    }

    private fun drawPartlyCloudy(canvas: Canvas, cx: Float, cy: Float, s: Float, stroke: Paint, fill: Paint) {
        drawSun(canvas, cx - 14f * s, cy - 12f * s, 12f * s, stroke)
        drawCloud(canvas, cx + 6f * s, cy + 8f * s, s * 0.92f, stroke, fill)
    }

    private fun drawRain(canvas: Canvas, cx: Float, cy: Float, s: Float, stroke: Paint, fill: Paint) {
        drawCloud(canvas, cx, cy - 6f * s, s * 0.88f, stroke, fill)
        for (dx in intArrayOf(-14, 0, 14)) {
            val x = cx + dx * s
            canvas.drawLine(x, cy + 16f * s, x - 5f * s, cy + 28f * s, stroke)
        }
    }

    private fun drawSnow(canvas: Canvas, cx: Float, cy: Float, s: Float, stroke: Paint, fill: Paint) {
        drawCloud(canvas, cx, cy - 6f * s, s * 0.88f, stroke, fill)
        fill.color = Color.BLACK
        for (dx in intArrayOf(-14, 0, 14)) {
            canvas.drawCircle(cx + dx * s, cy + 23f * s, 3.2f * s, fill)
        }
    }
}
