package com.example.learningassistance.historypage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import kotlinx.coroutines.launch
class HistoryViewModel(private val dao: TaskDao) : ViewModel() {
    var histTasks = MutableLiveData<List<Task>?>(null)

    fun setHistTasksDate(d: String) {
        viewModelScope.launch {
            histTasks.value = dao.getAllDoneFromDate(d)
        }
    }
}