package com.example.learningassistance

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
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
import com.example.learningassistance.facedetection.FaceGraphicOverlayView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var faceGraphicOverlayView: FaceGraphicOverlayView
    private lateinit var textViewEulerX: TextView
    private lateinit var textViewEulerY: TextView
    private lateinit var textViewEulerZ: TextView
    private lateinit var textViewRightEyeOpenProb: TextView
    private lateinit var textViewLeftEyeOpenProb: TextView
    private lateinit var textViewLatencyTime: TextView
    private lateinit var textViewDrowsinessMsg: TextView
    private lateinit var textViewDrowsinessTimer: TextView
    private var cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.viewFinder)
        viewFinder.scaleType = PreviewView.ScaleType.FILL_CENTER
        faceGraphicOverlayView = findViewById(R.id.faceGraphicOverlay)
        textViewEulerX = findViewById(R.id.textViewEulerX)
        textViewEulerY = findViewById(R.id.textViewEulerY)
        textViewEulerZ = findViewById(R.id.textViewEulerZ)
        textViewRightEyeOpenProb = findViewById(R.id.textViewRightEyeOpenProb)
        textViewLeftEyeOpenProb = findViewById(R.id.textViewLeftEyeOpenProb)
        textViewLatencyTime = findViewById(R.id.textViewLatencyTime)
        textViewDrowsinessMsg = findViewById(R.id.textViewDrowsinessMsg)
        textViewDrowsinessTimer = findViewById(R.id.textViewDrowsinessTimer)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
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
            val cameraProvider: ProcessCameraProvider = cameraFutureProvider.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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
                    faceGraphicOverlayView,
                    textViewEulerX,
                    textViewEulerY,
                    textViewEulerZ,
                    textViewRightEyeOpenProb,
                    textViewLeftEyeOpenProb,
                    textViewLatencyTime,
                    textViewDrowsinessMsg,
                    textViewDrowsinessTimer
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
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }


}