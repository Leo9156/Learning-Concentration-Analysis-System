package com.example.learningassistance.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.learningassistance.CameraPreviewActivity
import com.example.learningassistance.graphicOverlay.ObjectDetectionGraphicOverlay
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector.ObjectDetectorOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ObjectDetectionProcessor(
    context: Context,
    private val objectDetectionGraphicOverlay: ObjectDetectionGraphicOverlay,
    private val cameraPreviewActivity: CameraPreviewActivity,
    ): ImageAnalysis.Analyzer {

    private val options = ObjectDetectorOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath("model.tflite").build())
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setMaxResults(5)
        .setScoreThreshold(0.5f)
        .setResultListener { result, input ->
            returnLiveStreamResult(result, input)
        }
        .setErrorListener {
            returnLiveStreamError()
        }
        .build()
    private val objectDetector = ObjectDetector.createFromOptions(context, options)

    private var rotationDegree = 0


    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888

        )
        imageProxy.use {
            bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
        }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        // Convert the input Bitmap object to an MPImage object to run inference
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()
        // val mpImage = MediaImageBuilder(mediaImage).build()
        val frameTime = SystemClock.uptimeMillis()

        // Object detection
        //textViewObjectMsg.text = ""
        //cameraPreviewActivity.resetObjectDetectionMessageTextView()
        detectAsync(mpImage, frameTime)

        mpImage.close()
    }

    private fun detectAsync(mpImage: MPImage?, frameTime: Long) {
        objectDetector.detectAsync(mpImage, frameTime)
    }

    private fun returnLiveStreamResult(result: ObjectDetectorResult?, input: MPImage?) {
        if (result == null) {
            cameraPreviewActivity.resetObjectDetectionMessageTextView()
            Log.v(TAG, "No object detected")
        } else {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()

            setObjectDetectionGraphicOverlay(result, input, rotationDegree)

            result.let {
                for (detection in it.detections()) {
                    val objectDetectionMessage = "category: ${detection.categories()[0].categoryName()}, score: ${detection.categories()[0].score()}"

                    Log.v(TAG, "Object detected: ${detection.categories()[0].categoryName()}")
                    Log.v(TAG, "score: ${detection.categories()[0].score()}")
                    Log.v(TAG, "Inference time(ms): $inferenceTime")
                }
            }
        }
    }

    private fun returnLiveStreamError() {
        Log.e(TAG, "Object detector failed.")
    }

    private fun setObjectDetectionGraphicOverlay(result: ObjectDetectorResult, input: MPImage?, rotation: Int) {
        objectDetectionGraphicOverlay.setResult(result)
        if (input != null) {
            Log.v(TAG, "input image width: ${input.width}, input image height: ${input.height}")
            objectDetectionGraphicOverlay.setTransformationInfo(input.width, input.height)
        } else {
            Log.e(TAG, "The input image to the ObjectDetectionGraphicOverlay is null.")
        }
    }

    companion object {
        private const val TAG = "ObjectDetectionProcessor"
    }

}