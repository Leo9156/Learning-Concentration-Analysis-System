package com.example.learningassistance.historypage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryViewModel(private val dao: TaskDao) : ViewModel() {
    //var date = LocalDate.now().toString()
    var histTasks = MutableLiveData<List<Task>?>(null)

    /*init {
        viewModelScope.launch {
            histTasks.value = dao.getAllDoneFromDate(date)
        }
    }*/

    fun setHistTasksDate(d: String) {
        viewModelScope.launch {
            histTasks.value = dao.getAllDoneFromDate(d)
        }
    }
}