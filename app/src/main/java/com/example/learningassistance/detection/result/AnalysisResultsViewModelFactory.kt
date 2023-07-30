package com.example.learningassistance.detection.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.database.TaskDao

class AnalysisResultsViewModelFactory(
    private val dao: TaskDao,
    private val taskId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalysisResultsViewModel::class.java)) {
            return AnalysisResultsViewModel(dao, taskId) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}