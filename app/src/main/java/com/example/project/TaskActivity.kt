package com.example.project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskActivity : BaseActivity() {

    private lateinit var recyclerCategory: RecyclerView
    private lateinit var recyclerTasks: RecyclerView

    private val allTasks = mutableListOf<Task>()
    private val displayList = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        setupBottomBar()

        recyclerCategory = findViewById(R.id.recyclerCategory)
        recyclerTasks = findViewById(R.id.recyclerTasks)

        recyclerCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(displayList)
        recyclerTasks.adapter = taskAdapter

        loadTasks()

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            // เช็คว่ามี Activity นี้จริงไหม หรือเปลี่ยนเป็น AddTaskActivity ตามโปรเจกต์คุณ
            try {
                startActivity(Intent(this, AddTaskActivity::class.java))
            } catch (e: Exception) {
                Log.e("TaskActivity", "AddTaskActivity not found: ${e.message}")
            }
        }

        val categoryList = listOf("ทั้งหมด", "งาน", "รายการโปรด", "วันเกิด")
        recyclerCategory.adapter = CategoryAdapter(categoryList) { selectedCategory ->
            filterTasks(selectedCategory)
        }
    }

    private fun loadTasks() {
        if (userId.isEmpty()) return

        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                allTasks.clear()
                for (document in result) {
                    try {
                        val task = document.toObject(Task::class.java)
                        allTasks.add(task)
                    } catch (e: Exception) {
                        Log.e("TaskActivity", "Error parsing task: ${e.message}")
                    }
                }
                filterTasks("ทั้งหมด")
            }
            .addOnFailureListener { e ->
                Log.e("TaskActivity", "Error loading tasks", e)
            }
    }

    private fun filterTasks(category: String) {
        val filteredResult = if (category == "ทั้งหมด") {
            allTasks
        } else {
            // เพิ่มการเช็ค null (it.category ?: "") เพื่อป้องกันแอปเด้งถ้าข้อมูลใน Firestore ไม่มีหมวดหมู่
            allTasks.filter { (it.category ?: "") == category }
        }
        
        taskAdapter.updateList(filteredResult)
    }
}