package com.example.learningassistance.detection.concentration

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.example.learningassistance.R
import com.example.learningassistance.facedetection.DrowsinessDetection
import com.example.learningassistance.facedetection.HeadPoseAttentionAnalysis
import com.example.learningassistance.facedetection.NoFaceDetection
import com.example.learningassistance.graphicOverlay.FaceDetectionGraphicOverlay
import com.example.learningassistance.graphicOverlay.FaceMeshGraphicOverlay
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions

class ConcentrationAnalysisFaceProcessor(
    private val context: Context,
    var headEulerOffsetX: Float,
    var headEulerOffsetY: Float,
    private val faceGraphicOverlay: FaceDetectionGraphicOverlay,
    private val faceMeshGraphicOverlay: FaceMeshGraphicOverlay,
    ) : ImageAnalysis.Analyzer {

    // Face detector
    private val faceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private var faceDetectionDetector: FaceDetector? = null

    // Face mesh detector
    private val faceMeshOptions = FaceMeshDetectorOptions.Builder()
    private var faceMeshDetector: FaceMeshDetector? = null

    // Drowsiness detector
    val drowsinessDetector = DrowsinessDetection(context)

    // No face detector
    val noFaceDetector = NoFaceDetection(context)

    // head pose analyzer
    val headPoseAttentionAnalyzer = HeadPoseAttentionAnalysis(context)

    // head rotation
    val rotX = MutableLiveData<Float?>(null)
    val rotY = MutableLiveData<Float?>(null)

    // Necessary states
    val isFaceDetected = MutableLiveData<Boolean>(false)
    val isEyesOpen = MutableLiveData<Boolean?>(true)
    var isNoFaceDetectionShouldStart = false
    var isDrowsinessDetectionShouldStart = false
    var isHeadPoseAnalysisShouldStart = false
    private var isAlertDialogShowing = false
    var isGraphicShow = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            if (faceDetectionDetector != null) {
                faceDetectionDetector!!.process(image)
                    .addOnSuccessListener { faces ->
                        setFaceDetectionGraphicOverlay(faces, image)

                        if (faces.size == 0) {
                            // Change state
                            isFaceDetected.value = false

                            // rotation degree
                            rotX.value = null
                            rotY.value = null

                        } else {
                            // Change state
                            isFaceDetected.value = true

                            for (face in faces) {
                                // Calculate the normalized rotation degrees
                                rotX.value = (face.headEulerAngleX - headEulerOffsetX)
                                rotY.value = (face.headEulerAngleY - headEulerOffsetY)
                            }
                        }

                        // No face detection
                        noFaceDetection()

                        // Head pose analysis
                        headPoseAttentivenessAnalysis(rotX.value ?: 0f, rotY.value ?: 0f)
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

            if (faceMeshDetector != null) {
                faceMeshDetector!!.process(image)
                    .addOnSuccessListener { faceMeshes ->
                        setFaceMeshGraphicOverlay(faceMeshes, image)

                        if (faceMeshes.size == 0) {
                            // Change state
                            drowsinessDetector.resetEAR()
                        } else {
                            for (faceMesh in faceMeshes) {
                                drowsinessDetector.calculateEAR(faceMesh.allPoints)
                            }
                        }

                        drowsinessDetection()
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Face mesh failed. $e")
                    }
            } else {
                imageProxy.close()
            }
            //mediaImage.close()
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

    fun close() {
        if (faceDetectionDetector != null) {
            faceDetectionDetector!!.close()
            faceDetectionDetector = null

            noFaceDetector.resetDetector()
            noFaceDetector.isNoFaceDetecting = false

            headPoseAttentionAnalyzer.isHeadPoseAnalyzing = false
            headPoseAttentionAnalyzer.resetAnalyzer()
            headPoseAttentionAnalyzer.isDistracted = false
        }
        if (faceMeshDetector != null) {
            faceMeshDetector!!.close()
            faceMeshDetector = null

            drowsinessDetector.resetDetector()
            drowsinessDetector.isDrowsinessAnalyzing = false
        }
    }

    private fun drowsinessDetection() {
        if (isDrowsinessDetectionShouldStart) {
            drowsinessDetector.increaseTotalFrameNumber()

            if (drowsinessDetector.getEAR() < drowsinessDetector.getClosedEyeThreshold()) {
                drowsinessDetector.increaseClosedEyesFrameNumber()
                isEyesOpen.value = false
            } else {
                if (drowsinessDetector.getEAR() == 2.0f) {
                    isEyesOpen.value = null
                } else {
                    isEyesOpen.value = true
                }
            }
        }
    }

    private fun noFaceDetection() {
        if (isNoFaceDetectionShouldStart) {
            // Necessary steps
            noFaceDetector.increaseTotalFrameNumber()

            // No face detection
            if (!isFaceDetected.value!!) {
                noFaceDetector.increaseNoFaceFrameNumber()
            }
        }
    }

    private fun headPoseAttentivenessAnalysis(eulerX: Float, eulerY: Float) {
        if (isHeadPoseAnalysisShouldStart) {
            if (!isFaceDetected.value!!) {
                headPoseAttentionAnalyzer.analyzeHeadPose(0f, 0f)
            } else {
                headPoseAttentionAnalyzer.analyzeHeadPose(eulerX, eulerY)
            }
            headPoseAttentionAnalyzer.evaluateAttention()
        }
    }

    private fun setFaceDetectionGraphicOverlay(faces: List<Face>, image: InputImage) {
        if (isGraphicShow) {
            faceGraphicOverlay.setFace(faces)
            faceGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
        }
    }

    private fun setFaceMeshGraphicOverlay(faceMeshes: MutableList<FaceMesh>, image: InputImage) {
        if (isGraphicShow) {
            faceMeshGraphicOverlay.setFace(faceMeshes)
            faceMeshGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
        }
    }

    companion object {
        private val TAG = "ConcentrationAnalysisFaceProcessor"
    }
}