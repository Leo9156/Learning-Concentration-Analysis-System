package com.example.learningassistance.graphicOverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PoseGraphicOverlay(
    context: Context,
    attrs: AttributeSet
    ): GraphicOverlay(context, attrs) {

    private var poses: Pose? = null

    private val leftPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8.0f
    }

    private val rightPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 8.0f
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 8.0f
    }

    private val displayLandmarks = intArrayOf(
        PoseLandmark.LEFT_SHOULDER,
        PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_ELBOW,
        PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.LEFT_WRIST,
        PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE,
        PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.LEFT_PINKY,
        PoseLandmark.RIGHT_PINKY,
        PoseLandmark.LEFT_INDEX,
        PoseLandmark.RIGHT_INDEX,
        PoseLandmark.LEFT_THUMB,
        PoseLandmark.RIGHT_THUMB,
        PoseLandmark.LEFT_HEEL,
        PoseLandmark.RIGHT_HEEL,
        PoseLandmark.LEFT_FOOT_INDEX,
        PoseLandmark.RIGHT_FOOT_INDEX
    )

    fun setPoses(poses: Pose) {
        this.poses = poses
        invalidate()
    }

    fun setTransformationInfo(imageWidth: Int, imageHeight: Int, rotation: Int) {
        if (rotation == 0 || rotation == 180) {
            this.setImageInfo(imageWidth, imageHeight)
        } else {
            this.setImageInfo(imageHeight, imageWidth)
        }

        this.setTransformationElement()
    }

    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark?, whitePaint: Paint) {
        val point = landmark!!.position3D
        canvas.drawCircle(translateX(point.x), translateY(point.y), 4.0f, whitePaint)
    }

    private fun drawLine(
        canvas: Canvas,
        startLandmark: PoseLandmark?,
        endLandmark: PoseLandmark?,
        paint: Paint
    ) {
        val start = startLandmark!!.position3D
        val end = endLandmark!!.position3D

        canvas.drawLine(
            translateX(start.x),
            translateY(start.y),
            translateX(end.x),
            translateY(end.y),
            paint
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (this.poses != null) {
            val landmark = poses!!.allPoseLandmarks

            if (landmark.isEmpty()) {
                poses = null
                invalidate()
                return
            }

            // Draw all the points
            displayLandmarks.forEach { type ->
                drawPoint(canvas, poses!!.getPoseLandmark(type), whitePaint)
            }

            // Draw all the lines
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
                whitePaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_HIP),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_HIP),
                whitePaint
            )
            // Left body
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_WRIST),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_HIP),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_HIP),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_KNEE),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_KNEE),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_THUMB),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_PINKY),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_INDEX),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_INDEX),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_PINKY),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_ANKLE),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_HEEL),
                leftPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.LEFT_HEEL),
                poses!!.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX),
                leftPaint
            )

            // Right body
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_HIP),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_HIP),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_KNEE),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_THUMB),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_PINKY),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_WRIST),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_INDEX),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_INDEX),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_PINKY),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_ANKLE),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_HEEL),
                rightPaint
            )
            drawLine(
                canvas,
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_HEEL),
                poses!!.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX),
                rightPaint
            )
        }
    }

}