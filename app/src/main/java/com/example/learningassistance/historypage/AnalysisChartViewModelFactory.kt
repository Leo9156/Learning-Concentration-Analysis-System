package com.example.learningassistance.historypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.database.TaskDao
import com.example.learningassistance.detection.result.AnalysisResultsViewModel

class AnalysisChartViewModelFactory(
    private val dao: TaskDao,
    private val taskId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisChartViewModel::class.java)) {
            return AnalysisChartViewModel(dao, taskId) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}