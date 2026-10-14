package com.example.studentmanager

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    // Lấy toàn bộ danh sách, tự động cập nhật UI khi có thay đổi
    @Query("SELECT * FROM students ORDER BY id DESC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)
}