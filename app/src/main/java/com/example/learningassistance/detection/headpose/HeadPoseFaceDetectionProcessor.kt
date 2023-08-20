package com.example.learningassistance.detection.headpose

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.abs
import kotlin.math.sqrt

class HeadPoseFaceDetectionProcessor: ImageAnalysis.Analyzer {

    // The current rotation degree of pitch, yaw, and row
    var rotX = MutableLiveData<Float?>(null)
    var rotY = MutableLiveData<Float?>(null)

    // Eyes aspect Ratio
    var EAR = MutableLiveData<Float?>(null)
    var prevEAR = 0f

    // Average head rotation degree
    var headEulerXOffset = 0f
    var headEulerYOffset = 0f
    var avgEAR = 0f

    // The necessary calculation resources for head position analysis
    var sumOfHeadEulerX = 0f
    var sumOfHeadEulerY = 0f
    var sumOfEAR= 0f
    var totalFrame = 0
    var totalEarFrame = 0

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

    // Face mesh detector
    private val faceMeshOptions = FaceMeshDetectorOptions.Builder()
    private var faceMeshDetector: FaceMeshDetector? = null

    // Flags
    private var isFaceDetectionCompleted = false
    private var isFaceMeshCompleted = false

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
                            EAR.value = null

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
                        isFaceDetectionCompleted = true
                        if (isFaceMeshCompleted) {
                            imageProxy.close()
                        }
                    }

                if (faceMeshDetector != null) {
                    faceMeshDetector!!.process(image)
                        .addOnSuccessListener { faceMeshes ->
                            if (this.isFaceDetected.value!!) {
                                if (faceMeshes.size == 0) {
                                    // Change the state of analysis
                                    this.canAnalysisStart = false
                                    this.isFaceDetected.value = false

                                    // Set the rotation degrees to null
                                    rotX.value = null
                                    rotY.value = null
                                    EAR.value = null

                                    // If analyzing, stop the analysis
                                    if (isAnalysisStarting) {
                                        hasToRestart.value = true
                                    }
                                } else {
                                    for (faceMesh in faceMeshes) {
                                        EAR.value = calculateEAR(faceMesh.allPoints)
                                        checkDifferencePercent(EAR.value!!)
                                        if (isAnalysisStarting) {
                                            prevEAR = EAR.value!!
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Face mesh detector failed. $e")
                        }
                        .addOnCompleteListener {
                            isFaceMeshCompleted = true
                            if (isFaceDetectionCompleted) {
                                imageProxy.close()
                            }
                        }
                }
            } else {
                imageProxy.close()
            }
        }
    }

    private fun checkDifferencePercent(ear: Float) {
        if (prevEAR != 0f) {
            val difference = abs((ear - prevEAR)) / ear
            if (difference < 0.5f) {
                sumOfEAR += ear
                totalEarFrame++
            }
        }
    }

    private fun calculateEAR(allPoints: List<FaceMeshPoint>): Float {
        // Left EAR
        val aLeft = calculateDistance(allPoints[362].position, allPoints[263].position)
        val bLeft = calculateDistance(allPoints[385].position, allPoints[380].position)
        val cLeft = calculateDistance(allPoints[387].position, allPoints[373].position)
        val leftEAR = (bLeft + cLeft) / (2f * aLeft)

        // Right EAR
        val aRight = calculateDistance(allPoints[33].position, allPoints[133].position)
        val bRight = calculateDistance(allPoints[160].position, allPoints[144].position)
        val cRight = calculateDistance(allPoints[158].position, allPoints[153].position)
        val rightEAR = (bRight + cRight) / (2f * aRight)

        return (leftEAR + rightEAR) / 2f
    }

    private fun calculateDistance(point1: PointF3D, point2: PointF3D): Float {
        val xDiff = point1.x - point2.x
        val yDiff = point1.y - point2.y
        return sqrt((xDiff * xDiff) + (yDiff * yDiff))
    }

    private fun sumHeadEulerAngle() {
        sumOfHeadEulerX += rotX.value ?: 0f
        sumOfHeadEulerY += rotY.value ?: 0f
        totalFrame++
    }

    fun calculateBasicHeadEulerAngle() {
        headEulerXOffset = String.format("%.2f", sumOfHeadEulerX / totalFrame.toFloat()).toFloat()
        headEulerYOffset = String.format("%.2f", sumOfHeadEulerY / totalFrame.toFloat()).toFloat()
    }

    fun calculateAvgEar() {
        avgEAR = String.format("%.2f", sumOfEAR / totalEarFrame.toFloat()).toFloat()
    }

    fun resetProperties() {
        sumOfHeadEulerX = 0f
        sumOfHeadEulerY = 0f
        sumOfEAR = 0f
        totalFrame = 0
        totalEarFrame = 0
        prevEAR = 0f
        isFaceDetectionCompleted = false
        isFaceMeshCompleted = false
    }

    fun close() {
        if (faceDetectionDetector != null) {
            faceDetectionDetector!!.close()
            faceDetectionDetector = null
        }
        if (faceMeshDetector != null) {
            faceMeshDetector!!.close()
            faceMeshDetector = null
        }
    }

    fun start() {
        if (faceDetectionDetector == null) {
            faceDetectionDetector = FaceDetection.getClient(faceDetectionOptions)
        }
        if (faceMeshDetector == null) {
            faceMeshDetector = FaceMeshDetection.getClient(faceMeshOptions.build())
        }
    }

    companion object {
        private val TAG = "HeadPoseFaceDetectionProcessor"
    }
}