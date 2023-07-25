package com.example.learningassistance.homepage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learningassistance.database.Task
import com.example.learningassistance.database.TaskDao
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(val dao: TaskDao) : ViewModel() {
    // Data in the task table
    var newTaskName = ""
    var newTaskDescription = ""
    var newTaskDuration = 0
    private val newTaskCurrentDate = LocalDate.now().toString()
    var newTaskCompletePercentage = 0
    var newTaskDone = false

    // Total number of tasks in the database
    var totalTasksCount = MutableLiveData<Int>(-1)

    // All the tasks in the task table
    val tasks = dao.getAll()

    fun addTask() {
        viewModelScope.launch {
            // Initialize input data
            val task = Task()
            task.taskName = newTaskName
            task.taskDescription = newTaskDescription
            task.taskDurationMin = newTaskDuration
            //task.taskTimeLeft = newTaskDuration
            task.taskDate = newTaskCurrentDate

            // Insert the new task into task table
            dao.insert(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.delete(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.update(task)
        }
    }
}