package com.example.learningassistance.detection.concentration

import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.R
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ConcentrationAnalysisViewModel(
    private val context: Context,
    private val dao: TaskDao,
    private val id: Long,
    private var faceProcessor: ConcentrationAnalysisFaceProcessor,
    private var objectProcessor: ConcentrationAnalysisObjectProcessor,
) : ViewModel() {

    // task
    var task = MutableLiveData<Task?>(null)
    var timeLeftMs = MutableLiveData<Long?>(null)

    // timer
    private var timer: CountDownTimer? = null
    private var concentrationAnalysisTimer: CountDownTimer? = null

    // state
    val isTimerShouldStart = MutableLiveData<Boolean>(false)
    val isAnalysisTimerShouldStart = MutableLiveData<Boolean>(false)
    val isPaused = MutableLiveData<Boolean?>(null)
    val isFinished = MutableLiveData<Boolean>(false)
    private var isAlertDialogShowing = false

    // Distraction time
    var fatigueTime: Long = 0
    var noFaceTime: Long = 0
    var lookAroundTime: Long = 0
    var electronicDevicesTime: Long = 0

    init {
        viewModelScope.launch {
            task.value = dao.get(id)
        }
    }

    fun setTimerTime() {
        timeLeftMs.value = (task.value!!.taskTimeLeftMs)
    }

    fun setDistractionValue() {
        fatigueTime = task.value!!.fatigueTimeMs
        noFaceTime = task.value!!.noFaceTimeMs
        lookAroundTime = task.value!!.lookAroundTimeMs
        electronicDevicesTime = task.value!!.electronicDevicesTimeMs
    }

    fun startTimer() {
        if (timer == null) {
            timer = object : CountDownTimer(timeLeftMs.value!!, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftMs.value = millisUntilFinished
                }

                override fun onFinish() {
                    // Update database
                    task.value!!.taskDone = true
                    updateTask()
                    // Change state
                    isFinished.value = true
                }

            }.start()
        }

        startAnalysisTimer()
    }

    fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
            stopAnalysisTimer()
            updateTask()
        }
    }

    fun startAnalysisTimer() {
        if (concentrationAnalysisTimer == null) {
            startConcentrationAnalysis()

            concentrationAnalysisTimer = object : CountDownTimer(15000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    Log.v(TAG, "Analyzing ... (${millisUntilFinished / 1000} left)")
                }

                override fun onFinish() {
                    if (noFaceDetection()) {
                        Log.v(TAG, "no face")
                    } else if (drowsinessDetection()) {
                        Log.v(TAG, "drowsiness")
                    } else if (headPoseDistraction()) {
                        Log.v(TAG, "look around")
                    } else {
                        electronicDevicesDetection()
                    }

                    stopConcentrationAnalysis()
                    resetDetector()
                    concentrationAnalysisTimer = null
                    startAnalysisTimer()
                }
            }.start()
        }
    }

    private fun stopAnalysisTimer() {
        if (concentrationAnalysisTimer != null) {
            concentrationAnalysisTimer!!.cancel()
            concentrationAnalysisTimer = null
            stopConcentrationAnalysis()
            resetDetector()
        }
    }

    fun noFaceDetection(): Boolean {
        faceProcessor.noFaceDetector.calculatePerNoFace()
        val perNoFace = faceProcessor.noFaceDetector.getPerNoFace()
        if (perNoFace > faceProcessor.noFaceDetector.getSevereNoFaceThreshold()) {
            this.noFaceTime += (30000 * perNoFace).toLong()

            if (!isAlertDialogShowing) {
                isAlertDialogShowing = true
                if (!faceProcessor.noFaceDetector.isAlarmPlaying()) {
                    faceProcessor.noFaceDetector.playAlarm()
                }
                if (!faceProcessor.noFaceDetector.isVibrating()) {
                    faceProcessor.noFaceDetector.startVibrating()
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.are_you_still_there)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(context.getString(R.string.no_face_alert_dialog_msg))
                    .setPositiveButton(R.string.close) { dialog, _ ->
                        if (faceProcessor.noFaceDetector.isAlarmPlaying()) {
                            faceProcessor.noFaceDetector.stopAlarm()
                        }
                        if (faceProcessor.noFaceDetector.isVibrating()) {
                            faceProcessor.noFaceDetector.stopVibrating()
                        }
                        isAlertDialogShowing = false
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            return true
        }
        return false
    }

    fun drowsinessDetection(): Boolean {
        faceProcessor.drowsinessDetector.calculatePerClose()
        val perClose = faceProcessor.drowsinessDetector.getPerClose()
        if (perClose > faceProcessor.drowsinessDetector.getFatigueThreshold()) {
            fatigueTime += (30000 * perClose).toLong()
            if (!isAlertDialogShowing) {
                isAlertDialogShowing = true
                if (!faceProcessor.drowsinessDetector.isAlarmPlaying()) {
                    faceProcessor.drowsinessDetector.playAlarm()
                }
                if (!faceProcessor.drowsinessDetector.isVibrating()) {
                    faceProcessor.drowsinessDetector.startVibrating()
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.warning)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(context.getString(R.string.drowsiness_alert_dialog_msg))
                    .setPositiveButton(R.string.close) { dialog, _ ->
                        if (faceProcessor.drowsinessDetector.isAlarmPlaying()) {
                            faceProcessor.drowsinessDetector.stopAlarm()
                        }
                        if (faceProcessor.drowsinessDetector.isVibrating()) {
                            faceProcessor.drowsinessDetector.stopVibrating()
                        }
                        isAlertDialogShowing = false
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            return true
        }
        else if(faceProcessor.drowsinessDetector.getTotalYawningPeriod()>5000) {
            fatigueTime += 30000
            if (!isAlertDialogShowing) {
                isAlertDialogShowing = true
                if (!faceProcessor.drowsinessDetector.isAlarmPlaying()) {
                    faceProcessor.drowsinessDetector.playAlarm()
                }
                if (!faceProcessor.drowsinessDetector.isVibrating()) {
                    faceProcessor.drowsinessDetector.startVibrating()
                }
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.warning)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(context.getString(R.string.drowsiness_alert_dialog_msg))
                    .setPositiveButton(R.string.close) { dialog, _ ->
                        if (faceProcessor.drowsinessDetector.isAlarmPlaying()) {
                            faceProcessor.drowsinessDetector.stopAlarm()
                        }
                        if (faceProcessor.drowsinessDetector.isVibrating()) {
                            faceProcessor.drowsinessDetector.stopVibrating()
                        }
                        isAlertDialogShowing = false
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
            }
            return true
        }
        return false
    }

    fun headPoseDistraction(): Boolean {
        if (faceProcessor.headPoseAttentionAnalyzer.calculatePerAttention() > faceProcessor.headPoseAttentionAnalyzer.inattentionThreshold) {
            lookAroundTime += (30000 * faceProcessor.headPoseAttentionAnalyzer.calculatePerAttention()).toLong()

            val mediaPlayer = MediaPlayer.create(context, R.raw.attention_alarm)
            mediaPlayer.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer.start()

            if (!isAlertDialogShowing) {
                isAlertDialogShowing = true
                val alertDialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.pay_attention)
                    .setIcon(R.drawable.ic_warning)
                    .setMessage(context.getString(R.string.head_pose_inattention_msg))
                    .show()

                object : CountDownTimer(5000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        Log.v(TAG, "$millisUntilFinished")
                    }

                    override fun onFinish() {
                        alertDialog.dismiss()
                        isAlertDialogShowing = false
                    }

                }.start()
            }

            return true
        }

        return false
    }

    fun electronicDevicesDetection(){
        electronicDevicesTime += (30000 * objectProcessor.calculatePerDetected()).toLong()
    }

    private fun resetDetector() {
        faceProcessor.noFaceDetector.resetDetector()
        faceProcessor.drowsinessDetector.resetDetector()
        faceProcessor.headPoseAttentionAnalyzer.resetAnalyzer()
        objectProcessor.resetDetector()
    }

    private fun startConcentrationAnalysis() {
        faceProcessor.isNoFaceDetectionShouldStart = true
        faceProcessor.isDrowsinessDetectionShouldStart = true
        faceProcessor.isHeadPoseAnalysisShouldStart = true
        objectProcessor.isObjectDetectionShouldStart = true
    }

    private fun stopConcentrationAnalysis() {
        faceProcessor.isNoFaceDetectionShouldStart = false
        faceProcessor.isDrowsinessDetectionShouldStart = false
        faceProcessor.isHeadPoseAnalysisShouldStart = false
        objectProcessor.isObjectDetectionShouldStart = false
    }

    private fun updateTask() {
        task.value!!.taskTimeLeftMs = timeLeftMs.value!!
        task.value!!.taskCompletePercentage = 100 - (100 * (timeLeftMs.value!!.toFloat() / (task.value!!.taskDurationMin * 60000).toFloat())).toInt()
        task.value!!.fatigueTimeMs = fatigueTime
        task.value!!.noFaceTimeMs = noFaceTime
        task.value!!.lookAroundTimeMs = lookAroundTime
        task.value!!.electronicDevicesTimeMs = electronicDevicesTime
        update()
    }

    private fun update() {
        viewModelScope.launch {
            dao.update(task.value!!)
        }
    }

    fun updateProcessor(newFace: ConcentrationAnalysisFaceProcessor, newObject: ConcentrationAnalysisObjectProcessor) {
        this.faceProcessor = newFace
        this.objectProcessor = newObject
    }

    companion object {
        private val TAG = "ConcentrationAnalysisViewModel"
    }
}