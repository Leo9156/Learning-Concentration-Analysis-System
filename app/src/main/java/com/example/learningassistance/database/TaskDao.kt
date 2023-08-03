package com.example.learningassistance.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM task_table ORDER BY task_id")
    fun getAll(): LiveData<List<Task>>

    @Query("SELECT * FROM task_table WHERE task_done = 0 ORDER BY task_id DESC")
    fun getAllNotYetDone(): LiveData<List<Task>>

    @Query("SELECT * FROM task_table WHERE task_done = 1 AND task_date == :date ORDER BY task_id DESC")
    suspend fun getAllDoneFromDate(date: String): List<Task>

    @Query("SELECT * FROM task_table WHERE task_done = 1 AND task_date BETWEEN :startDate AND :currentDate")
    suspend fun getAllDoneFromWeek(startDate: String, currentDate: String): List<Task>

    @Query("SELECT * FROM task_table WHERE task_id == :id")
    suspend fun get(id: Long): Task

    @Query("DELETE FROM task_table")
    suspend fun deleteAll()
}