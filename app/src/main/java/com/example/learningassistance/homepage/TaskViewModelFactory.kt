package com.example.learningassistance.homepage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.learningassistance.database.TaskDao

class TaskViewModelFactory(private val dao: TaskDao) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}