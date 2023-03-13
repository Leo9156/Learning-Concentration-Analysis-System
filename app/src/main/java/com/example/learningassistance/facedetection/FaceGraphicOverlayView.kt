package com.example.learningassistance.facedetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Half.toFloat
import android.util.Log
import android.view.View
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class FaceGraphicOverlayView constructor(
    context: Context,
    attrs: AttributeSet
    ): View(context, attrs) {

    private var imageWidth = 0f
    private var imageHeight = 0f
    private var previewWidth = 0f
    private var previewHeight = 0f
    private var rotation = 0
    private var isLandScape = true
    private var faces: List<Face>? = null
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.0f
    }
    private val dotPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        strokeWidth = 4.0f
    }

    fun setFace(faces: List<Face>) {
        this.faces = faces
        invalidate()
    }

    fun setImageSize(width: Float, height: Float) {
        if (isLandScape) {
            this.imageWidth = width
            this.imageHeight = height
        } else {
            this.imageWidth = height
            this.imageHeight = width
        }

        Log.v(TAG, "image width: $imageWidth image height: $imageHeight")
    }

    fun setPreviewSize(width: Float, height: Float) {
        if (isLandScape) {
            this.previewWidth = width
            this.previewHeight = height
        } else {
            this.previewWidth = height
            this.previewHeight = width
        }

        Log.v(TAG, "preview width: $previewWidth preview height: $previewHeight")
    }

    fun setRotation(rotation: Int) {
        this.rotation = rotation

        isLandScape = rotation == 0 || rotation == 180
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        faces?.forEach { face ->
            val boundingBox = face.boundingBox
            val faceContour = face.allContours

            val scaleFactorX = previewWidth / imageWidth
            val scaleFactorY = previewHeight / imageHeight
            val scale = if (isLandScape) {
                max(scaleFactorX, scaleFactorY)
            } else {
                min(scaleFactorX, scaleFactorY)
            }
            val offsetX = if (isLandScape) (previewWidth - imageWidth * scale) / 2
                          else (previewHeight - imageWidth * scale) / 2
            val offsetY = if (isLandScape) (previewHeight - imageHeight * scale) / 2
                          else (previewWidth - imageHeight * scale) / 2

            val left = boundingBox.left * scale + offsetX
            val top = boundingBox.top * scale + offsetY
            val right = boundingBox.right * scale + offsetX
            val bottom = boundingBox.bottom * scale + offsetY

            canvas.drawRect(left, top, right, bottom, paint)

            for (contour in faceContour) {
                for (point in contour.points) {
                    val px = point.x * scale + offsetX
                    val py = point.y * scale + offsetY
                    canvas.drawCircle(px, py, 5.0f, dotPaint)
                }
            }

        }
    }

    companion object {
        private const val TAG = "FaceGraphicOverlay"
    }
}