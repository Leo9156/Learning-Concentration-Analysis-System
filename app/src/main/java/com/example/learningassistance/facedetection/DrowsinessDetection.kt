package com.example.learningassistance.facedetection

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.learningassistance.R

class DrowsinessDetection(private val context: Context) {

    private var totalFrameNumber = 0
    private var isFatigueDialogShowing = false
    private var closedEyesFrameNumber = 0
    private var startDrowsinessTimerMs = System.currentTimeMillis()
    private var endDrowsinessTimerMs = System.currentTimeMillis()
    private var duration: Long = 0
    private var perClose = 0f
    private var closedEyeThreshold = 0.3
    private var detectionPeriodMs: Long = 30000
    private var awakeThreshold = 0.15
    private var fatigueThreshold = 0.3

    private var isAlarmPlaying = false
    private lateinit var mediaPlayer: MediaPlayer

    private var isVibrating = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun setClosedEyeThreshold(threshold: Double) {
        this.closedEyeThreshold = threshold
    }

    fun getClosedEyeThreshold(): Double {
        return this.closedEyeThreshold
    }

    fun setDetectionPeriodMs(period: Long) {
        this.detectionPeriodMs = period
    }

    fun getDetectionPeriodMs(): Long {
        return this.detectionPeriodMs
    }

    fun setAwakeThreshold(threshold: Double) {
        this.awakeThreshold = threshold
    }

    fun getAwakeThreshold(): Double {
        return this.awakeThreshold
    }

    fun setFatigueThreshold(threshold: Double) {
        this.fatigueThreshold = threshold
    }

    fun getFatigueThreshold(): Double {
        return this.fatigueThreshold
    }

    fun increaseTotalFrameNumber() {
        this.totalFrameNumber++
    }

    fun resetTotalFrameNumber() {
        this.totalFrameNumber = 0
    }

    fun increaseClosedEyesFrameNumber() {
        this.closedEyesFrameNumber++
    }

    fun startDrowsinessTimer() {
        this.startDrowsinessTimerMs = System.currentTimeMillis()
    }

    fun endDrowinessTimer() {
        this.endDrowsinessTimerMs = System.currentTimeMillis()
    }

    fun calculateDuration() {
        duration = endDrowsinessTimerMs - startDrowsinessTimerMs
    }

    fun getDuration(): Long {
        return this.duration
    }

    fun calculatePerClose() {
        this.perClose = closedEyesFrameNumber.toFloat() / totalFrameNumber.toFloat()
    }

    fun getPerClose(): Float {
        return this.perClose
    }

    fun resetClosedEyesFrameNumber() {
        this.closedEyesFrameNumber = 0
    }

    fun setIsFatigueDialogShowing(flag: Boolean) {
        this.isFatigueDialogShowing = flag
    }

    fun setIsAlarmPlaying(flag: Boolean) {
        this.isAlarmPlaying = flag
    }

    fun playAlarm() {
        mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
        isAlarmPlaying = true
    }

    fun stopAlarm() {
        mediaPlayer.stop()
        mediaPlayer.release()
        isAlarmPlaying = false
    }

    fun setIsVibrating(flag: Boolean) {
        this.isVibrating = flag
    }

    fun startVibrating() {
        if (vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 1000)
            isVibrating = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                    pattern,
                    0
                ))
            } else {
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    fun stopVibrating() {
        vibrator.cancel()
        isVibrating = false
    }

    fun isFatigueDialogShowing(): Boolean {
        return this.isFatigueDialogShowing
    }

    fun isAlarmPlaying(): Boolean {
        return this.isAlarmPlaying
    }

    fun isVibrating(): Boolean {
        return this.isVibrating
    }

}