package com.example.learningassistance.detection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HeadPoseMeasureViewModelFactory(
    private val headPoseFaceDetectionProcessor: HeadPoseFaceDetectionProcessor
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HeadPoseMeasureViewModel::class.java)) {
            return HeadPoseMeasureViewModel(headPoseFaceDetectionProcessor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}