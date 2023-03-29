package com.example.learningassistance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender.OnFinished
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.learningassistance.facedetection.FaceDetectionProcessor
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    //private lateinit var btnStartDetection: Button
    //private lateinit var faceGraphicOverlayView: FaceGraphicOverlayView
    private lateinit var textViewEulerX: TextView
    private lateinit var textViewEulerY: TextView
    private lateinit var textViewEulerZ: TextView
    private lateinit var textViewRightEyeOpenProb: TextView
    private lateinit var textViewLeftEyeOpenProb: TextView
    private lateinit var textViewLatencyTime: TextView
    private lateinit var textViewLearningTimer: TextView
    private lateinit var textViewNoFaceMsg: TextView
    private var learningTime: Int = 0
    private lateinit var cameraProvider: ProcessCameraProvider
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        viewFinder = findViewById(R.id.viewFinder)
        //btnStartDetection = findViewById(R.id.buttonStartDetection)
        //faceGraphicOverlayView = findViewById(R.id.faceGraphicOverlay)
        textViewEulerX = findViewById(R.id.textViewEulerX)
        textViewEulerY = findViewById(R.id.textViewEulerY)
        textViewEulerZ = findViewById(R.id.textViewEulerZ)
        textViewRightEyeOpenProb = findViewById(R.id.textViewRightEyeOpenProb)
        textViewLeftEyeOpenProb = findViewById(R.id.textViewLeftEyeOpenProb)
        textViewLatencyTime = findViewById(R.id.textViewLatencyTime)
        textViewLearningTimer = findViewById(R.id.textViewLearningTimer)
        textViewNoFaceMsg = findViewById(R.id.textViewNoFaceMsg)

        // Get the info from the MainActivity
        val intent: Intent = intent
        if (intent.extras?.getInt("LEARNING_TIME") != null) {
            learningTime = intent.extras!!.getInt("LEARNING_TIME")
        }

        if (allPermissionsGranted()) {
            startCamera()
            startLearningTimer(this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
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
            .setIcon(R.drawable.ic_smile)
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

            Log.v(TAG, "resolution: ${targetSolution}")

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(targetSolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(
                cameraExecutor,
                FaceDetectionProcessor(
                    this,
                    //faceGraphicOverlayView,
                    textViewEulerX,
                    textViewEulerY,
                    textViewEulerZ,
                    textViewRightEyeOpenProb,
                    textViewLeftEyeOpenProb,
                    textViewLatencyTime,
                    textViewNoFaceMsg
                )
            )

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /*private fun getTargetResolution(): Size {
        return when(resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> Size(480, 640)
            Configuration.ORIENTATION_LANDSCAPE -> Size(640, 480)
            else -> Size(640, 480)
        }
    }*/
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "CameraPreviewActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}