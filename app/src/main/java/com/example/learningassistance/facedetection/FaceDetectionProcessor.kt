package com.example.learningassistance.facedetection

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.Vibrator
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.learningassistance.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionProcessor(
    private val context: Context,
    //private val fgOverlay: FaceGraphicOverlayView,
    private val root: ConstraintLayout,
    private val tvEulerX: TextView,
    private val tvEulerY: TextView,
    private val tvEulerZ: TextView,
    private val tvRightEyeOpenProb: TextView,
    private val tvLeftEyeOpenProb: TextView,
    private val tvLatencyTime: TextView,
    private val tvNoFaceMsg: TextView,
    private val tvDrowsinessTimer: TextView
    ): ImageAnalysis.Analyzer {

    private val drowsinessDetector = DrowsinessDetection(context)
    private lateinit var fatigueAlertDialog: AlertDialog

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Get the image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            //val rotation = imageProxy.imageInfo.rotationDegrees
            val detectionStartTimeMs = System.currentTimeMillis()

            //Log.v(TAG, "rotation: $rotation")
            //Log.v(TAG, "isBasicHeadPoseStarting: $isBasicHeadPoseStarting")

            // Process the image
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    Log.d(TAG, "Number of face detected: ${faces.size}")

                    // Dealing with no face detected situations
                    if (faces.size == 0) {
                        setNoFaceMsg()

                        // Reset the variables of drowsiness detection
                        drowsinessDetector.resetTotalFrameNumber()
                        drowsinessDetector.resetClosedEyesFrameNumber()
                        drowsinessDetector.startDrowsinessTimer()

                        // Restart basic head pose measuring if the user is measuring basic head pose
                        restartHeadPoseMeasurement()

                    } else {
                        //totalDrowsinessDetectionFrameCount++

                        // Calculate the processing time
                        val detectionEndTimeMs = System.currentTimeMillis()
                        processTime(detectionStartTimeMs, detectionEndTimeMs)

                        // Set the information about the face graphic overlay
                        //setFaceGraphicOverlay(faces, image, rotation)

                        for (face in faces) {
                            // reset the textView of no face detection
                            tvNoFaceMsg.text = ""

                            // Get the euler angles of the detected face
                            val rotX = face.headEulerAngleX
                            val rotY = face.headEulerAngleY
                            val rotZ = face.headEulerAngleZ
                            val rightEyeOpenProb = face.rightEyeOpenProbability
                            val leftEyeOpenProb = face.leftEyeOpenProbability

                            // Measure the basic head pose for normalization
                            if (BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
                                if (BasicHeadPoseMeasurement.isBasicHeadPoseMeasurementStarting()) {
                                    measureBasicHeadPose(context, rotX, rotY, rotZ)
                                } else {
                                    BasicHeadPoseMeasurement.setStartTimer(System.currentTimeMillis())
                                }

                                // reset the drowsiness start timer
                                drowsinessDetector.startDrowsinessTimer()
                            }
                            // Start attention analysis
                            else {
                                //Set the open probability of each eye on the textView
                                if (rightEyeOpenProb != null && leftEyeOpenProb != null) {
                                    setRightEyeMsg(rightEyeOpenProb)
                                    setLeftEyeMsg(leftEyeOpenProb)
                                    drowsinessDetection(
                                        rightEyeOpenProb.toFloat(),
                                        leftEyeOpenProb.toFloat()
                                    )
                                }
                            }

                            // Set the euler angles on the textView
                            setEulerAnglesMsg(rotX, rotY, rotZ)

                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Face detector failed. $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    }

    private fun restartHeadPoseMeasurement() {
        if (BasicHeadPoseMeasurement.isBasicHeadPoseMeasurementStarting()) {
            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(false)
            BasicHeadPoseMeasurement.setHasToRestart(true)
            BasicHeadPoseMeasurement.setTotalFrame(0)
            BasicHeadPoseMeasurement.setSumOfHeadEulerX(0f)
            BasicHeadPoseMeasurement.setSumOfHeadEulerY(0f)
            BasicHeadPoseMeasurement.setSumOfHeadEulerZ(0f)

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.warning)
                .setMessage(R.string.head_pose_retry_msg)
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton(R.string.retry) { dialog, _ ->
                    Snackbar.make(root, R.string.basic_head_pose_msg, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.start) {
                            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(true)

                            object : CountDownTimer(5000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                    Snackbar.make(
                                        root,
                                        String.format(
                                            context.getString(R.string.basic_head_pose_counting_msg),
                                            (millisUntilFinished / 1000).toInt()),
                                        Snackbar.LENGTH_INDEFINITE
                                    ).show()
                                }

                                override fun onFinish() {
                                    Snackbar.make(root, R.string.basic_head_pose_complete_msg, Snackbar.LENGTH_SHORT)
                                        .show()
                                }

                            }.start()
                        }
                        .show()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun measureBasicHeadPose(context: Context, rotX: Float, rotY: Float, rotZ: Float) {
        // TODO: Handle with the landscape mode
        BasicHeadPoseMeasurement.increaseTotalFrameNumber()
        BasicHeadPoseMeasurement.setEndTimer(System.currentTimeMillis())
        BasicHeadPoseMeasurement.calculateDuration()

        if (BasicHeadPoseMeasurement.getDuration() >= BasicHeadPoseMeasurement.getMeasureDurationMs()) {
            BasicHeadPoseMeasurement.calculateBasicHeadPose()

            BasicHeadPoseMeasurement.setIsBasicHeadPoseDetecting(false)
            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(false)

            Log.v(TAG, "basicEulerX: ${BasicHeadPoseMeasurement.getBasicHeadEulerX()}")
            Log.v(TAG, "basicEulerY: ${BasicHeadPoseMeasurement.getBasicHeadEulerY()}")
            Log.v(TAG, "basicEulerZ: ${BasicHeadPoseMeasurement.getBasicHeadEulerZ()}")
        } else {
            BasicHeadPoseMeasurement.addSumOfEulerX(rotX)
            BasicHeadPoseMeasurement.addSumOfEulerY(rotY)
            BasicHeadPoseMeasurement.addSumOfEulerZ(rotZ)
        }
    }


    private fun drowsinessDetection(rightEyeOpenProb: Float, leftEyeOpenProb: Float) {
        drowsinessDetector.increaseTotalFrameNumber()

        drowsinessDetector.endDrowinessTimer()
        drowsinessDetector.calculateDuration()

        if (leftEyeOpenProb < drowsinessDetector.getClosedEyeThreshold()
            || rightEyeOpenProb < drowsinessDetector.getClosedEyeThreshold()) {
            drowsinessDetector.increaseClosedEyesFrameNumber()
        }

        // Show the timer
        tvDrowsinessTimer.text = context.getString(R.string.drowsiness_timer, drowsinessDetector.getDuration() / 1000)

        if (drowsinessDetector.getDuration() >= drowsinessDetector.getDetectionPeriodMs()) {
            drowsinessDetector.calculatePerClose()
            val perClose = drowsinessDetector.getPerClose()

            when {
                perClose <= drowsinessDetector.getAwakeThreshold() -> {
                    if (drowsinessDetector.isFatigueDialogShowing()) {
                        fatigueAlertDialog.dismiss()
                        drowsinessDetector.setIsFatigueDialogShowing(false)
                    }

                    if (drowsinessDetector.isAlarmPlaying()) {
                        drowsinessDetector.stopAlarm()
                    }

                    if (drowsinessDetector.isVibrating()) {
                        drowsinessDetector.stopVibrating()
                    }

                    Log.v(TAG, "awake")
                }
                perClose > drowsinessDetector.getAwakeThreshold() && perClose <= drowsinessDetector.getFatigueThreshold() -> {
                    Snackbar.make(root, R.string.drowsiness_tired_msg, Snackbar.LENGTH_SHORT)
                        .show()

                    Log.v(TAG, "tired")
                }
                else -> {
                    if (!drowsinessDetector.isAlarmPlaying()) {
                        drowsinessDetector.playAlarm()
                    }

                    if (!drowsinessDetector.isVibrating()) {
                        drowsinessDetector.startVibrating()
                    }

                    if (!drowsinessDetector.isFatigueDialogShowing()) {
                        showFatigueAlertDialog(context)
                    }

                    Log.v(TAG, "fatigue")
                }
            }

            // Reset the variables
            drowsinessDetector.resetTotalFrameNumber()
            drowsinessDetector.resetClosedEyesFrameNumber()
            drowsinessDetector.startDrowsinessTimer()
        }
    }

    private fun showFatigueAlertDialog(context: Context) {
        drowsinessDetector.setIsFatigueDialogShowing(true)

        this.fatigueAlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.warning)
            .setIcon(R.drawable.ic_warning)
            .setMessage(context.getString(R.string.drowsiness_alert_dialog_msg))
            .setPositiveButton(R.string.close) { dialog, _ ->
                drowsinessDetector.stopAlarm()

                drowsinessDetector.stopVibrating()

                drowsinessDetector.setIsFatigueDialogShowing(false)

                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun processTime(detectionStartTimeMs: Long, detectionEndTimeMs: Long) {
        val latencyMs = detectionEndTimeMs - detectionStartTimeMs
        val latencyMsg =context.getString(R.string.face_detector_latency) + "$latencyMs" + " ms"
        tvLatencyTime.text = latencyMsg
    }

    private fun setNoFaceMsg() {
        tvEulerX.text = ""
        tvEulerY.text = ""
        tvEulerZ.text = ""
        tvRightEyeOpenProb.text = ""
        tvLeftEyeOpenProb.text = ""
        tvLatencyTime.text = ""
        tvNoFaceMsg.text = context.getString(R.string.no_face_detected_info)
    }

    /*private fun setFaceGraphicOverlay(faces: List<Face>, image: InputImage, rotation: Int) {
        fgOverlay.setFace(faces)
        fgOverlay.setRotation(rotation)
        fgOverlay.setImageSize(image.width.toFloat(), image.height.toFloat())
        fgOverlay.setPreviewSize(fgOverlay.width.toFloat(), fgOverlay.height.toFloat())
    }*/


    private fun setEulerAnglesMsg(rotX: Float, rotY: Float, rotZ: Float) {
        val eulerXMsg = String.format(context.getString(R.string.eulerx), rotX)
        val eulerYMsg = String.format(context.getString(R.string.eulery), rotY)
        val eulerZMsg = String.format(context.getString(R.string.eulerz), rotZ)
        tvEulerX.text = eulerXMsg
        tvEulerY.text = eulerYMsg
        tvEulerZ.text = eulerZMsg
    }

    private fun setLeftEyeMsg(leftEyeOpenProb: Float) {
        val leftEyeOpenMsg = context.getString(R.string.right_eye_open) + "%.2f".format(leftEyeOpenProb)
        tvLeftEyeOpenProb.text = leftEyeOpenMsg
    }

    private fun setRightEyeMsg(rightEyeOpenProb: Float) {
        val rightEyeOpenMsg = context.getString(R.string.left_eye_open) + "%.2f".format(rightEyeOpenProb)
        tvRightEyeOpenProb.text = rightEyeOpenMsg
    }

    companion object {
        private const val TAG = "FaceDetectionProcessor"
    }

}