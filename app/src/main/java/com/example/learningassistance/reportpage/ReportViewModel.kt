package com.example.learningassistance.reportpage

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class ReportViewModel(private val dao: TaskDao) : ViewModel() {
    var avgAttentionScore = 0f
    var avgLearningTimeMin = 0f
    var avgAttentionScoreMon = 0f
    var monTaskCounter = 0
    var avgAttentionScoreTue = 0f
    var tueTaskCounter = 0
    var avgAttentionScoreWed = 0f
    var wedTaskCounter = 0
    var avgAttentionScoreThu = 0f
    var thuTaskCounter = 0
    var avgAttentionScoreFri = 0f
    var friTaskCounter = 0
    var avgAttentionScoreSat = 0f
    var satTaskCounter = 0
    var avgAttentionScoreSun = 0f
    var sunTaskCounter = 0
    var tasks = MutableLiveData<List<Task>?>(null)

    fun setTasks() {
        viewModelScope.launch {
            tasks.value = dao.getAllDoneFromWeek(LocalDate.now().with(DayOfWeek.MONDAY).toString(), LocalDate.now().toString())
        }
    }

    fun calculateAvg() {
        tasks.value!!.forEach {
            avgAttentionScore += calculateAttentionPercent(it)
            avgLearningTimeMin += (it.taskDurationMin.toFloat())
        }

        if (avgAttentionScore != 0f) {
            avgAttentionScore = String.format("%.1f", avgAttentionScore / tasks.value!!.size.toFloat()).toFloat()
        }
        if (avgLearningTimeMin != 0f) {
            avgLearningTimeMin = String.format("%.1f", avgLearningTimeMin / tasks.value!!.size.toFloat()).toFloat()
        }

        calculateAvgPerDay()
    }

    private fun calculateAvgPerDay() {
        val startDay = LocalDate.now().with(DayOfWeek.MONDAY)

        tasks.value!!.forEach {
            when {
                // Monday
                it.taskDate.equals(startDay.toString()) -> {
                    avgAttentionScoreMon += calculateAttentionPercent(it)
                    monTaskCounter++
                }
                // Tuesday
                it.taskDate.equals(startDay.plusDays(1L).toString()) -> {
                    avgAttentionScoreTue += calculateAttentionPercent(it)
                    tueTaskCounter++
                }
                // Wednesday
                it.taskDate.equals(startDay.plusDays(2L).toString()) -> {
                    avgAttentionScoreWed += calculateAttentionPercent(it)
                    wedTaskCounter++
                }
                // Thursday
                it.taskDate.equals(startDay.plusDays(3L).toString()) -> {
                    avgAttentionScoreThu += calculateAttentionPercent(it)
                    thuTaskCounter++
                }
                // Friday
                it.taskDate.equals(startDay.plusDays(4L).toString()) -> {
                    avgAttentionScoreFri += calculateAttentionPercent(it)
                    friTaskCounter++
                }
                // Saturday
                it.taskDate.equals(startDay.plusDays(5L).toString()) -> {
                    avgAttentionScoreSat += calculateAttentionPercent(it)
                    satTaskCounter++
                }
                // Sunday
                it.taskDate.equals(startDay.plusDays(6L).toString()) -> {
                    avgAttentionScoreSun += calculateAttentionPercent(it)
                    sunTaskCounter++
                }
            }
        }

        if (avgAttentionScoreMon != 0f) {
            avgAttentionScoreMon = String.format("%.1f", avgAttentionScoreMon / monTaskCounter).toFloat()
        }
        if (avgAttentionScoreTue != 0f) {
            avgAttentionScoreTue = String.format("%.1f", avgAttentionScoreTue / tueTaskCounter).toFloat()
        }
        if (avgAttentionScoreWed != 0f) {
            avgAttentionScoreWed = String.format("%.1f", avgAttentionScoreWed / wedTaskCounter).toFloat()
        }
        if (avgAttentionScoreThu != 0f) {
            avgAttentionScoreThu = String.format("%.1f", avgAttentionScoreThu / thuTaskCounter).toFloat()
        }
        if (avgAttentionScoreFri != 0f) {
            avgAttentionScoreFri = String.format("%.1f", avgAttentionScoreFri / friTaskCounter).toFloat()
        }
        if (avgAttentionScoreSat != 0f) {
            avgAttentionScoreSat = String.format("%.1f", avgAttentionScoreSat / satTaskCounter).toFloat()
        }
        if (avgAttentionScoreSun != 0f) {
            avgAttentionScoreSun = String.format("%.1f", avgAttentionScoreSun / sunTaskCounter).toFloat()
        }
    }

    fun reset() {
        tasks.value = null
        avgAttentionScore = 0f
        avgLearningTimeMin = 0f
        avgAttentionScoreMon = 0f
        monTaskCounter = 0
        avgAttentionScoreTue = 0f
        tueTaskCounter = 0
        avgAttentionScoreWed = 0f
        wedTaskCounter = 0
        avgAttentionScoreThu = 0f
        thuTaskCounter = 0
        avgAttentionScoreFri = 0f
        friTaskCounter = 0
        avgAttentionScoreSat = 0f
        satTaskCounter = 0
        avgAttentionScoreSun = 0f
        sunTaskCounter = 0
    }

    private fun calculateAttentionPercent(task: Task): Float {
        val noFacePercent = String.format("%.2f", 100f * (task.noFaceTimeMs.toFloat() / (task.taskDurationMin * 60000).toFloat())).toFloat()
        val drowsinessPercent = String.format("%.2f", 100f * (task.fatigueTimeMs.toFloat() / (task.taskDurationMin * 60000).toFloat())).toFloat()
        val lookAroundPercent = String.format("%.2f", 100f * (task.lookAroundTimeMs.toFloat() / (task.taskDurationMin * 60000).toFloat())).toFloat()
        val electDevPercent = String.format("%.2f", 100f * (task.electronicDevicesTimeMs.toFloat() / (task.taskDurationMin * 60000).toFloat())).toFloat()
        return String.format("%.2f", 100f - (noFacePercent + drowsinessPercent + lookAroundPercent + electDevPercent)).toFloat()
    }

    companion object {
        private val TAG = "ReportViewModel"
    }
}