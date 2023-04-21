package com.example.learningassistance.facedetection

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.learningassistance.R
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class DrowsinessDetection(private val context: Context) {

    private var totalFrameNumber = 0
    private var isFatigueDialogShowing = false
    private var closedEyesFrameNumber = 0
    private var startDrowsinessTimerMs = System.currentTimeMillis()
    private var endDrowsinessTimerMs = System.currentTimeMillis()
    private var duration: Long = 0
    private var perClose = 0f
    private var closedEyeThreshold = 0.2
    private var detectionPeriodMs: Long = 30000
    private var awakeThreshold = 0.15
    private var fatigueThreshold = 0.3
    private var EAR = 1.0f
    private var leftEAR = 1.0f
    private var rightEAR = 1.0f
    private var rotX = 0f
    private var rotY = 0f
    private var rotZ = 0f

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

    fun resetDetector() {
        resetTotalFrameNumber()
        resetClosedEyesFrameNumber()
        startDrowsinessTimer()
    }

    fun getTotalFrame(): Int {
        return this.totalFrameNumber
    }

    fun getCloseFrame(): Int {
        return this.closedEyesFrameNumber
    }

    fun getEAR(): Float {
        return this.EAR
    }

    fun getRightEAR(): Float {
        return this.rightEAR
    }

    fun getLeftEAR(): Float {
        return this.leftEAR
    }

    fun resetEAR() {
        this.EAR = 1.0f
        this.leftEAR = 1.0f
        this.rightEAR = 1.0f
    }

    fun calculateEAR(allPoints: List<FaceMeshPoint>) {
        leftEAR = calculateLeftEAR(allPoints)
        rightEAR = calculateRightEAR(allPoints)

        EAR = (leftEAR + rightEAR) / 2
    }

    private fun calculateRightEAR(allPoints: List<FaceMeshPoint>): Float {
        var a = calculateDistance(allPoints[33].position, allPoints[133].position)
        var b = calculateDistance(allPoints[160].position, allPoints[144].position)
        var c = calculateDistance(allPoints[158].position, allPoints[153].position)

        //Log.v("FaceDetectionProcessor", "origin right A: $a")
        //Log.v("FaceDetectionProcessor", "origin right B: $b")
        //Log.v("FaceDetectionProcessor", "origin right C: $c")

//        a *= 0.5f * abs(cos(rotY))
//        b *= 0.5f * sqrt(1 - (sin(rotX) * sin(rotX)) * (cos(rotY) * cos(rotY)))
//        c *= 0.5f * sqrt(1 - (sin(rotX) * sin(rotX)) * (cos(rotY) * cos(rotY)))

        //Log.v("FaceDetectionProcessor", "formula1: ${abs(cos(rotY))}")
        //Log.v("FaceDetectionProcessor", "formula2: ${sqrt(1 - (sin(rotX) * sin(rotX)) * (cos(rotY) * cos(rotY)))}")

        //Log.v("FaceDetectionProcessor", "right A: $a")
        //Log.v("FaceDetectionProcessor", "right B: $b")
        //Log.v("FaceDetectionProcessor", "right C: $c")

        return (b + c) / (2 * a)
    }

    private fun calculateLeftEAR(allPoints: List<FaceMeshPoint>): Float {
        var a = calculateDistance(allPoints[362].position, allPoints[263].position)
        var b = calculateDistance(allPoints[385].position, allPoints[380].position)
        var c = calculateDistance(allPoints[387].position, allPoints[373].position)

//        Log.v("FaceDetectionProcessor", "origin left A: $a")
//        Log.v("FaceDetectionProcessor", "origin left B: $b")
//        Log.v("FaceDetectionProcessor", "origin left C: $c")


//        a *= abs(cos(rotX))
//        b *= sqrt(1 - (sin(rotY) * sin(rotY)) * (cos(rotX) * cos(rotX)))
//        c *= sqrt(1 - (sin(rotY) * sin(rotY)) * (cos(rotX) * cos(rotX)))

//        Log.v("FaceDetectionProcessor", "rotx: ${rotX}")
//        Log.v("FaceDetectionProcessor", "roty: ${rotY}")
//
//        Log.v("FaceDetectionProcessor", "formula1: ${abs(cos(rotY))}")
//        Log.v("FaceDetectionProcessor", "formula2: ${sqrt(1 - (sin(rotX) * sin(rotX)) * (cos(rotY) * cos(rotY)))}")
//
//        Log.v("FaceDetectionProcessor", "left A: $a")
//        Log.v("FaceDetectionProcessor", "left B: $b")
//        Log.v("FaceDetectionProcessor", "left C: $c")

        return (b + c) / (2 * a)
    }

    private fun calculateDistance(point1: PointF3D, point2: PointF3D): Float {
        val xDiff = point1.x - point2.x
        val yDiff = point1.y - point2.y

        return sqrt((xDiff * xDiff) + (yDiff * yDiff))
    }

    fun setRotX(angle: Float) {
        this.rotX = Math.toRadians(angle.toDouble()).toFloat()
    }

    fun setRotY(angle: Float) {
        this.rotY = Math.toRadians(angle.toDouble()).toFloat()
    }

    fun setRotZ(angle: Float) {
        this.rotZ = Math.toRadians(angle.toDouble()).toFloat()
    }
}