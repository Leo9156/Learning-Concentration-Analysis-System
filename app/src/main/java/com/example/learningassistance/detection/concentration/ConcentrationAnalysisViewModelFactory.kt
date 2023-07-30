package com.example.learningassistance.detection.concentration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.database.TaskDao

class ConcentrationAnalysisViewModelFactory(
    private val context: Context,
    private val dao: TaskDao,
    private val id: Long,
    private var faceProcessor: ConcentrationAnalysisFaceProcessor,
    private var objectProcessor: ConcentrationAnalysisObjectProcessor,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConcentrationAnalysisViewModel::class.java)) {
            return ConcentrationAnalysisViewModel(context, dao, id, faceProcessor, objectProcessor) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}