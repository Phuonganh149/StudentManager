package com.example.studentmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.studentmanager.ui.theme.StudentManagerTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var studentDao: StudentDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Database & DAO
        val database = StudentDatabase.getDatabase(this)
        studentDao = database.studentDao()

        enableEdgeToEdge()
        setContent {
            StudentManagerTheme {
                var studentList by remember { mutableStateOf<List<Student>>(emptyList()) }

                // Lắng nghe dữ liệu realtime từ Room Database
                LaunchedEffect(Unit) {
                    studentDao.getAllStudents().collectLatest { list ->
                        studentList = list
                    }
                }

                // Quản lý trạng thái các trang (Navigation bằng state đơn giản)
                // 1: Trang danh sách, 2: Trang chi tiết, 3: Trang Thêm/Sửa
                var currentScreen by remember { mutableStateOf<Int>(1) }
                var selectedStudent by remember { mutableStateOf<Student?>(null) }
                var isEditing by remember { mutableStateOf(false) }

                // Biến điều khiển Hộp thoại xác nhận Sửa/Xóa
                var showConfirmDialog by remember { mutableStateOf(false) }
                var confirmActionType by remember { mutableStateOf("") } // "EDIT" hoặc "DELETE"
                var pendingStudent by remember { mutableStateOf<Student?>(null) }

                // Các biến lưu giá trị form ở Trang 3
                var inputCode by remember { mutableStateOf("") }
                var inputName by remember { mutableStateOf("") }
                var inputEmail by remember { mutableStateOf("") }
                var inputAvatar by remember { mutableStateOf("") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Text(text = when(currentScreen) {
                                    1 -> stringResource(R.string.title_list)
                                    2 -> "Chi Tiết Sinh Viên"
                                    else -> if (isEditing) "Sửa Thông Tin" else "Thêm Sinh Viên"
                                })
                            }
                        )
                    },
                    floatingActionButton = {
                        if (currentScreen == 1) {
                            FloatingActionButton(onClick = {
                                // Mở Trang 3 (Thêm mới)
                                selectedStudent = null
                                inputCode = ""
                                inputName = ""
                                inputEmail = ""
                                inputAvatar = ""
                                isEditing = false
                                currentScreen = 3
                            }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.btn_add))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            // --- TRANG 1: DANH SÁCH SINH VIÊN ---
                            1 -> {
                                if (studentList.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(text = "Chưa có sinh viên nào", color = Color.Gray)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(studentList) { student ->
                                            StudentItem(student = student) {
                                                // Click item -> Mở Trang 2 (Xem chi tiết)
                                                selectedStudent = student
                                                currentScreen = 2
                                            }
                                        }
                                    }
                                }
                            }

                            // --- TRANG 2: CHI TIẾT SINH VIÊN (XEM) ---
                            2 -> {
                                selectedStudent?.let { student ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = "Mã số SV: ${student.studentCode}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Họ tên: ${student.fullName}", fontSize = 18.sp)
                                        Text(text = "Email: ${student.email}", fontSize = 16.sp)
                                        Text(text = "Avatar: ${student.avatarUrl ?: "Không có"}", fontSize = 14.sp, color = Color.Gray)

                                        Spacer(modifier = Modifier.height(20.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(onClick = {
                                                // Chuyển sang Trang 3 (Sửa thông tin)
                                                inputCode = student.studentCode
                                                inputName = student.fullName
                                                inputEmail = student.email
                                                inputAvatar = student.avatarUrl ?: ""
                                                isEditing = true
                                                currentScreen = 3
                                            }) {
                                                Text("Sửa")
                                            }

                                            Button(
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                onClick = {
                                                    // Bật hộp thoại xác nhận Xóa
                                                    pendingStudent = student
                                                    confirmActionType = "DELETE"
                                                    showConfirmDialog = true
                                                }
                                            ) {
                                                Text("Xóa")
                                            }

                                            OutlinedButton(onClick = { currentScreen = 1 }) {
                                                Text("Quay lại")
                                            }
                                        }
                                    }
                                }
                            }

                            // --- TRANG 3: THÊM / SỬA THÔNG TIN SINH VIÊN ---
                            3 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputCode,
                                        onValueChange = { inputCode = it },
                                        label = { Text("Mã số SV") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = inputName,
                                        onValueChange = { inputName = it },
                                        label = { Text("Họ tên SV") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = inputEmail,
                                        onValueChange = { inputEmail = it },
                                        label = { Text("Email") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = inputAvatar,
                                        onValueChange = { inputAvatar = it },
                                        label = { Text("Link Avatar (Tùy chọn)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(onClick = {
                                            if (isEditing && selectedStudent != null) {
                                                // Yêu cầu xác nhận trước khi Sửa
                                                pendingStudent = selectedStudent!!.copy(
                                                    studentCode = inputCode,
                                                    fullName = inputName,
                                                    email = inputEmail,
                                                    avatarUrl = inputAvatar
                                                )
                                                confirmActionType = "EDIT"
                                                showConfirmDialog = true
                                            } else {
                                                // Thêm mới trực tiếp không cần hỏi
                                                lifecycleScope.launch {
                                                    studentDao.insertStudent(
                                                        Student(
                                                            studentCode = inputCode,
                                                            fullName = inputName,
                                                            email = inputEmail,
                                                            avatarUrl = inputAvatar
                                                        )
                                                    )
                                                }
                                                currentScreen = 1
                                            }
                                        }) {
                                            Text(stringResource(R.string.save))
                                        }

                                        OutlinedButton(onClick = { currentScreen = if(isEditing) 2 else 1 }) {
                                            Text(stringResource(R.string.cancel))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- HỘP THOẠI XÁC NHẬN SỬA / XÓA THEO YÊU CẦU ---
                if (showConfirmDialog) {
                    val message = if (confirmActionType == "EDIT") {
                        stringResource(R.string.msg_confirm_edit)
                    } else {
                        stringResource(R.string.msg_confirm_delete)
                    }

                    AlertDialog(
                        onDismissRequest = { showConfirmDialog = false },
                        title = { Text("Xác nhận") },
                        text = { Text(message) },
                        confirmButton = {
                            TextButton(onClick = {
                                showConfirmDialog = false
                                lifecycleScope.launch {
                                    if (confirmActionType == "EDIT" && pendingStudent != null) {
                                        studentDao.updateStudent(pendingStudent!!)
                                        currentScreen = 1
                                    } else if (confirmActionType == "DELETE" && pendingStudent != null) {
                                        studentDao.deleteStudent(pendingStudent!!)
                                        currentScreen = 1 // Quay về trang danh sách sau khi xóa
                                    }
                                }
                            }) {
                                Text(stringResource(R.string.yes))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmDialog = false }) {
                                Text(stringResource(R.string.no))
                            }
                        }
                    )
                }
            }
        }
    }
}

// Giao diện hiển thị mỗi dòng sinh viên trong danh sách (Trang 1)
@Composable
fun StudentItem(student: Student, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(text = student.fullName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = student.studentCode, fontSize = 14.sp, color = Color.Gray)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}