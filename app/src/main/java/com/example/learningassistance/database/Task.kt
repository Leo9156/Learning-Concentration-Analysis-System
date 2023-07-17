package com.example.learningassistance.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    var taskId: Long = 0L,

    @ColumnInfo(name = "task_name")
    var taskName: String = "",

    @ColumnInfo(name = "task_description")
    var taskDescription: String = "",

    @ColumnInfo(name = "task_duration")
    var taskDurationMin: Int = 0,

    @ColumnInfo(name = "task_date")
    var taskDate: String = "",

    @ColumnInfo(name = "task_done")
    var taskDone: Boolean = false,

    @ColumnInfo(name = "task_complete_percentage")
    var taskCompletePercentage: Int = 0
)