package com.example.learningassistance.detection.concentration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.learningassistance.objectdetection.ObjectDetectionProcessor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ConcentrationAnalysisObjectProcessor(private val context: Context) : ImageAnalysis.Analyzer {
    // state
    val isElectronicDevicesDetected = MutableLiveData<String>("")

    // detector
    private val options = ObjectDetector.ObjectDetectorOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath("model.tflite").build())
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setMaxResults(5)
        .setScoreThreshold(0.7f)
        .setResultListener { result, input ->
            returnLiveStreamResult(result, input)
        }
        .setErrorListener {
            returnLiveStreamError()
        }
        .build()
    private var objectDetector: ObjectDetector? = null

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

        // Detection
        detectAsync(mpImage, frameTime)

        mpImage.close()
    }

    private fun detectAsync(mpImage: MPImage?, frameTime: Long) {
        if (objectDetector != null) {
            objectDetector!!.detectAsync(mpImage, frameTime)
        }
    }

    private fun returnLiveStreamResult(result: ObjectDetectorResult?, input: MPImage?) {
        if (result == null) {
            isElectronicDevicesDetected.value = ""
        } else {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()

            result.let {
                for (detection in it.detections()) {
                    isElectronicDevicesDetected.value = detection.categories()[0].categoryName()
                }
            }
        }
    }

    private fun returnLiveStreamError() {
        Log.e(TAG, "Object detector failed.")
    }

    fun start() {
        if (objectDetector == null) {
            objectDetector = ObjectDetector.createFromOptions(context, options)
        }
    }

    fun close() {
        if (objectDetector != null) {
            objectDetector!!.close()
        }
    }

    companion object {
        private val TAG = "ConcentrationAnalysisObjectProcessor"
    }
}