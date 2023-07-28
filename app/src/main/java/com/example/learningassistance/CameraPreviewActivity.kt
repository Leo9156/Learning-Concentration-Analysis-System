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
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.learningassistance.databinding.ActivityCameraPreviewBinding
import com.example.learningassistance.facedetection.BasicHeadPoseMeasurement
import com.example.learningassistance.facedetection.FaceDetectionProcessor
import com.example.learningassistance.objectdetection.ObjectDetectionProcessor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraPreviewBinding

    // Processors
    private lateinit var faceDetectionProcessor: FaceDetectionProcessor
    private lateinit var objectDetectionProcessor: ObjectDetectionProcessor

    private var basicHeadPoseTimer: CountDownTimer? = null
    private var learningTime: Int = 0
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor = Executors.newSingleThreadExecutor()
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        faceDetectionProcessor = FaceDetectionProcessor(
            this,
            binding.faceDetectionGraphicOverlay,
            binding.faceMeshGraphicOverlay,
            binding.poseGraphicOverlay,
            binding.root,
            this,
            binding.textViewHeadPoseAttentionAnalyzerTimer,
            binding.buttonRetryBasicHeadPoseMeasurement
        )

        objectDetectionProcessor = ObjectDetectionProcessor(
            this,
            binding.objectDetectionGraphicOverlay,
            this)

        // Hide the btnRetryBasicHeadPoseMeasurement when the basic head pose is detecting
        if (BasicHeadPoseMeasurement.isBasicHeadPoseDetecting()) {
            binding.buttonRetryBasicHeadPoseMeasurement.visibility = View.INVISIBLE
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
        val snackBar = Snackbar.make(binding.root, R.string.basic_head_pose_msg, Snackbar.LENGTH_INDEFINITE)
        snackBar.setAction(R.string.start) {
            BasicHeadPoseMeasurement.setIsBasicHeadPoseMeasurementStarting(true)

            basicHeadPoseTimer = object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    if (!BasicHeadPoseMeasurement.hasToRestart()) {
                        binding.textViewBasicHeadPoseTimer.visibility = View.VISIBLE
                        binding.textViewBasicHeadPoseTimer.text = String.format(context.getString(R.string.basic_head_pose_counting_msg), (millisUntilFinished / 1000))
                    } else {
                        if (basicHeadPoseTimer != null) {
                            basicHeadPoseTimer!!.cancel()
                        }
                    }
                }

                override fun onFinish() {
                    if (!BasicHeadPoseMeasurement.hasToRestart()) {
                        Snackbar.make(
                            binding.root,
                            R.string.basic_head_pose_complete_msg,
                            Snackbar.LENGTH_SHORT
                        )
                            .show()

                        binding.textViewBasicHeadPoseTimer.visibility = View.INVISIBLE

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

                binding.textViewLearningTimer.text = String.format(getString(R.string.learning_count_down_timer), minute, second)
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
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
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
            binding.textViewLatencyTime.text = msg
        }
    }

    fun setNoFaceTextView() {
        binding.textViewEulerX.text = ""
        binding.textViewEulerY.text = ""
        binding.textViewEulerZ.text = ""
        binding.textViewRightEAR.text = ""
        binding.textViewLeftEAR.text = ""
        binding.textViewLatencyTime.text = ""
        binding.textViewEAR.text = ""
        binding.textViewNoFaceMsg.text = getString(R.string.no_face_detected_info)
    }

    fun setEulerAnglesTextView(rotX: Float, rotY: Float, rotZ: Float) {
        val eulerXMsg = String.format(getString(R.string.eulerx), rotX)
        val eulerYMsg = String.format(getString(R.string.eulery), rotY)
        val eulerZMsg = String.format(getString(R.string.eulerz), rotZ)
        binding.textViewEulerX.text = eulerXMsg
        binding.textViewEulerY.text = eulerYMsg
        binding.textViewEulerZ.text = eulerZMsg
    }

    fun setLeftEyeTextView(leftEAR: Float) {
        binding.textViewLeftEAR.text = String.format(getString(R.string.left_eye_open), leftEAR)
    }

    fun setRightEyeTextView(rightEAR: Float) {
        binding.textViewRightEAR.text = String.format(getString(R.string.right_eye_open), rightEAR)
    }

    fun setEARTextView(ear: Float) {
        binding.textViewEAR.text = String.format(getString(R.string.ear), ear)
    }

    fun resetNoFaceTextView() {
        binding.textViewNoFaceMsg.text = ""
    }

    fun setDrowsinessTimerTextView(time: Long) {
        runOnUiThread {
            binding.textViewDrowsinessTimer.text = getString(R.string.drowsiness_timer, time)

        }
    }

    fun setNoFaceTimerTextView(time: Long) {
        runOnUiThread {
            binding.textViewNoFaceTimer.text = getString(R.string.no_face_timer, time)
        }
    }

    fun setBasicHeadPoseTimerTexView(time: Long) {
        binding.textViewBasicHeadPoseTimer.text = String.format(getString(R.string.basic_head_pose_counting_msg), time)
    }

    fun setBasicHeadPoseTimerTextViewVisibility(flag: Boolean) = if (flag) {
        binding.textViewBasicHeadPoseTimer.visibility = View.VISIBLE
    } else {
        binding.textViewBasicHeadPoseTimer.visibility = View.INVISIBLE
    }

    fun resetBasicHeadPoseTimerTextView() {
        binding.textViewBasicHeadPoseTimer.text = ""
    }

    fun setObjectDetectionMessageTextView(msg: String) {
        runOnUiThread {
            binding.textViewObjectMsg.text = msg
        }
    }

    fun resetObjectDetectionMessageTextView() {
        runOnUiThread {
            binding.textViewObjectMsg.text = "object detection: "
        }
    }

    companion object {
        private const val TAG = "CameraPreviewActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}