package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.camera.core.ImageProxy
import com.example.learningassistance.detection.concentration.yolov5.Results
import com.example.learningassistance.detection.concentration.yolov5.Yolov5TFliteDetector

class ObjectDetectionGraphicOverlay(
    context: Context,
    attrs: AttributeSet
): GraphicOverlay(context, attrs) {

    private var objectDetectionResult: ArrayList<Results>? = null
    private var viewWidth = 0
    private var viewHeight = 0

    fun setResult(results: ArrayList<Results>) {
        this.objectDetectionResult = results
        postInvalidate()
    }

    private val cellPhonePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 8.0f
    }

    private val tabletPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 8.0f
    }

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int, imageProxy: ImageProxy) {
        setImageInfo(imageWidth, imageHeight)
        this.viewWidth = imageProxy.width
        this.viewHeight = imageProxy.height
        this.setTransformationElement()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        objectDetectionResult?.let { results ->
            for (result in results) {
                val loc = result.loc
                val label = result.name

                val imgScaleX: Float = viewWidth.toFloat() / Yolov5TFliteDetector.INPUT_SIZE.width
                val imgScaleY: Float = viewHeight.toFloat() / Yolov5TFliteDetector.INPUT_SIZE.height
                val ivScaleX: Float = width / viewWidth.toFloat()
                val ivScaleY: Float = height / viewHeight.toFloat()

                var left: Float = (imgScaleX * loc.left) * ivScaleX
                val top: Float = (imgScaleY * loc.top) * ivScaleY
                var right: Float = (imgScaleX * loc.right) * ivScaleX
                val bottom: Float = (imgScaleY * loc.bottom) * ivScaleY
                val centerX = width / 2f
                val distLeft = left - centerX
                left -= 2f * distLeft
                val distRight = right - centerX
                right -= 2f * distRight

                if (label.equals("cell phone")) {
                    canvas.drawRect(left, top, right, bottom, cellPhonePaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, tabletPaint)
                }
            }
        }
    }
}