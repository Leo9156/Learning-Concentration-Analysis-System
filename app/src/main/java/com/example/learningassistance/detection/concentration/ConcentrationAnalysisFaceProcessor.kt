package com.example.learningassistance.detection.concentration

import android.content.Context
import android.media.MediaPlayer
import android.os.Build.VERSION_CODES.M
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.learningassistance.R
import com.example.learningassistance.detection.headpose.HeadPoseFaceDetectionProcessor
import com.example.learningassistance.facedetection.BasicHeadPoseMeasurement
import com.example.learningassistance.facedetection.DrowsinessDetection
import com.example.learningassistance.facedetection.FaceDetectionProcessor
import com.example.learningassistance.facedetection.HeadPoseAttentionAnalysis
import com.example.learningassistance.facedetection.NoFaceDetection
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions

class ConcentrationAnalysisFaceProcessor(
    private val context: Context,
    private val headEulerOffsetX: Float,
    private val headEulerOffsetY: Float
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
    private val drowsinessDetector = DrowsinessDetection(context)

    // No face detector
    private val noFaceDetector = NoFaceDetection(context)

    // head pose analyzer
    private val headPoseAttentionAnalyzer = HeadPoseAttentionAnalysis(context)

    // head rotation
    val rotX = MutableLiveData<Float?>(null)
    val rotY = MutableLiveData<Float?>(null)

    // Necessary states
    val isFaceDetected = MutableLiveData<Boolean>(false)
    val isEyesOpen = MutableLiveData<Boolean?>(true)
    private var isAlertDialogShowing = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            if (faceDetectionDetector != null) {
                faceDetectionDetector!!.process(image)
                    .addOnSuccessListener { faces ->
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
        }
        if (faceMeshDetector != null) {
            faceMeshDetector!!.close()
            faceMeshDetector = null
        }
    }

    private fun drowsinessDetection() {
        // When to start timer
        if (!drowsinessDetector.isDrowsinessAnalyzing) {
            drowsinessDetector.isDrowsinessAnalyzing = true
            drowsinessDetector.startDrowsinessTimer()
        }

        // Necessary steps
        drowsinessDetector.increaseTotalFrameNumber()
        drowsinessDetector.endDrowinessTimer()
        drowsinessDetector.calculateDuration()
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

        // Check whether the time had exceeded threshold
        if (drowsinessDetector.getDuration() >= drowsinessDetector.getDetectionPeriodMs()) {
            drowsinessDetector.calculatePerClose()
            val perClose = drowsinessDetector.getPerClose()

            when {
                perClose <= drowsinessDetector.getAwakeThreshold() -> {
                }
                perClose > drowsinessDetector.getAwakeThreshold() && perClose <= drowsinessDetector.getFatigueThreshold() -> {
                    Toast.makeText(context, R.string.drowsiness_tired_msg, Toast.LENGTH_SHORT).show()
                }
                else -> {
                    if (!isAlertDialogShowing) {
                        isAlertDialogShowing = true
                        if (!drowsinessDetector.isAlarmPlaying()) {
                            drowsinessDetector.playAlarm()
                        }
                        if (!drowsinessDetector.isVibrating()) {
                            drowsinessDetector.startVibrating()
                        }
                        MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.warning)
                            .setIcon(R.drawable.ic_warning)
                            .setMessage(context.getString(R.string.drowsiness_alert_dialog_msg))
                            .setPositiveButton(R.string.close) { dialog, _ ->
                                if (drowsinessDetector.isAlarmPlaying()) {
                                    drowsinessDetector.stopAlarm()
                                }
                                if (drowsinessDetector.isVibrating()) {
                                    drowsinessDetector.stopVibrating()
                                }
                                isAlertDialogShowing = false
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
            }

            drowsinessDetector.resetDetector()
        }
    }

    private fun noFaceDetection() {
        // When to start timer
        if (!noFaceDetector.isNoFaceDetecting) {
            noFaceDetector.isNoFaceDetecting = true
            noFaceDetector.startNoFaceTimer()
        }

        // Necessary steps
        noFaceDetector.increaseTotalFrameNumber()
        noFaceDetector.endNoFaceTimer()
        noFaceDetector.calculateDuration()

        // No face detection
        if (!isFaceDetected.value!!) {
            Log.v(TAG, "no face")
            noFaceDetector.increaseNoFaceFrameNumber()
        }

        if (noFaceDetector.getDuration() >= noFaceDetector.getDetectionPeriodMs()) {
            noFaceDetector.calculatePerNoFace()
            val perNoFace = noFaceDetector.getPerNoFace()
            if (perNoFace > noFaceDetector.getSevereNoFaceThreshold()) {
                if (!isAlertDialogShowing) {
                    isAlertDialogShowing = true
                    if (!noFaceDetector.isAlarmPlaying()) {
                        noFaceDetector.playAlarm()
                    }
                    if (!noFaceDetector.isVibrating()) {
                        noFaceDetector.startVibrating()
                    }
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.are_you_still_there)
                        .setIcon(R.drawable.ic_warning)
                        .setMessage(context.getString(R.string.no_face_alert_dialog_msg))
                        .setPositiveButton(R.string.close) { dialog, _ ->
                            if (noFaceDetector.isAlarmPlaying()) {
                                noFaceDetector.stopAlarm()
                            }
                            if (noFaceDetector.isVibrating()) {
                                noFaceDetector.stopVibrating()
                            }
                            isAlertDialogShowing = false
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
            // Reset the variables
            noFaceDetector.resetDetector()
        }
    }

    private fun headPoseAttentivenessAnalysis(eulerX: Float, eulerY: Float) {
        headPoseAttentionAnalyzer.isAlertDialogShowing = this.isAlertDialogShowing

        if (!headPoseAttentionAnalyzer.isHeadPoseAnalyzing) {
            headPoseAttentionAnalyzer.startTimer()
            headPoseAttentionAnalyzer.isHeadPoseAnalyzing = true
        }

        if (!isFaceDetected.value!!) {
            headPoseAttentionAnalyzer.increaseTotalInattentionFrame()
        } else {
            headPoseAttentionAnalyzer.analyzeHeadPose(eulerX, eulerY)
        }
        headPoseAttentionAnalyzer.evaluateAttention()
        headPoseAttentionAnalyzer.analyzeAttentiveness()

        if (headPoseAttentionAnalyzer.isDistracted) {
            if (!isAlertDialogShowing) {
                isAlertDialogShowing = true
                val alertDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.pay_attention)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(context.getString(R.string.head_pose_inattention_msg))
                    .show()

                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        Log.v(TAG, "$millisUntilFinished")
                    }

                    override fun onFinish() {
                        alertDialog.dismiss()
                        isAlertDialogShowing = false
                    }

                }.start()
            }

            headPoseAttentionAnalyzer.isDistracted = false
        }
    }

    companion object {
        private val TAG = "ConcentrationAnalysisFaceProcessor"
    }
}