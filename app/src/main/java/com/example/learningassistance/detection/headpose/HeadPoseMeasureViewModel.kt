package com.example.learningassistance.detection.headpose

import android.os.CountDownTimer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HeadPoseMeasureViewModel(
    private var headPoseFaceDetectionProcessor: HeadPoseFaceDetectionProcessor
    ) : ViewModel() {

    // The state of analysis
    var isAnalysisStarting = MutableLiveData<Boolean>(false)  // Indicate whether the analysis has started
    var startPrepareTimer = MutableLiveData<Boolean>(false)  // Indicate whether the prepare timer should start
    var isAnalysisFinished = MutableLiveData<Boolean>(false)
    private var canStartAnalysis = false  // Indicate whether the start analysis button can be clicked

    // Timer indicator
    var prepareTimerLeftCount = MutableLiveData<Int>(0)
    var analysisProgress = MutableLiveData<Int>(0)

    // Count down timer
    private var prepareTimer: CountDownTimer? = null
    private var analysisTimer: CountDownTimer? = null

    fun canStartAnalysis(): Boolean {
        this.canStartAnalysis = headPoseFaceDetectionProcessor.canAnalysisStart
        return this.canStartAnalysis
    }

    fun startPrepareTimer() {
        prepareTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                prepareTimerLeftCount.value = (millisUntilFinished / 1000).toInt() + 1
            }

            override fun onFinish() {
                startPrepareTimer.value = false
                prepareTimerLeftCount.value = 0
                startAnalysisTimer()
            }
        }.start()
    }

    fun startAnalysisTimer() {
        isAnalysisStarting.value = true
        headPoseFaceDetectionProcessor.isAnalysisStarting = true

        analysisTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                analysisProgress.value = analysisProgress.value!!.plus(20)
            }

            override fun onFinish() {
                // Change the state
                isAnalysisStarting.value = false
                headPoseFaceDetectionProcessor.isAnalysisStarting = false
                analysisProgress.value = 0

                // Get the basic head pose results
                headPoseFaceDetectionProcessor.calculateBasicHeadEulerAngle()

                // Reset the attributes in the head processor
                headPoseFaceDetectionProcessor.resetProperties()

                isAnalysisFinished.value = true
            }
        }.start()
    }

    fun restart() {
        if (analysisTimer != null) {
            analysisTimer!!.cancel()
        }
        analysisProgress.value = 0
        isAnalysisStarting.value = false
        headPoseFaceDetectionProcessor.isAnalysisStarting = false
        headPoseFaceDetectionProcessor.resetProperties()
        headPoseFaceDetectionProcessor.hasToRestart.value = false
    }

    fun reset() {
        isAnalysisStarting.value = false
        isAnalysisFinished.value = false
        canStartAnalysis = false
        startPrepareTimer.value = false
        if (analysisTimer != null) {
            analysisTimer!!.cancel()
            analysisTimer = null
        }
        if (prepareTimer != null) {
            prepareTimer!!.cancel()
            prepareTimer = null
        }
        analysisProgress.value = 0
        headPoseFaceDetectionProcessor.isAnalysisStarting = false
        headPoseFaceDetectionProcessor.resetProperties()
        headPoseFaceDetectionProcessor.hasToRestart.value = false
    }

    fun updateProcessor(newProcessor: HeadPoseFaceDetectionProcessor) {
        this.headPoseFaceDetectionProcessor = newProcessor
    }

    companion object {
        private val TAG = "HeadPoseMeasureViewModel"
    }
}