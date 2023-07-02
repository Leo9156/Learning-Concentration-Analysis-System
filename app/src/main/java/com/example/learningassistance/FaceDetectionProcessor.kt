package com.example.learningassistance

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.learningassistance.graphicOverlay.FaceDetectionGraphicOverlay
import com.example.learningassistance.graphicOverlay.FaceMeshGraphicOverlay
import com.example.learningassistance.facedetection.*
import com.example.learningassistance.graphicOverlay.PoseGraphicOverlay
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class FaceDetectionProcessor(
    private val context: Context,
    private val faceDetectionGraphicOverlay: FaceDetectionGraphicOverlay,
    private val faceMeshGraphicOverlay: FaceMeshGraphicOverlay,
    private val poseGraphicOverlay: PoseGraphicOverlay,
    private val root: ConstraintLayout,
    private val cameraPreviewActivity: CameraPreviewActivity,
    private val tvHeadPoseAttentionAnalyzerTimer: TextView,
    private val btnRetryBasicHeadPoseMeasurement: MaterialButton
): ImageAnalysis.Analyzer {

    // The rotation degree of pitch, yaw, and row
    private var rotX = 0f
    private var rotY = 0f
    private var rotZ = 0f

    // The media player for basic head pose measurement
    private lateinit var mediaPlayer: MediaPlayer

    // The count down timer of the basic head pose
    private var basicHeadPoseTimer: CountDownTimer? = null

    // Create the drowsiness detector
    private val drowsinessDetector = DrowsinessDetection(context)
    private lateinit var fatigueAlertDialog: AlertDialog

    // Create the no face detector
    private val noFaceDetector = NoFaceDetection(context)
    private lateinit var noFaceAlertDialog: AlertDialog

    private var isAlertDialogShowing = false

    // Create the face detector of ML kit
    private val faceDetectionOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        //.setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()
    private val faceDetectionDetector = FaceDetection.getClient(faceDetectionOptions)

    // Create the face mesh detector of ML kit
    private val faceMeshOptions = FaceMeshDetectorOptions.Builder()
    private val faceMeshDetector = FaceMeshDetection.getClient(faceMeshOptions.build())

    // Create the analyzer based on head pose
    private val headPoseAttentionAnalyzer = HeadPoseAttentionAnalysis(context, root, tvHeadPoseAttentionAnalyzerTimer)

    // Create the pose detector of ML kit
    private val poseDetectOptions = AccuratePoseDetectorOptions.Builder()
        .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector = PoseDetection.getClient(poseDetectOptions)

    // Create the object detector
    private val localModel = LocalModel.Builder()  // Get the model path
        .setAssetFilePath("model_int8_qat.tflite")
        .build()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Get the image
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detectionStartTimeMs = System.currentTimeMillis()

            // Process the image
            val faceDetectionResult = faceDetectionDetector.process(image)
                .addOnSuccessListener { faces ->
                    // Set the information about the face graphic overlay
                    setFaceDetectionGraphicOverlay(faces, image)

                    // Dealing with no face detected situations
                    if (faces.size == 0) {
                        setNoFaceMsg()  // Show the no face message to the user
                        noFaceDetector.setIsNoFace(true)  // Set no face flag to true
                        drowsinessDetector.resetEAR()   // Reset leftEyeEAR and rightEyeEAR
                        drowsinessDetector.resetMOR()   // Reset MOR
                        restartBasicHeadPoseMeasurement()  // Restart basic head pose measuring if the user is measuring basic head pose
                    } else {
                        // Set no face flag to false
                        noFaceDetector.setIsNoFace(false)

                        for (face in faces) {
                            // reset the textView of no face detection
                            cameraPreviewActivity.resetNoFaceTextView()

                            // Get the euler angles of the detected face
                            rotX = face.headEulerAngleX
                            rotY = face.headEulerAngleY
                            rotZ = face.headEulerAngleZ

                            // Measure the basic head pose for normalization
                            if (BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
                                headPoseAttentionAnalyzer.resetAnalyzer()

                                if (BasicHeadPoseMeasurement.isBasicHeadPoseMeasurementStarting()) {
                                    noFaceDetector.resetDetector()
                                    drowsinessDetector.resetDetector()
                                    measureBasicHeadPose(context, rotX, rotY, rotZ)
                                } else {
                                    BasicHeadPoseMeasurement.setStartTimer(System.currentTimeMillis())
                                }
                            }
                            else {
                                // Let the retry button of basic head pose measurement become visible and set the listener
                                btnRetryBasicHeadPoseMeasurement.visibility = View.VISIBLE
                                btnRetryBasicHeadPoseMeasurement.setOnClickListener {
                                    retryBasicHeadPoseMeasurement()
                                }
                            }
                        }

                        // Show the normalized euler angles on the textView
                        rotX -= BasicHeadPoseMeasurement.getBasicHeadEulerX()
                        rotY -= BasicHeadPoseMeasurement.getBasicHeadEulerY()
                        rotZ -= BasicHeadPoseMeasurement.getBasicHeadEulerZ()
                        setEulerAnglesMsg(rotX, rotY, rotZ)
                    }

                    // No face detection
                    noFaceDetection()

                    // Attention analysis based on head pose
                    headPoseAttentivenessAnalysis(rotX, rotY, rotZ)
                }
                .addOnFailureListener { e ->
                    // TODO: Add alert dialog
                    Log.e(TAG, "Face detector failed. $e")
                }
                .addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()
                }

            val faceMeshResult = faceMeshDetector.process(image)
                .addOnSuccessListener { faceMeshes ->
                    // Set face mesh graphic overlay
                    setFaceMeshGraphicOverlay(faceMeshes, image)

                    if (faceMeshes.size == 0) {
                        Log.v(TAG, "face mesh no face")
                        // Reset EAR
                        drowsinessDetector.resetEAR()
                        // Reset MOR
                        drowsinessDetector.resetMOR()
                    } else {
                        for (faceMesh in faceMeshes) {
                            drowsinessDetector.calculateEAR(faceMesh.allPoints)

                            drowsinessDetector.calculateMOR(faceMesh.allPoints)
                            //Show the EAR of each eye on the textView
                            if (drowsinessDetector.getEAR() != 1.0f && !noFaceDetector.isNoFace()) {
                                setRightEyeMsg(drowsinessDetector.getRightEAR())
                                setLeftEyeMsg(drowsinessDetector.getLeftEAR())
                                setEARMsg(drowsinessDetector.getEAR())
                            }
                        }
                    }
                    // Drowsiness detection
                    drowsinessDetection()
                }
                .addOnFailureListener { e ->
                // TODO: add alert dialog
                Log.w(TAG, "Face mesh detector failed. $e")
                }
                .addOnCompleteListener {
                    mediaImage.close()
                    imageProxy.close()
                }

            /*val poseDetectionResult = poseDetector.process(image)
                .addOnSuccessListener { poses ->
                    setPoseGraphicOverlay(poses, image)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Pose detector failed. $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }*/

            // Calculate the processing time
            val detectionEndTimeMs = System.currentTimeMillis()
            processTime(detectionStartTimeMs, detectionEndTimeMs)

            mediaImage.close()
        }

    }

    private fun headPoseAttentivenessAnalysis(rotX: Float, rotY: Float, rotZ: Float) {
        if (!BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
            if (noFaceDetector.isNoFace()) {
                headPoseAttentionAnalyzer.increaseTotalInattentionFrame()
            } else {
                headPoseAttentionAnalyzer.analyzeHeadPose(rotX, rotY, rotZ)
            }

            headPoseAttentionAnalyzer.evaluateAttention()

            headPoseAttentionAnalyzer.analyzeAttentiveness()
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
        cameraPreviewActivity.setNoFaceTimerTextView(noFaceDetector.getDuration() / 1000)

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
        cameraPreviewActivity.setDrowsinessTimerTextView(drowsinessDetector.getDuration() / 1000)

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
        //Yawning Detection
        drowsinessDetector.endYawningTimer()
        drowsinessDetector.calculateYawningDetectDuration()

        if (drowsinessDetector.getMOR() >= drowsinessDetector.getYawningThreshold()) {
            when {
                drowsinessDetector.getYawningDetectionDuration() <= drowsinessDetector.getDetectionYawningPeriodMs() -> {
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
        }
        else {
            drowsinessDetector.resetYawningDetect()
        }

    }

    private fun restartBasicHeadPoseMeasurement() {
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
    }

    private fun retryBasicHeadPoseMeasurement() {
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

    private fun restartHeadPoseMeasurement() {
        cameraPreviewActivity.resetBasicHeadPoseTimerTextView()

        // Reset drowsiness and no face detectors
        drowsinessDetector.resetDetector()
        noFaceDetector.resetDetector()

        Snackbar.make(root, R.string.basic_head_pose_msg, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.start) {
                BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(true)
                BasicHeadPoseMeasurement.setHasToRestart(false)

                basicHeadPoseTimer = object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (!BasicHeadPoseMeasurement.hasToRestart()) {
                            cameraPreviewActivity.setBasicHeadPoseTimerTextViewVisibility(true)
                            cameraPreviewActivity.setBasicHeadPoseTimerTexView(millisUntilFinished / 1000)
                            /*Snackbar.make(
                                root,
                                String.format(
                                    context.getString(R.string.basic_head_pose_counting_msg),
                                    (millisUntilFinished / 1000).toInt()),
                                Snackbar.LENGTH_INDEFINITE
                            ).show()*/
                        } else {
                            if (basicHeadPoseTimer != null) {
                                basicHeadPoseTimer!!.cancel()
                            }
                        }
                    }

                    override fun onFinish() {
                        if (!BasicHeadPoseMeasurement.hasToRestart()) {
                            cameraPreviewActivity.setBasicHeadPoseTimerTextViewVisibility(false)
                            Snackbar.make(root, R.string.basic_head_pose_complete_msg, Snackbar.LENGTH_SHORT)
                                .show()

                            //val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                            //mediaPlayer = MediaPlayer.create(context, notificationUri)
                            mediaPlayer = MediaPlayer.create(context, R.raw.basic_head_pose_complete)
                            mediaPlayer.setOnCompletionListener { mp ->
                                mp.release()
                            }
                            mediaPlayer.start()
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
        cameraPreviewActivity.setLatencyTextView(latencyMsg)
    }

    private fun setFaceDetectionGraphicOverlay(faces: List<Face>, image: InputImage) {
        faceDetectionGraphicOverlay.setFace(faces)
        Log.v(TAG, "rotate: ${image.rotationDegrees}")
        faceDetectionGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
    }

    private fun setFaceMeshGraphicOverlay(faceMeshes: MutableList<FaceMesh>, image: InputImage) {
        faceMeshGraphicOverlay.setFace(faceMeshes)
        faceMeshGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
    }

    private fun setPoseGraphicOverlay(poses: Pose, image: InputImage) {
        poseGraphicOverlay.setPoses(poses)
        poseGraphicOverlay.setTransformationInfo(image.width, image.height, image.rotationDegrees)
    }

    private fun setNoFaceMsg() {
        cameraPreviewActivity.setNoFaceTextView()
    }

    private fun setEulerAnglesMsg(rotX: Float, rotY: Float, rotZ: Float) {
        cameraPreviewActivity.setEulerAnglesTextView(rotX, rotY, rotZ)
    }

    private fun setLeftEyeMsg(leftEAR: Float) {
        cameraPreviewActivity.setLeftEyeTextView(leftEAR)
    }

    private fun setRightEyeMsg(rightEAR: Float) {
        cameraPreviewActivity.setRightEyeTextView(rightEAR)
    }

    private fun setEARMsg(ear: Float) {
        cameraPreviewActivity.setEARTextView(ear)
    }
    //just a void fun, after building the setMORTextView, remove the '//' below
    private fun setMORMsg(MOR: Float) {
        //cameraPreviewActivity.setMORTextView(MOR)
    }
    companion object {
        private const val TAG = "FaceDetectionProcessor"
    }
}