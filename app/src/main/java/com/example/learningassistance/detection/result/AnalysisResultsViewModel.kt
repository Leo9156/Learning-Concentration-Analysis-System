package com.example.learningassistance.detection.result

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import kotlinx.coroutines.launch

class AnalysisResultsViewModel(private val dao: TaskDao, private val id: Long) : ViewModel() {
    var task = MutableLiveData<Task?>(null)
    var noFacePercent = 0f
    var drowsinessPercent = 0f
    var lookAroundPercent = 0f
    var electDevPercent = 0f
    var attentionPercent = 0f

    init {
        viewModelScope.launch {
            task.value = dao.get(id)
        }
    }

    fun calculatePercent() {
        noFacePercent = String.format("%.2f", 100f * (task.value!!.noFaceTimeMs.toFloat() / (task.value!!.taskDurationMin * 60000).toFloat())).toFloat()
        drowsinessPercent = String.format("%.2f", 100f * (task.value!!.fatigueTimeMs.toFloat() / (task.value!!.taskDurationMin * 60000).toFloat())).toFloat()
        lookAroundPercent = String.format("%.2f", 100f * (task.value!!.lookAroundTimeMs.toFloat() / (task.value!!.taskDurationMin * 60000).toFloat())).toFloat()
        electDevPercent = String.format("%.2f", 100f * (task.value!!.electronicDevicesTimeMs.toFloat() / (task.value!!.taskDurationMin * 60000).toFloat())).toFloat()
        attentionPercent = String.format("%.2f", 100f - (noFacePercent + drowsinessPercent + lookAroundPercent + electDevPercent)).toFloat()
        if (attentionPercent < 0) {
            attentionPercent = 0f
        }
    }
}