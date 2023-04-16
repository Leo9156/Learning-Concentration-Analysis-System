package com.example.learningassistance.facedetection

import android.util.Log
import com.example.learningassistance.facedetection.BasicHeadPoseMeasurement.resetProperties

class HeadPoseAttentionAnalysis {
    private var slidingWindowSize = 30
    private var totalAttentionFrame = 0
    private var totalInattentionFrame = 0
    private var xPositiveThreshold = 20.0f
    private var xNegativeThreshold = -20.0f
    private var yPositiveThreshold = 20.0f
    private var yNegativeThreshold = -20.0f
    //private var zPositiveThreshold = 0f
    //private var zNegativeThreshold = 0f
    private var isAttention = true

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

    fun setPositiveThreasholdY(angle: Float) {
        this.yPositiveThreshold = angle
    }

    fun setNegativeThresholdY(angle: Float) {
        this.yNegativeThreshold = angle
    }

    fun isAttention(): Boolean {
        return this.isAttention
    }

    /*fun setThreasholdZ(angle: Float) {
        this.zPositiveThreshold = angle
    }*/

    fun analyzeAttention(eulerX: Float, eulerY: Float, eulerZ: Float) {
        if (eulerX > xPositiveThreshold || eulerX < xNegativeThreshold) {
            this.totalInattentionFrame++
        } else if (eulerY > yPositiveThreshold || eulerY < yNegativeThreshold) {
            this.totalInattentionFrame++
        } else {
            this.totalAttentionFrame++
        }
    }

    fun assesAttention() {
        if (totalAttentionFrame + totalInattentionFrame == slidingWindowSize) {
            isAttention = totalAttentionFrame >= totalInattentionFrame
            resetProperties()

            Log.v("FaceDetectionProcessor", "attention state: $isAttention")
        }
    }

    private fun resetProperties() {
        this.totalAttentionFrame = 0
        this.totalInattentionFrame = 0
    }
}