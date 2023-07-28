package com.example.learningassistance.detection.concentration

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.learningassistance.graphicOverlay.ObjectDetectionGraphicOverlay
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

class ConcentrationAnalysisObjectProcessor(
    private val context: Context,
    private val objectDetectionGraphicOverlay: ObjectDetectionGraphicOverlay,
    private val viewModel: ConcentrationAnalysisViewModel
    ) : ImageAnalysis.Analyzer {
    // state
    val isElectronicDevicesDetected = MutableLiveData<String>("")
    var isGraphicShow = false
    private var isTimerStart = false

    // Timer to calculate how long does the user use electronic devices
    private var startTimer = System.currentTimeMillis()
    private var endTimer = System.currentTimeMillis()
    private var duration: Long = 0

    // detector
    private val options = ObjectDetector.ObjectDetectorOptions.builder()
        .setBaseOptions(BaseOptions.builder().setModelAssetPath("model.tflite").build())
        .setRunningMode(RunningMode.LIVE_STREAM)
        .setMaxResults(1)
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
            Log.v(TAG, "no detected")
            isElectronicDevicesDetected.postValue("")
        } else {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()

            setObjectDetectionGraphicOverlay(result, input)

            result.let {
                if (it.detections().size == 0) {
                    isElectronicDevicesDetected.postValue("")

                    if (!this.isTimerStart) {
                        this.startTimer = System.currentTimeMillis()
                    }
                    this.endTimer = System.currentTimeMillis()
                    this.duration = endTimer - startTimer
                    if (this.duration > 3000) {
                        Log.v(TAG, "time: $duration")
                        viewModel.electronicDevicesTime += this.duration
                    }
                    this.isTimerStart = false
                } else {
                    for (detection in it.detections()) {
                        isElectronicDevicesDetected.postValue(detection.categories()[0].categoryName())

                        if (!this.isTimerStart) {
                            this.startTimer = System.currentTimeMillis()
                            this.isTimerStart = true
                        }
                    }
                }
            }
        }
    }

    private fun returnLiveStreamError() {
        Log.e(TAG, "Object detector failed.")
    }

    private fun setObjectDetectionGraphicOverlay(result: ObjectDetectorResult, input: MPImage?) {
        if (isGraphicShow) {
            objectDetectionGraphicOverlay.setResult(result)
            if (input != null) {
                objectDetectionGraphicOverlay.setTransformationInfo(input.width, input.height)
            } else {
                Log.e(TAG, "The input image to the ObjectDetectionGraphicOverlay is null.")
            }
        }
    }

    fun start() {
        if (objectDetector == null) {
            objectDetector = ObjectDetector.createFromOptions(context, options)
        }
    }

    fun close() {
        if (objectDetector != null) {
            objectDetector!!.close()
            objectDetector = null
            isTimerStart = false
        }
    }

    companion object {
        private val TAG = "ConcentrationAnalysisObjectProcessor"
    }
}