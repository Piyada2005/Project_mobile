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
    // ไม่จำเป็นต้องใช้ displayList แยก ถ้า adapter มีกลไก updateList อยู่แล้ว
    private lateinit var taskAdapter: TaskAdapter

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        setupBottomBar()

        recyclerCategory = findViewById(R.id.recyclerCategory)
        recyclerTasks = findViewById(R.id.recyclerTasks)

        recyclerCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerTasks.layoutManager = LinearLayoutManager(this)

        // เริ่มต้นด้วย List ว่างเปล่า
        taskAdapter = TaskAdapter(mutableListOf())
        recyclerTasks.adapter = taskAdapter

        loadTasks()

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        val categoryList = listOf("ทั้งหมด", "งาน", "รายการโปรด", "วันเกิด")
        recyclerCategory.adapter = CategoryAdapter(categoryList) { selectedCategory ->
            filterTasks(selectedCategory)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("DEBUG_TASK", "User not logged in")
            return
        }

        db.collection("tasks")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                allTasks.clear()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    // เพิ่มบรรทัดนี้เพื่อแอบดูว่ามันแปลงค่าได้ครบไหม หรือเป็น null
                    Log.d("DEBUG_TASK", "ชื่อ: ${task.title}, หมวด: ${task.category}")
                    allTasks.add(task)
                }
                Log.d("DEBUG_TASK", "Loaded ${allTasks.size} tasks")
                filterTasks("ทั้งหมด")
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG_TASK", "Error loading tasks", e)
            }
    }

    private fun filterTasks(category: String) {
        val filteredResult = if (category == "ทั้งหมด") {
            allTasks
        } else {
            allTasks.filter { it.category == category }
        }

        // ตรวจสอบว่าใน TaskAdapter มีฟังก์ชัน updateList(newList: List<Task>)
        // ที่เรียก notifyDataSetChanged() หรือยัง
        taskAdapter.updateList(filteredResult)
    }
}