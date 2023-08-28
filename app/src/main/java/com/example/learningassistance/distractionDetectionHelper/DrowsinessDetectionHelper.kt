package com.example.learningassistance.distractionDetectionHelper

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.learningassistance.R
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import kotlin.math.sqrt

class DrowsinessDetectionHelper(private val context: Context) {

    private var totalFrameNumber = 0
    private var isFatigueDialogShowing = false
    private var closedEyesFrameNumber = 0
    private var perClose = 0f
    private var detectionPeriodMs: Long = 30000
    private var awakeThreshold = 0.15
    private var fatigueThreshold = 0.3
    private var EAR = 1.0f
    private var leftEAR = 1.0f
    private var rightEAR = 1.0f
    private var rotX = 0f
    private var rotY = 0f
    private var rotZ = 0f

    // Timer
    private var startDrowsinessTimerMs = System.currentTimeMillis()
    private var endDrowsinessTimerMs = System.currentTimeMillis()
    private var duration: Long = 0

    // State
    var isDrowsinessAnalyzing = false

    // sound
    private var isAlarmPlaying = false
    private lateinit var mediaPlayer: MediaPlayer

    // vibration
    private var isVibrating = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    /*
    try to code yawning detection in DrowsinessDetection.kt
    by John
    How?
        be honest, I want to detection yawning by 3sec, not 30sec(because yawning time may not yawn to 30sec
        declare variable:
            detectionYawningPeriodMs : Long = 3000
            OpenmouthThreshold = 0.2
            yawningMOR
            MouthOpenRatio
            and some method
            caculate as EAR
    */
    //declare variables
    private var YawningThreshold = 0.2
    private var DetectionYawningPeriodMs : Long = 3000
    private var YawningDetectionDuration : Long = 0
    private var startYawningTimerMs = System.currentTimeMillis()
    private var endYawningTimerMs = System.currentTimeMillis()
    private var Yawning = false
    private var totalYawningPeriod: Long = 0
    //mouth open ratio
    private var yawningMOR = 0.0f
    private var OpenMouthCount = 0
    private var OpenMouthThreshold = 2
    /*
    and some method
    setYawningThreshold
    getYawningThreshold
    getMOR
     */
    fun getOpenMouthThreshold(): Int {
        return this.OpenMouthThreshold
    }
    fun setOpenMouthThreshold(new: Int) {
        this.OpenMouthThreshold = new
    }
    fun getTotalYawningPeriod(): Long {
        return this.totalYawningPeriod
    }
    fun resetTotalYawningPeriod(){
        this.totalYawningPeriod=0
    }
    fun addTotalYawningPeriod(){
        this.totalYawningPeriod+=this.YawningDetectionDuration
    }
    fun setYawningStatus(now: Boolean) {
        this.Yawning = now
    }
    fun getYawningStatus():Boolean {
        return this.Yawning
    }
    fun increaseOpenMouthNumber() {
        this.OpenMouthCount++
    }
    fun resetOpenMouthNumber() {
        this.OpenMouthCount=0
    }
    fun getOpenMouthNumber():Int {
        return this.OpenMouthCount
    }
    fun setYawningThreshold(threshold: Double) {
        this.YawningThreshold = threshold
    }
    fun getYawningThreshold(): Double {
        return this.YawningThreshold
    }
    fun calculateYawningDetectDuration() {
        YawningDetectionDuration = endYawningTimerMs - startYawningTimerMs
    }
    fun startYawningTimer() {
        this.startYawningTimerMs = System.currentTimeMillis()
    }
    fun endYawningTimer() {
        this.endYawningTimerMs = System.currentTimeMillis()
    }
    fun getYawningDetectionDuration(): Long {
        return this.YawningDetectionDuration
    }
    fun setDetectionYawningPeriodMs(period: Long) {
        this.DetectionYawningPeriodMs = period
    }
    fun getDetectionYawningPeriodMs(): Long {
        return this.DetectionYawningPeriodMs
    }
    // I'm not sure to use this.yawningMOR or yawningMOR
    fun calculateMOR(allPoints: List<FaceMeshPoint>) {
        var a = calculateDistance(allPoints[78].position, allPoints[308].position)
        var b = calculateDistance(allPoints[38].position, allPoints[86].position)
        var c = calculateDistance(allPoints[268].position, allPoints[316].position)
        this.yawningMOR = (b+c)/(2*a)
    }
    fun getMOR(): Float {
        return this.yawningMOR
    }
    fun resetMOR() {
        this.yawningMOR = 0.0f
    }
    fun resetYawningDetect() {
        startYawningTimer()
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
        resetOpenMouthNumber()
        resetTotalYawningPeriod()
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
        this.EAR = 2.0f
        this.leftEAR = 2.0f
        this.rightEAR = 2.0f
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

        return (b + c) / (2 * a)
    }

    private fun calculateLeftEAR(allPoints: List<FaceMeshPoint>): Float {
        var a = calculateDistance(allPoints[362].position, allPoints[263].position)
        var b = calculateDistance(allPoints[385].position, allPoints[380].position)
        var c = calculateDistance(allPoints[387].position, allPoints[373].position)

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