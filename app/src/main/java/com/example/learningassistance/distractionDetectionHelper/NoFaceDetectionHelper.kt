package com.example.learningassistance.distractionDetectionHelper

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.learningassistance.R

class NoFaceDetectionHelper(private val context: Context) {

    private var totalFrameNumber = 0
    private var isNoFaceDialogShowing = false
    private var noFaceFrameNumber = 0
    private var duration: Long = 0
    private var perNoFace = 0f
    private var detectionPeriodMs: Long = 30000
    private var slightlyNoFaceThreshold = 0.15
    private var severeNoFaceThreshold = 0.5

    // Timer
    private var startNoFaceTimerMs = System.currentTimeMillis()
    private var endNoFaceTimerMs = System.currentTimeMillis()

    // State
    var isNoFaceDetecting = false

    // Sound
    private var isAlarmPlaying = false
    private lateinit var mediaPlayer: MediaPlayer

    // Vibration
    private var isVibrating = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator


    fun setDetectionPeriodMs(period: Long) {
        this.detectionPeriodMs = period
    }

    fun getDetectionPeriodMs(): Long {
        return this.detectionPeriodMs
    }

    fun setSlightlyNoFaceThreshold(threshold: Double) {
        this.slightlyNoFaceThreshold = threshold
    }

    fun getSlightlyNoFaceThreshold(): Double {
        return this.slightlyNoFaceThreshold
    }

    fun setSevereNoFaceThreshold(threshold: Double) {
        this.severeNoFaceThreshold = threshold
    }

    fun getSevereNoFaceThreshold(): Double {
        return this.severeNoFaceThreshold
    }

    fun increaseTotalFrameNumber() {
        this.totalFrameNumber++
    }

    fun resetTotalFrameNumber() {
        this.totalFrameNumber = 0
    }

    fun increaseNoFaceFrameNumber() {
        this.noFaceFrameNumber++
    }

    fun startNoFaceTimer() {
        this.startNoFaceTimerMs = System.currentTimeMillis()
    }

    fun endNoFaceTimer() {
        this.endNoFaceTimerMs = System.currentTimeMillis()
    }

    fun calculateDuration() {
        duration = endNoFaceTimerMs - startNoFaceTimerMs
    }

    fun getDuration(): Long {
        return this.duration
    }

    fun calculatePerNoFace() {
        this.perNoFace = noFaceFrameNumber.toFloat() / totalFrameNumber.toFloat()
    }

    fun getPerNoFace(): Float {
        return this.perNoFace
    }

    fun isNoFaceDialogShowing(): Boolean {
        return this.isNoFaceDialogShowing
    }

    fun resetNoFaceFrameNumber() {
        this.noFaceFrameNumber = 0
    }

    fun setIsNoFaceDialogShowing(flag: Boolean) {
        this.isNoFaceDialogShowing = flag
    }

    fun setIsAlarmPlaying(flag: Boolean) {
        this.isAlarmPlaying = flag
    }

    fun playAlarm() {
        //val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)
        //mediaPlayer = MediaPlayer.create(context, alarmUri)
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


    fun isAlarmPlaying(): Boolean {
        return this.isAlarmPlaying
    }

    fun isVibrating(): Boolean {
        return this.isVibrating
    }

    fun resetDetector() {
        resetTotalFrameNumber()
        resetNoFaceFrameNumber()
        startNoFaceTimer()
    }
}