package com.example.learningassistance.detection.headpose

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class HeadPoseFaceDetectionProcessor: ImageAnalysis.Analyzer {

    // The current rotation degree of pitch, yaw, and row
    var rotX = MutableLiveData<Float?>(null)
    var rotY = MutableLiveData<Float?>(null)

    // Average head rotation degree
    var headEulerXOffset = 0f
    var headEulerYOffset = 0f

    // The necessary calculation resources for head position analysis
    var sumOfHeadEulerX = 0f
    var sumOfHeadEulerY = 0f
    var totalFrame = 0

    // The state of analysis
    var isAnalysisStarting = false  // Indicate whether the analysis has started
    var hasToRestart = MutableLiveData<Boolean>(false)  // Indicate whether the analysis needed to be restarted
    var canAnalysisStart = false
    var isFaceDetected = MutableLiveData<Boolean>(false)

    // Create the face detector of ML kit
    private val faceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private var faceDetectionDetector: FaceDetector? = null


    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            // Get the image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            if (faceDetectionDetector != null) {
                faceDetectionDetector!!.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.size == 0) {
                            // Change the state of analysis
                            this.canAnalysisStart = false
                            this.isFaceDetected.value = false

                            // Set the rotation degrees to null
                            rotX.value = null
                            rotY.value = null

                            // If analyzing, stop the analysis
                            if (isAnalysisStarting) {
                                hasToRestart.value = true
                            }
                        } else {
                            for (face in faces) {
                                // Change the state of analysis
                                this.canAnalysisStart = true
                                this.isFaceDetected.value = true

                                // Get the rotation degrees of head
                                rotX.value = face.headEulerAngleX
                                rotY.value = face.headEulerAngleY

                                if (isAnalysisStarting) {
                                    sumHeadEulerAngle()
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Face detector failed. $e")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun sumHeadEulerAngle() {
        sumOfHeadEulerX += rotX.value ?: 0f
        sumOfHeadEulerY += rotY.value ?: 0f
        totalFrame++
        Log.v(TAG, "sumx: $sumOfHeadEulerX")
        Log.v(TAG, "sumy: $sumOfHeadEulerY")
        Log.v(TAG, "sumframe: $totalFrame")

    }

    fun calculateBasicHeadEulerAngle() {
        headEulerXOffset = String.format("%.2f", sumOfHeadEulerX / totalFrame.toFloat()).toFloat()
        headEulerYOffset = String.format("%.2f", sumOfHeadEulerY / totalFrame.toFloat()).toFloat()
    }

    fun resetProperties() {
        sumOfHeadEulerX = 0f
        sumOfHeadEulerY = 0f
        totalFrame = 0
        /*isAnalysisStarting = false
        canAnalysisStart = false
        hasToRestart = false*/
    }

    fun close() {
        if (faceDetectionDetector != null) {
            faceDetectionDetector!!.close()
            faceDetectionDetector = null
        }
    }

    fun start() {
        if (faceDetectionDetector == null) {
            faceDetectionDetector = FaceDetection.getClient(faceDetectionOptions)
        }
    }

    companion object {
        private val TAG = "HeadPoseFaceDetectionProcessor"
    }
}