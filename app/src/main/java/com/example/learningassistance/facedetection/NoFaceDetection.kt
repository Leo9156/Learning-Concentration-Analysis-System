package com.example.learningassistance.facedetection

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.learningassistance.R

class NoFaceDetection(private val context: Context) {

    private var totalFrameNumber = 0
    private var isNoFaceDialogShowing = false
    private var noFaceFrameNumber = 0
    private var startNoFaceTimerMs = System.currentTimeMillis()
    private var endNoFaceTimerMs = System.currentTimeMillis()
    private var duration: Long = 0
    private var perNoFace = 0f
    private var detectionPeriodMs: Long = 30000
    private var slightlyNoFaceThreshold = 0.15
    private var severeNoFaceThreshold = 0.3
    private var isNoFaceOccur = false
    private var isNoFace = false

    private var isAlarmPlaying = false
    private lateinit var mediaPlayer: MediaPlayer

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

    fun setIsNoFaceOccur(flag: Boolean) {
        this.isNoFaceOccur = flag
    }

    fun getIsNoFaceOccur(): Boolean {
        return this.isNoFaceOccur
    }

    fun setIsNoFace(flag: Boolean) {
        this.isNoFace = flag
    }

    fun isNoFace(): Boolean {
        return this.isNoFace
    }
}