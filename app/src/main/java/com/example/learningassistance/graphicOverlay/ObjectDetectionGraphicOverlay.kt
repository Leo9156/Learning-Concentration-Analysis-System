package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import kotlin.math.min

class ObjectDetectionGraphicOverlay(
    context: Context,
    attrs: AttributeSet
): GraphicOverlay(context, attrs) {

    private var objectDetectionResult: ObjectDetectorResult? = null

    fun setResult(result: ObjectDetectorResult) {
        this.objectDetectionResult = result
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

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int) {
        this.setImageInfo(imageWidth, imageHeight)

        this.setTransformationElement()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        objectDetectionResult?.let {
            for (detection in it.detections()) {
                val boundingBox = detection.boundingBox()
                val x = translateX(boundingBox.centerX())
                val y = translateY(boundingBox.centerY())

                val top = y - scale(boundingBox.height() / 2.0f)
                val left = x - scale(boundingBox.width() / 2.0f)
                val bottom = y + scale(boundingBox.height() / 2.0f)
                val right = x + scale(boundingBox.width() / 2.0f)

                // Draw bounding box around detected objects
                if (detection.categories()[0].categoryName() == "cell phone") {
                    canvas.drawRect(left, top, right, bottom, cellPhonePaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, tabletPaint)
                }
            }

        }
    }
}