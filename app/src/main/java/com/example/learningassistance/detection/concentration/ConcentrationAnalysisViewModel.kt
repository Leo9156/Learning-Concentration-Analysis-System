package com.example.learningassistance.detection.concentration

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
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
    val isPaused = MutableLiveData<Boolean>(false)

    init {
        viewModelScope.launch {
            task.value = dao.get(id)
        }
    }

    fun setTimerTime() {
        timeLeftMs.value = (task.value!!.taskTimeLeftMs)
    }

    fun startTimer() {
        if (timer == null) {
            timer = object : CountDownTimer(timeLeftMs.value!!, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeftMs.value = millisUntilFinished
                }

                override fun onFinish() {
                    Log.v(TAG, "completed")
                    // TODO: Add Something
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
        viewModelScope.launch {
            dao.update(task.value!!)

        }
    }
    companion object {
        private val TAG = "ConcentrationAnalysisViewModel"
    }
}