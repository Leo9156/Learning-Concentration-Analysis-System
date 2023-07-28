package com.example.learningassistance.detection.concentration

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import com.github.mikephil.charting.utils.Utils.init
import kotlinx.coroutines.launch

class ConcentrationAnalysisViewModel(
    private val dao: TaskDao,
    private val id: Long
) : ViewModel() {

    // task
    var task = MutableLiveData<Task?>(null)
    var timeLeftMs = MutableLiveData<Long?>(null)

    // timer
    private var timer: CountDownTimer? = null

    // state
    val isTimerShouldStart = MutableLiveData<Boolean>(false)
    val isPaused = MutableLiveData<Boolean?>(null)
    val isFinished = MutableLiveData<Boolean>(false)

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
                    update()
                    // Change state
                    isFinished.value = true
                }

            }.start()
        }
    }

    fun stopTimer() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
            updateTask()
        }
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

    companion object {
        private val TAG = "ConcentrationAnalysisViewModel"
    }
}