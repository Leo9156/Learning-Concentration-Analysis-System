package com.example.learningassistance

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Half.toFloat
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnticipateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var nbpTime: NumberPicker
    private lateinit var tvTime: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set splash screen
        val splashScreen = installSplashScreen()
        var isReady = false
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_X,
                0f,
                -splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = DecelerateInterpolator()
            slideUp.duration = 700L

            slideUp.doOnEnd { splashScreenView.remove() }

            slideUp.start()
        }

        setContentView(R.layout.activity_main)
        // Increase the splash screen duration
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )
        Thread.sleep(700)
        isReady = true

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