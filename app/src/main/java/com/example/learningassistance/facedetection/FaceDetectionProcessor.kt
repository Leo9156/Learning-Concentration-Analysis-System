package com.example.learningassistance.facedetection

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.learningassistance.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions

class FaceDetectionProcessor(
    private val context: Context,
    //private val fgOverlay: FaceGraphicOverlayView,
    private val root: ConstraintLayout,
    private val tvEulerX: TextView,
    private val tvEulerY: TextView,
    private val tvEulerZ: TextView,
    private val tvRightEAR: TextView,
    private val tvLeftEAR: TextView,
    private val tvEAR: TextView,
    private val tvLatencyTime: TextView,
    private val tvNoFaceMsg: TextView,
    private val tvDrowsinessTimer: TextView,
    private val tvNoFaceTimer: TextView,
    private val tvBasicHeadPoseTimer: TextView,
    private val btnRetryBasicHeadPoseMeasurement: MaterialButton
    ): ImageAnalysis.Analyzer {

    private var rotX = 0f
    private var rotY = 0f
    private var rotZ = 0f

    private val drowsinessDetector = DrowsinessDetection(context)
    private lateinit var fatigueAlertDialog: AlertDialog

    private val noFaceDetector = NoFaceDetection(context)
    private lateinit var noFaceAlertDialog: AlertDialog

    private var isAlertDialogShowing = false

    private val faceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        //.setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetectionDetector = FaceDetection.getClient(faceDetectionOptions)

    private val faceMeshOptions = FaceMeshDetectorOptions.Builder()
    private val faceMeshDetector = FaceMeshDetection.getClient(faceMeshOptions.build())

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Get the image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            //val rotation = imageProxy.imageInfo.rotationDegrees
            val detectionStartTimeMs = System.currentTimeMillis()

            //Log.v(TAG, "rotation: $rotation")

            // Process the image
            val faceDetectionResult = faceDetectionDetector.process(image)
                .addOnSuccessListener { faces ->
                    // Dealing with no face detected situations
                    if (faces.size == 0) {
                        // Show the no face message to the user
                        setNoFaceMsg()

                        // Set no face flag
                        noFaceDetector.setIsNoFace(true)

                        // Reset leftEyeOpenProb and rightEyeOpenProb
                        drowsinessDetector.resetEAR()

                        // Restart basic head pose measuring if the user is measuring basic head pose
                        if (BasicHeadPoseMeasurement.isBasicHeadPoseMeasurementStarting()) {
                            // Reset no face detector
                            noFaceDetector.resetDetector()
                            drowsinessDetector.resetDetector()

                            BasicHeadPoseMeasurement.setHasToRestart(true)
                            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(false)
                            BasicHeadPoseMeasurement.setTotalFrame(0)
                            BasicHeadPoseMeasurement.setSumOfHeadEulerX(0f)
                            BasicHeadPoseMeasurement.setSumOfHeadEulerY(0f)
                            BasicHeadPoseMeasurement.setSumOfHeadEulerZ(0f)

                            MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.warning)
                                .setMessage(R.string.head_pose_retry_msg)
                                .setIcon(R.drawable.ic_warning)
                                .setPositiveButton(R.string.retry) { dialog, _ ->
                                    restartHeadPoseMeasurement()
                                    dialog.dismiss()
                                }
                                .setCancelable(false)
                                .show()
                        }

                    } else {
                        // Set the information about the face graphic overlay
                        //setFaceGraphicOverlay(faces, image, rotation)

                        // Set no face flag to false
                        noFaceDetector.setIsNoFace(false)


                        for (face in faces) {
                            // reset the textView of no face detection
                            tvNoFaceMsg.text = ""

                            // Get the euler angles of the detected face
                            rotX = face.headEulerAngleX
                            rotY = face.headEulerAngleY
                            rotZ = face.headEulerAngleZ

                            val faceMeshResult = faceMeshDetector.process(image)
                                .addOnSuccessListener { faceMeshes ->
                                    if (faceMeshes.size == 0) {
                                        Log.v(TAG, "face mesh no face")
                                        // Reset EAR
                                        drowsinessDetector.resetEAR()
                                    } else {
                                        for (faceMesh in faceMeshes) {
                                            drowsinessDetector.setRotX(rotX)
                                            drowsinessDetector.setRotY(rotY)
                                            drowsinessDetector.setRotZ(rotZ)
                                            drowsinessDetector.calculateEAR(faceMesh.allPoints)
                                        }
                                    }
                                }
                                .addOnFailureListener{ e ->
                                    // TODO: add alert dialog
                                    Log.w(TAG, "Face detector failed. $e")
                                }

                            // Measure the basic head pose for normalization
                            if (BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
                                if (BasicHeadPoseMeasurement.isBasicHeadPoseMeasurementStarting()) {
                                    noFaceDetector.resetDetector()
                                    drowsinessDetector.resetDetector()

                                    measureBasicHeadPose(context, rotX, rotY, rotZ)
                                } else {
                                    BasicHeadPoseMeasurement.setStartTimer(System.currentTimeMillis())
                                }
                            }
                            // Start attention analysis
                            else {
                                // Let the retry button of basic head pose measurement become visible and set the listener
                                btnRetryBasicHeadPoseMeasurement.visibility = View.VISIBLE
                                btnRetryBasicHeadPoseMeasurement.setOnClickListener {
                                    BasicHeadPoseMeasurement.setIsBasicHeadPoseDetecting(true)

                                    MaterialAlertDialogBuilder(context)
                                        .setTitle(R.string.retry_basic_head_pose_measurement_msg)
                                        .setMessage(R.string.retyr_basic_head_pose_measurement_content)
                                        .setPositiveButton(R.string.retry) { dialog, _ ->
                                            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(false)
                                            BasicHeadPoseMeasurement.setTotalFrame(0)
                                            BasicHeadPoseMeasurement.setSumOfHeadEulerX(0f)
                                            BasicHeadPoseMeasurement.setSumOfHeadEulerY(0f)
                                            BasicHeadPoseMeasurement.setSumOfHeadEulerZ(0f)

                                            restartHeadPoseMeasurement()

                                            // Reset the variables of drowsiness and no face detection
                                            drowsinessDetector.resetDetector()
                                            noFaceDetector.resetDetector()

                                            dialog.dismiss()
                                        }
                                        .setNegativeButton(R.string.cancel) {dialog, _ ->
                                            BasicHeadPoseMeasurement.setIsBasicHeadPoseDetecting(false)
                                            dialog.dismiss()
                                        }
                                        .show()
                                }
                            }
                        }

                        // Show the euler angles on the textView
                        rotX -= BasicHeadPoseMeasurement.getBasicHeadEulerX()
                        rotY -= BasicHeadPoseMeasurement.getBasicHeadEulerY()
                        rotZ -= BasicHeadPoseMeasurement.getBasicHeadEulerZ()
                        setEulerAnglesMsg(rotX, rotY, rotZ)

                        //Show the EAR of each eye on the textView
                        if (drowsinessDetector.getEAR() != 1.0f) {
                            setRightEyeMsg(drowsinessDetector.getRightEAR())
                            setLeftEyeMsg(drowsinessDetector.getLeftEAR())
                            setEARMsg(drowsinessDetector.getEAR())
                        }
                    }

                    // No face detection
                    noFaceDetection()

                    // Drowsiness detection
                    drowsinessDetection()

                    // Calculate the processing time
                    val detectionEndTimeMs = System.currentTimeMillis()
                    processTime(detectionStartTimeMs, detectionEndTimeMs)
                }
                .addOnFailureListener { e ->
                    // TODO: Add alert dialog
                    Log.w(TAG, "Face detector failed. $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }

    }

    private fun noFaceDetection() {
        noFaceDetector.increaseTotalFrameNumber()

        noFaceDetector.endNoFaceTimer()
        noFaceDetector.calculateDuration()

        if (noFaceDetector.isNoFace()) {
            noFaceDetector.increaseNoFaceFrameNumber()
        }

        // Show the timer
        tvNoFaceTimer.text = context.getString(R.string.no_face_timer, noFaceDetector.getDuration() / 1000)

        if (noFaceDetector.getDuration() >= noFaceDetector.getDetectionPeriodMs()) {
            noFaceDetector.calculatePerNoFace()
            val perNoFace = noFaceDetector.getPerNoFace()

            if (perNoFace <= noFaceDetector.getSevereNoFaceThreshold()) {
                if (noFaceDetector.isNoFaceDialogShowing()) {
                    noFaceAlertDialog.dismiss()
                    noFaceDetector.setIsNoFaceDialogShowing(false)
                }

                if (noFaceDetector.isAlarmPlaying()) {
                    noFaceDetector.stopAlarm()
                }

                if (noFaceDetector.isVibrating()) {
                    noFaceDetector.stopVibrating()
                }

                if (isAlertDialogShowing) {
                    isAlertDialogShowing = false
                }

                Log.v(TAG, "normal")
            } else {
                //drowsinessDetector.resetDetector()

                if (!isAlertDialogShowing) {
                    if (!noFaceDetector.isAlarmPlaying()) {
                        noFaceDetector.playAlarm()
                    }

                    if (!noFaceDetector.isVibrating()) {
                        noFaceDetector.startVibrating()
                    }

                    if (!noFaceDetector.isNoFaceDialogShowing()) {
                        showNoFaceAlertDialog(context)
                    }

                    isAlertDialogShowing = true
                }

                Log.v(TAG, "no face")
            }

            // Reset the variables
            noFaceDetector.resetDetector()
        }
    }

    private fun drowsinessDetection() {
        drowsinessDetector.increaseTotalFrameNumber()

        drowsinessDetector.endDrowinessTimer()
        drowsinessDetector.calculateDuration()

        if (drowsinessDetector.getEAR() < drowsinessDetector.getClosedEyeThreshold()) {
            drowsinessDetector.increaseClosedEyesFrameNumber()
        }

        // Show the timer
        tvDrowsinessTimer.text = context.getString(R.string.drowsiness_timer, drowsinessDetector.getDuration() / 1000)

        if (drowsinessDetector.getDuration() >= drowsinessDetector.getDetectionPeriodMs()) {
            drowsinessDetector.calculatePerClose()
            val perClose = drowsinessDetector.getPerClose()

            Log.v(TAG, "closed: ${drowsinessDetector.getCloseFrame()}")
            Log.v(TAG, "total: ${drowsinessDetector.getTotalFrame()}")
            Log.v(TAG, "perclose: $perClose")

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

                    if (isAlertDialogShowing) {
                        isAlertDialogShowing = false
                    }

                    Log.v(TAG, "awake")
                }

                perClose > drowsinessDetector.getAwakeThreshold() && perClose <= drowsinessDetector.getFatigueThreshold() -> {
                    Toast.makeText(context, R.string.drowsiness_tired_msg, Toast.LENGTH_SHORT).show()
                    /*Snackbar.make(root, R.string.drowsiness_tired_msg, Snackbar.LENGTH_SHORT)
                        .show()*/

                    Log.v(TAG, "tired")
                }
                else -> {
                    if (!isAlertDialogShowing) {
                        if (!drowsinessDetector.isAlarmPlaying()) {
                            drowsinessDetector.playAlarm()
                        }

                        if (!drowsinessDetector.isVibrating()) {
                            drowsinessDetector.startVibrating()
                        }

                        if (!drowsinessDetector.isFatigueDialogShowing()) {
                            showFatigueAlertDialog(context)
                        }

                        isAlertDialogShowing = true
                    }

                    Log.v(TAG, "fatigue")
                }
            }

            // Reset the variables
            drowsinessDetector.resetDetector()
        }
    }

    private fun restartHeadPoseMeasurement() {
        tvBasicHeadPoseTimer.text = ""

        // Reset drowsiness and no face detectors
        drowsinessDetector.resetDetector()
        noFaceDetector.resetDetector()

        Snackbar.make(root, R.string.basic_head_pose_msg, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.start) {
                BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(true)
                BasicHeadPoseMeasurement.setHasToRestart(false)

                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (!BasicHeadPoseMeasurement.hasToRestart()) {
                            tvBasicHeadPoseTimer.visibility = View.VISIBLE
                            tvBasicHeadPoseTimer.text = String.format(context.getString(R.string.basic_head_pose_counting_msg), (millisUntilFinished / 1000))
                            /*Snackbar.make(
                                root,
                                String.format(
                                    context.getString(R.string.basic_head_pose_counting_msg),
                                    (millisUntilFinished / 1000).toInt()),
                                Snackbar.LENGTH_INDEFINITE
                            ).show()*/
                        }
                    }

                    override fun onFinish() {
                        if (!BasicHeadPoseMeasurement.hasToRestart()) {
                            tvBasicHeadPoseTimer.visibility = View.INVISIBLE
                            Snackbar.make(root, R.string.basic_head_pose_complete_msg, Snackbar.LENGTH_SHORT)
                                .show()

                            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            val mediaPlayer = MediaPlayer.create(context, notificationUri)

                            if (mediaPlayer == null) {
                                Log.e(TAG, "mediaPlayer create failed")
                            } else {
                                mediaPlayer.start()

                                if (!mediaPlayer.isPlaying) {
                                    mediaPlayer.release()
                                }
                            }
                        }
                    }

                }.start()
            }
            .show()
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


    private fun showFatigueAlertDialog(context: Context) {
        drowsinessDetector.setIsFatigueDialogShowing(true)

        this.fatigueAlertDialog = MaterialAlertDialogBuilder(context)
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

                drowsinessDetector.setIsFatigueDialogShowing(false)
                isAlertDialogShowing = false

                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showNoFaceAlertDialog(context: Context) {
        noFaceDetector.setIsNoFaceDialogShowing(true)

        this.noFaceAlertDialog = MaterialAlertDialogBuilder(context)
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

                noFaceDetector.setIsNoFaceDialogShowing(false)
                isAlertDialogShowing = false

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
        tvRightEAR.text = ""
        tvLeftEAR.text = ""
        tvLatencyTime.text = ""
        tvEAR.text = ""
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

    private fun setLeftEyeMsg(leftEAR: Float) {
        tvLeftEAR.text = String.format(context.getString(R.string.left_eye_open), leftEAR)
    }

    private fun setRightEyeMsg(rightEAR: Float) {
        tvRightEAR.text = String.format(context.getString(R.string.left_eye_open), rightEAR)
    }

    private fun setEARMsg(ear: Float) {
        tvEAR.text = String.format(context.getString(R.string.ear), ear)
    }

    companion object {
        private const val TAG = "FaceDetectionProcessor"
    }

}