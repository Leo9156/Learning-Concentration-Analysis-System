package com.example.learningassistance.detection.concentration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.database.TaskDao

class ConcentrationAnalysisViewModelFactory(
    private val dao: TaskDao,
    private val id: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConcentrationAnalysisViewModel::class.java)) {
            return ConcentrationAnalysisViewModel(dao, id) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}