package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshPoint

class FaceMeshGraphicOverlay(
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

    private var faces: MutableList<FaceMesh>? = null

    private val displayContours = intArrayOf(
        FaceMesh.LEFT_EYE,
        FaceMesh.RIGHT_EYE,
        FaceMesh.LOWER_LIP_BOTTOM,
        FaceMesh.LOWER_LIP_TOP,
        FaceMesh.UPPER_LIP_BOTTOM,
        FaceMesh.UPPER_LIP_TOP
    )

    fun setFace(faceMeshes: MutableList<FaceMesh>) {
        if (faceMeshes.size == 0) {
            this.faces = null
        } else {
            this.faces = faceMeshes
        }
        invalidate()  // Will call onDraw again
    }

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int, rotation: Int) {
        if (rotation == 0 || rotation == 180) {
            this.setImageInfo(imageWidth, imageHeight)
        } else {
            this.setImageInfo(imageHeight, imageWidth)
        }

        this.setTransformationElement()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        Log.v("FaceMeshGrap", "faces: $faces")
        faces?.forEach { face ->
            val points = getContourPoints(face)

            for (point in points) {
                canvas.drawCircle(
                    translateX(point.position.x),
                    translateY(point.position.y),
                    4.0f,
                    dotPaint
                )
            }
        }
    }

    private fun getContourPoints(face: FaceMesh): List<FaceMeshPoint> {
        val contourPoints: MutableList<FaceMeshPoint> = mutableListOf()

        for (type in displayContours) {
            contourPoints.addAll(face.getPoints(type))
        }

        return contourPoints
    }
}