package com.example.learningassistance

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
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
import com.example.learningassistance.facedetection.FaceDetectionProcessor
import com.example.learningassistance.facedetection.FaceGraphicOverlayView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var nbpTime: NumberPicker
    private lateinit var tvTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart = findViewById(R.id.buttonStart)
        nbpTime = findViewById(R.id.timePicker)
        tvTime = findViewById(R.id.learningTime)

        nbpTime.maxValue = 120
        nbpTime.minValue = 1
        nbpTime.value = 60
        nbpTime.setOnValueChangedListener { _, _, newVal ->
            tvTime.text = String.format(getString(R.string.main_activity_timer), newVal)
        }

        btnStart.setOnClickListener {
            showHeadPoseAlertDialog(this, nbpTime.value)
        }
    }

    private fun showHeadPoseAlertDialog(context: Context, learningTime: Int) {
        MaterialAlertDialogBuilder(context)
            .setTitle("NOTICE")
            .setIcon(R.drawable.ic_notification)
            .setMessage(getString(R.string.start_detection_notification_msg))
            .setPositiveButton(getString(R.string.understand)) { dialog, _ ->
                val intent = Intent(this, CameraPreviewActivity::class.java)

                val bundle = Bundle()
                bundle.putInt("LEARNING_TIME", learningTime)
                intent.putExtras(bundle)

                startActivity(intent)

                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}