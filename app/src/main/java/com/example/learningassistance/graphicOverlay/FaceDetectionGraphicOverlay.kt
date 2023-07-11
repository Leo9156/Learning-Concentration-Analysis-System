package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import com.google.mlkit.vision.face.Face

class FaceDetectionGraphicOverlay(
    context: Context,
    attrs: AttributeSet
    ): GraphicOverlay(context, attrs) {

    private val linePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
    }

    private val dotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        strokeWidth = 4.0f
    }

    private var faces: List<Face>? = null

    fun setFace(faces: List<Face>) {
        if (faces.isEmpty()) {
            this.faces = null
        } else {
            this.faces = faces
        }
        //invalidate()
        postInvalidate()
    }

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int, rotation: Int) {
//        Log.v(TAG, "rotation: $rotation")
        if (rotation == 0 || rotation == 180) {
            this.setImageInfo(imageWidth, imageHeight)
        } else {
            this.setImageInfo(imageHeight, imageWidth)
        }
//        Log.v(TAG, "imageWidth: ${getImageWidth()}")
//        Log.v(TAG, "imageHeight: ${getImageHeight()}")
//        Log.v(TAG, "viewWidth: $width")
//        Log.v(TAG, "viewHeight: $height")

        this.setTransformationElement()

//        Log.v(TAG, "scale: $scaleFactor")
//        Log.v(TAG, "poseHeight: $postScaleHeightOffset")
//        Log.v(TAG, "poseWidth: $postScaleWidthOffset")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        faces?.forEach { face ->
            val x = translateX(face.boundingBox.centerX().toFloat())
            val y = translateY(face.boundingBox.centerY().toFloat())
            Log.v(TAG, "x: $x")
            Log.v(TAG, "y: $y")
            canvas.drawCircle(x, y, 8.0f, dotPaint)

            val left = x - scale(face.boundingBox.width() / 2.0f)
            val top = y - scale(face.boundingBox.height() / 2.0f)
            val right = x + scale(face.boundingBox.width() / 2.0f)
            val bottom = y + scale(face.boundingBox.height() / 2.0f)
            canvas.drawRect(left, top, right, bottom, linePaint)
        }
    }

    companion object {
        private const val TAG = "FaceDetectionGraphicOverlay"
    }
}