package com.example.learningassistance.facedetection

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout

object BasicHeadPoseMeasurement {
    private var isBasicHeadPoseStarting = false  // Whether the user clicks the start button in the snack bar
    private var isBasicHeadPoseDetecting = true  // Whether the system is in the basic head pose detection phase
    private var hasToRestart = false  // When no faces detected, basic head pose measurement has to restart
    private var hasToRetry = false  // Whether the user clicks the retry button and wants to measure the basic head pose again
    private var isTimerCounting = false
    private var totalFrameNumber = 0
    private var measureDurationMs: Int = 5000
    private var basicHeadEulerX = 0f
    private var basicHeadEulerY = 0f
    private var basicHeadEulerZ = 0f
    private var sumOfHeadEulerX = 0f
    private var sumOfHeadEulerY = 0f
    private var sumOfHeadEulerZ = 0f
    private var startMeasureBasicHeadPoseTimerMs = System.currentTimeMillis()
    private var endMeasureBasicHeadPoseTimerMs = System.currentTimeMillis()
    private var duration: Long = 0

    fun setIsBasicHeadPoseDetecting(flag: Boolean) {
        this.isBasicHeadPoseDetecting = flag
    }

    fun isBasicHeadPoseDetecting(): Boolean {
        return this.isBasicHeadPoseDetecting
    }

    fun setIsBasicHeadPoseMeasurementStarting(flag: Boolean) {
        this.isBasicHeadPoseStarting = flag
    }

    fun isBasicHeadPoseMeasurementStarting(): Boolean {
        return this.isBasicHeadPoseStarting
    }

    fun setHasToRestart(flag: Boolean) {
        this.hasToRestart = flag
    }

    fun hasToRestart(): Boolean {
        return this. hasToRestart
    }

    fun isTimerCounting(): Boolean {
        return this.isTimerCounting
    }

    fun setIsTimerCounting(flag: Boolean) {
        this.isTimerCounting = flag
    }

    fun setMeasureDurationMs(t: Int) {
        this.measureDurationMs = t
    }

    fun getMeasureDurationMs(): Int {
        return this.measureDurationMs
    }

    /*fun setBasicHeadEulerX(angle: Float) {
        this.basicHeadEulerX = angle
    }

    fun setBasicHeadEulerY(angle: Float) {
        this.basicHeadEulerY = angle
    }

    fun setBasicHeadEulerZ(angle: Float) {
        this.basicHeadEulerZ = angle
    }*/

    /*fun setCurrentHeadEulerX(angle: Float) {
        this.currentHeadEulerX = angle
    }

    fun setCurrentHeadEulerY(angle: Float) {
        this.currentHeadEulerX = angle
    }

    fun setCurrentHeadEulerZ(angle: Float) {
        this.currentHeadEulerX = angle
    }*/

    fun setSumOfHeadEulerX(angle: Float) {
        this.sumOfHeadEulerX = angle
    }

    fun setSumOfHeadEulerY(angle: Float) {
        this.sumOfHeadEulerY = angle
    }

    fun setSumOfHeadEulerZ(angle: Float) {
        this.sumOfHeadEulerZ = angle
    }

    fun setStartTimer(t: Long) {
        this.startMeasureBasicHeadPoseTimerMs = t
    }

    fun setEndTimer(t: Long) {
        this.endMeasureBasicHeadPoseTimerMs = t
    }

    fun setTotalFrame(total: Int) {
        this.totalFrameNumber = total
    }

    fun getBasicHeadEulerX(): Float {
        return this.basicHeadEulerX
    }

    fun getBasicHeadEulerY(): Float {
        return this.basicHeadEulerY
    }

    fun getBasicHeadEulerZ(): Float {
        return this.basicHeadEulerZ
    }

    fun getSumOfHeadEulerX(): Float {
        return this.sumOfHeadEulerX
    }

    fun getSumOfHeadEulerY(): Float {
        return this.sumOfHeadEulerY
    }

    fun getSumOfHeadEulerZ(): Float {
        return this.sumOfHeadEulerZ
    }

    fun getDuration(): Long {
        return this.duration
    }

    fun addSumOfEulerX(rotX: Float) {
        this.sumOfHeadEulerX += rotX
    }

    fun addSumOfEulerY(rotY: Float) {
        this.sumOfHeadEulerY += rotY
    }

    fun addSumOfEulerZ(rotZ: Float) {
        this.sumOfHeadEulerZ += rotZ
    }

    fun increaseTotalFrameNumber() {
        totalFrameNumber++
    }

    fun getTotalFrame(): Int {
        return totalFrameNumber
    }

    fun calculateDuration() {
        this.duration = this.endMeasureBasicHeadPoseTimerMs - this.startMeasureBasicHeadPoseTimerMs
    }

    fun calculateBasicHeadPose() {
        basicHeadEulerX = String.format("%.2f", sumOfHeadEulerX / totalFrameNumber.toFloat()).toFloat()
        basicHeadEulerY = String.format("%.2f", sumOfHeadEulerY / totalFrameNumber.toFloat()).toFloat()
        basicHeadEulerZ = String.format("%.2f", sumOfHeadEulerZ / totalFrameNumber.toFloat()).toFloat()
    }

    fun resetProperties() {
        isBasicHeadPoseStarting = false
        isBasicHeadPoseDetecting = true
        hasToRestart = false
        hasToRetry = false
        totalFrameNumber = 0
        basicHeadEulerX = 0f
        basicHeadEulerY = 0f
        basicHeadEulerZ = 0f
        sumOfHeadEulerX = 0f
        sumOfHeadEulerY = 0f
        sumOfHeadEulerZ = 0f
        startMeasureBasicHeadPoseTimerMs = System.currentTimeMillis()
        endMeasureBasicHeadPoseTimerMs = System.currentTimeMillis()
        duration = 0
    }
}