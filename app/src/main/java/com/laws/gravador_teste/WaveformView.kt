package com.laws.gravador_teste

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import kotlin.math.max
import kotlin.math.min

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var audioData: ByteArray? = null
    private val waveformPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.FILL
    }
    private val markerPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val path = Path()
    private val rect = RectF()
    private var sampleStep = 200
    private var progress: Float = 0f
    private var peaks: FloatArray? = null
    var onProgressChanged: ((Float) -> Unit)? = null
    var onFineAdjustment: ((Long) -> Unit)? = null

    private var isDragging = false
    private var startX = 0f
    private var lastProgress = 0f
    private var isAdjustingFine = false

    fun setAudioData(data: ByteArray) {
        audioData = data
        calculatePeaks()
        invalidate()
    }

    private fun calculatePeaks() {
        audioData?.let { data ->
            val numPeaks = width / 2 //
            sampleStep = max(1, data.size / numPeaks)
            peaks = FloatArray(numPeaks)

            var peakIndex = 0
            var i = 0
            while (i < data.size && peakIndex < numPeaks) {
                var min = 127f
                var max = -128f
                val limit = min(i + sampleStep, data.size)

                for (s in i until limit) {
                    val value = data[s].toFloat()
                    min = min(min, value)
                    max = max(max, value)
                }

                peaks!![peakIndex] = (max - min) / 255f
                peakIndex++
                i += sampleStep
            }
        }
    }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupGradient()
        calculatePeaks()
    }

    private fun setupGradient() {
        val gradient = LinearGradient(
            0f, 0f,
            0f, height.toFloat(),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Shader.TileMode.CLAMP
        )
        waveformPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        peaks?.let { peakData ->
            val centerY = height / 2f
            val scale = height / 2.5f

            path.reset()
            path.moveTo(0f, centerY)

            // Draw upper path
            for (i in peakData.indices) {
                val x = i * 2f
                val y = centerY - (peakData[i] * scale)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            // Draw lower path
            for (i in peakData.indices.reversed()) {
                val x = i * 2f
                val y = centerY + (peakData[i] * scale)
                path.lineTo(x, y)
            }

            path.close()
            canvas.drawPath(path, waveformPaint)

            // Draw progress
            rect.set(0f, 0f, width * progress, height.toFloat())
            canvas.drawRect(rect, progressPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                startX = event.x
                lastProgress = progress
                isAdjustingFine = event.pointerCount == 2
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val delta = event.x - startX
                    if (isAdjustingFine) {
                        // Ajuste fino: movimento mais preciso
                        val fineProgress = (delta / width) * 0.1f // 10x mais preciso
                        val newProgress = (lastProgress + fineProgress).coerceIn(0f, 1f)
                        setProgress(newProgress)
                        onFineAdjustment?.invoke((newProgress * getDuration()).toLong())
                    } else {
                        // Ajuste normal
                        val newProgress = (event.x / width).coerceIn(0f, 1f)
                        setProgress(newProgress)
                        onProgressChanged?.invoke(newProgress)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isAdjustingFine = false
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isAdjustingFine = true
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getDuration(): Long {
        return 0L // Implementar retorno da duração real do áudio/vídeo
    }
}