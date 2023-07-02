package com.example.learningassistance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.learningassistance.graphicOverlay.FaceDetectionGraphicOverlay
import com.example.learningassistance.graphicOverlay.FaceMeshGraphicOverlay
import com.example.learningassistance.facedetection.BasicHeadPoseMeasurement
import com.example.learningassistance.graphicOverlay.ObjectDetectionGraphicOverlay
import com.example.learningassistance.graphicOverlay.PoseGraphicOverlay
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var root: ConstraintLayout
    private lateinit var viewFinder: PreviewView

    // Graphic overlay
    private lateinit var faceDetectionGraphicOverlay: FaceDetectionGraphicOverlay
    private lateinit var faceMeshGraphicOverlay: FaceMeshGraphicOverlay
    private lateinit var poseGraphicOverlay: PoseGraphicOverlay
    private lateinit var objectDetectionGraphicOverlay: ObjectDetectionGraphicOverlay

    // Messages
    private lateinit var textViewEulerX: TextView
    private lateinit var textViewEulerY: TextView
    private lateinit var textViewEulerZ: TextView
    private lateinit var textViewRightEAR: TextView
    private lateinit var textViewLeftEAR: TextView
    private lateinit var textViewEAR: TextView
    private lateinit var textViewLatencyTime: TextView
    private lateinit var textViewLearningTimer: TextView
    private lateinit var textViewNoFaceMsg: TextView
    private lateinit var textViewDrowsinessTimer: TextView
    private lateinit var textViewNoFaceTimer: TextView
    private lateinit var textViewHeadPoseAttentionAnalyzerTimer: TextView
    private lateinit var textViewBasicHeadPoseTimer: TextView
    private lateinit var textViewObjectMsg: TextView

    private lateinit var btnRetryBasicHeadPoseMeasurement: MaterialButton
    private lateinit var faceDetectionProcessor: FaceDetectionProcessor
    private lateinit var objectDetectionProcessor: ObjectDetectionProcessor
    private var basicHeadPoseTimer: CountDownTimer? = null
    private var learningTime: Int = 0
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        root = findViewById(R.id.root)
        viewFinder = findViewById(R.id.viewFinder)

        faceDetectionGraphicOverlay = findViewById(R.id.faceDetectionGraphicOverlay)
        faceMeshGraphicOverlay = findViewById(R.id.faceMeshGraphicOverlay)
        poseGraphicOverlay = findViewById(R.id.poseGraphicOverlay)
        objectDetectionGraphicOverlay = findViewById(R.id.objectDetectionGraphicOverlay)

        textViewEulerX = findViewById(R.id.textViewEulerX)
        textViewEulerY = findViewById(R.id.textViewEulerY)
        textViewEulerZ = findViewById(R.id.textViewEulerZ)
        textViewRightEAR = findViewById(R.id.textViewRightEAR)
        textViewLeftEAR = findViewById(R.id.textViewLeftEAR)
        textViewEAR = findViewById(R.id.textViewEAR)
        textViewLatencyTime = findViewById(R.id.textViewLatencyTime)
        textViewLearningTimer = findViewById(R.id.textViewLearningTimer)
        textViewNoFaceMsg = findViewById(R.id.textViewNoFaceMsg)
        textViewDrowsinessTimer = findViewById(R.id.textViewDrowsinessTimer)
        textViewNoFaceTimer= findViewById(R.id.textViewNoFaceTimer)
        textViewHeadPoseAttentionAnalyzerTimer= findViewById(R.id.textViewHeadPoseAttentionAnalyzerTimer)
        textViewBasicHeadPoseTimer= findViewById(R.id.textViewBasicHeadPoseTimer)
        textViewObjectMsg = findViewById(R.id.textViewObjectMsg)

        btnRetryBasicHeadPoseMeasurement = findViewById(R.id.buttonRetryBasicHeadPoseMeasurement)

        faceDetectionProcessor = FaceDetectionProcessor(
            this,
            faceDetectionGraphicOverlay,
            faceMeshGraphicOverlay,
            poseGraphicOverlay,
            root,
            this,
            textViewHeadPoseAttentionAnalyzerTimer,
            btnRetryBasicHeadPoseMeasurement
        )

        objectDetectionProcessor = ObjectDetectionProcessor(this, objectDetectionGraphicOverlay, textViewObjectMsg, this)

        //textViewHeadPoseAttentionAnalyzerTimer.visibility = View.INVISIBLE
        //textViewDrowsinessTimer.visibility = View.INVISIBLE
        //textViewNoFaceTimer.visibility = View.INVISIBLE

        // Hide the btnRetryBasicHeadPoseMeasurement when the basic head pose is detecting
        if (BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
            btnRetryBasicHeadPoseMeasurement.visibility = View.INVISIBLE
        }

        // Get the info from the MainActivity
        val intent: Intent = intent
        if (intent.extras?.getInt("LEARNING_TIME") != null) {
            learningTime = intent.extras!!.getInt("LEARNING_TIME")
        }

        if (allPermissionsGranted()) {
            startCamera()
            startLearningTimer(this)
            showBasicHeadPoseMeasurementSnackBar(this)

        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BasicHeadPoseMeasurement.resetProperties()
    }

    private fun showBasicHeadPoseMeasurementSnackBar(context: Context) {
        val snackBar = Snackbar.make(root, R.string.basic_head_pose_msg, Snackbar.LENGTH_INDEFINITE)
        snackBar.setAction(R.string.start) {
            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(true)

            basicHeadPoseTimer = object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (!BasicHeadPoseMeasurement.hasToRestart()) {
                        textViewBasicHeadPoseTimer.visibility = View.VISIBLE
                        textViewBasicHeadPoseTimer.text = String.format(context.getString(R.string.basic_head_pose_counting_msg), (millisUntilFinished / 1000))
                    } else {
                        if (basicHeadPoseTimer != null) {
                            basicHeadPoseTimer!!.cancel()
                        }
                    }
                }

                override fun onFinish() {
                    if (!BasicHeadPoseMeasurement.hasToRestart()) {
                        Snackbar.make(
                            root,
                            R.string.basic_head_pose_complete_msg,
                            Snackbar.LENGTH_SHORT
                        )
                            .show()

                        textViewBasicHeadPoseTimer.visibility = View.INVISIBLE

                        mediaPlayer = MediaPlayer.create(context, R.raw.basic_head_pose_complete)
                        mediaPlayer.setOnCompletionListener { mp ->
                            mp.release()
                        }
                        mediaPlayer.start()
                    } else {
                        snackBar.dismiss()
                    }
                }
            }.start()
        }
            .show()
    }

    private fun startLearningTimer(context: Context) {
        object : CountDownTimer((learningTime * 60 * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsUntilFinished = millisUntilFinished / 1000
                val minute = secondsUntilFinished / 60
                val second = secondsUntilFinished - (minute * 60)

                textViewLearningTimer.text = String.format(getString(R.string.learning_count_down_timer), minute, second)
            }

            override fun onFinish() {
                cameraProvider.unbindAll()
                showFinishAlertDialog(context)
            }

        }.start()
    }

    private fun showFinishAlertDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.complete_learning_title))
            .setIcon(R.drawable.ic_congratulation)
            .setMessage(R.string.complete_learning_msg)
            .setPositiveButton(R.string.view_results) { dialog, _ ->
                // TODO: Switch to the result (After implementing result activity or fragment)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                    startCamera()
            }
            else {
                Toast.makeText(this, "Permissions not granted by the user", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraFutureProvider = ProcessCameraProvider.getInstance(this)

        cameraFutureProvider.addListener({
            //Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraFutureProvider.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            val orientation = this.resources.configuration.orientation
            val targetSolution = if (orientation == 1) {
                Size(480, 640)
            } else {
                Size(640, 480)
            }

            // Preview usecase
            val preview = Preview.Builder()
                .setTargetResolution(targetSolution)
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalysisYUV = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val imageAnalysisRGB = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysisYUV.setAnalyzer(
                cameraExecutor,
                faceDetectionProcessor
            )

            imageAnalysisRGB.setAnalyzer(
                cameraExecutor,
                objectDetectionProcessor
            )

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisYUV, imageAnalysisRGB, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    fun setLatencyTextView(msg: String) {
        runOnUiThread {
            textViewLatencyTime.text = msg
        }
    }

    fun setNoFaceTextView() {
        textViewEulerX.text = ""
        textViewEulerY.text = ""
        textViewEulerZ.text = ""
        textViewRightEAR.text = ""
        textViewLeftEAR.text = ""
        textViewLatencyTime.text = ""
        textViewEAR.text = ""
        textViewNoFaceMsg.text = getString(R.string.no_face_detected_info)
    }

    /*private fun setFaceGraphicOverlay(faces: List<Face>, image: InputImage, rotation: Int) {
        fgOverlay.setFace(faces)
        fgOverlay.setRotation(rotation)
        fgOverlay.setImageSize(image.width.toFloat(), image.height.toFloat())
        fgOverlay.setPreviewSize(fgOverlay.width.toFloat(), fgOverlay.height.toFloat())
    }*/


    fun setEulerAnglesTextView(rotX: Float, rotY: Float, rotZ: Float) {
        val eulerXMsg = String.format(getString(R.string.eulerx), rotX)
        val eulerYMsg = String.format(getString(R.string.eulery), rotY)
        val eulerZMsg = String.format(getString(R.string.eulerz), rotZ)
        textViewEulerX.text = eulerXMsg
        textViewEulerY.text = eulerYMsg
        textViewEulerZ.text = eulerZMsg
    }

    fun setLeftEyeTextView(leftEAR: Float) {
        textViewLeftEAR.text = String.format(getString(R.string.left_eye_open), leftEAR)
    }

    fun setRightEyeTextView(rightEAR: Float) {
        textViewRightEAR.text = String.format(getString(R.string.right_eye_open), rightEAR)
    }

    fun setEARTextView(ear: Float) {
        textViewEAR.text = String.format(getString(R.string.ear), ear)
    }

    fun resetNoFaceTextView() {
        textViewNoFaceMsg.text = ""
    }

    fun setDrowsinessTimerTextView(time: Long) {
        runOnUiThread {
            textViewDrowsinessTimer.text = getString(R.string.drowsiness_timer, time)

        }
    }

    fun setNoFaceTimerTextView(time: Long) {
        runOnUiThread {
            textViewNoFaceTimer.text = getString(R.string.no_face_timer, time)
        }
    }

    fun setBasicHeadPoseTimerTexView(time: Long) {
        textViewBasicHeadPoseTimer.text = String.format(getString(R.string.basic_head_pose_counting_msg), time)
    }

    fun setBasicHeadPoseTimerTextViewVisibility(flag: Boolean) = if (flag) {
        textViewBasicHeadPoseTimer.visibility = View.VISIBLE
    } else {
        textViewBasicHeadPoseTimer.visibility = View.INVISIBLE
    }

    fun resetBasicHeadPoseTimerTextView() {
        textViewBasicHeadPoseTimer.text = ""
    }

    fun setObjectDetectionMessageTextView(msg: String) {
        runOnUiThread {
            textViewObjectMsg.text = msg
        }
    }

    fun resetObjectDetectionMessageTextView() {
        runOnUiThread {
            textViewObjectMsg.text = "object detection: "
        }
    }

    companion object {
        private const val TAG = "CameraPreviewActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}