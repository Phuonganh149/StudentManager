package com.example.studentmanager

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentCode: String, // Mã số SV
    val fullName: String,    // Họ tên SV
    val email: String,       // Email
    val avatarUrl: String?   // Link ảnh avatar
)