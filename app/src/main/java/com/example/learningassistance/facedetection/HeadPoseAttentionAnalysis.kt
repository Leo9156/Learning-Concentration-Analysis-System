package com.example.learningassistance.facedetection

import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.learningassistance.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

// TODO: Design more accurate method to distinguish look around and look at other side for a period of time
class HeadPoseAttentionAnalysis(
    private val context: Context,
) {
    private var slidingWindowSize = 30
    private var totalAttentionFrame = 0
    private var totalInattentionFrame = 0
    private var xPositiveThreshold = 20.0f
    private var xNegativeThreshold = -20.0f
    private var yPositiveThreshold = 20.0f
    private var yNegativeThreshold = -20.0f
    private var isAttention = true
    private lateinit var mediaPlayer: MediaPlayer
    var isHeadPoseAnalyzing = false
    var isAlertDialogShowing = false
    var isDistracted = false


    // TODO: Temporary method to show whether the user is attentive
    private var duration: Long = 0
    private var startTimerMs = System.currentTimeMillis()
    private var endTimerMs = System.currentTimeMillis()
    private var perAttention = 0f
    private var inattentionThreshold = 0.3f
    private var totalFrame = 0
    private var inattentionFrame = 0

    fun setSlidingWindowSize(size: Int) {
        this.slidingWindowSize = size
    }

    fun getSlidingWindowSize(): Int {
        return this.slidingWindowSize
    }

    fun increaseTotalAttentionFrame() {
        this.totalAttentionFrame++
    }

    fun increaseTotalInattentionFrame() {
        this.totalInattentionFrame++
    }

    fun setPositiveThresholdX(angle: Float) {
        this.xPositiveThreshold = angle
    }

    fun setNegativeThresholdX(angle: Float) {
        this.xNegativeThreshold = angle
    }

    fun setPositiveThresholdY(angle: Float) {
        this.yPositiveThreshold = angle
    }

    fun setNegativeThresholdY(angle: Float) {
        this.yNegativeThreshold = angle
    }

    fun isAttention(): Boolean {
        return this.isAttention
    }

    fun startTimer() {
        this.startTimerMs = System.currentTimeMillis()
    }

    fun analyzeHeadPose(eulerX: Float, eulerY: Float) {
        if (eulerX > xPositiveThreshold || eulerX < xNegativeThreshold) {
            this.totalInattentionFrame++
        } else if (eulerY > yPositiveThreshold || eulerY < yNegativeThreshold) {
            this.totalInattentionFrame++
        } else {
            this.totalAttentionFrame++
        }
    }

    fun evaluateAttention() {
        if (totalAttentionFrame + totalInattentionFrame == slidingWindowSize) {
            isAttention = totalAttentionFrame >= totalInattentionFrame

            totalFrame++

            if (!isAttention) {
                inattentionFrame++
            }

            Log.v("HeadPoseAttention", "isAttention $isAttention")

            totalAttentionFrame = 0
            totalInattentionFrame = 0

            Log.v("FaceDetectionProcessor", "attention state: $isAttention")
        }
    }

    fun analyzeAttentiveness() {
        endTimerMs = System.currentTimeMillis()
        duration = endTimerMs - startTimerMs

        if (duration >= 30000) {
            perAttention = inattentionFrame.toFloat() / totalFrame.toFloat()

            if (perAttention > inattentionThreshold) {
                isDistracted = true

                mediaPlayer = MediaPlayer.create(context, R.raw.attention_alarm)
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.release()
                }
                mediaPlayer.start()
            }

            resetAnalyzer()
        }
    }

    fun resetAnalyzer() {
        totalFrame = 0
        inattentionFrame = 0
        startTimerMs = System.currentTimeMillis()
    }

    private fun resetProperties() {
        this.totalAttentionFrame = 0
        this.totalInattentionFrame = 0
    }
}