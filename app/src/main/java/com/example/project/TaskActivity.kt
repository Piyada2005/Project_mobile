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

        recyclerTasks.layoutManager =
            LinearLayoutManager(this)

        taskAdapter = TaskAdapter(displayList)
        recyclerTasks.adapter = taskAdapter

        // โหลดครั้งแรก
        loadTasks()

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        val categoryList = listOf("ทั้งหมด", "งาน", "รายการโปรด", "วันเกิด")

        recyclerCategory.adapter = CategoryAdapter(categoryList) { selectedCategory ->
            filterTasks(selectedCategory)
        }
    }

    // 🔥 รีโหลดทุกครั้งที่กลับมาหน้านี้
    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {

        if (userId.isEmpty()) {
            Log.e("TaskActivity", "User not logged in")
            return
        }

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
                        Log.e("TaskActivity", "Parse error: ${e.message}")
                    }
                }

                // แสดงทั้งหมดเป็น default
                filterTasks("ทั้งหมด")
            }
            .addOnFailureListener { e ->
                Log.e("TaskActivity", "Load error", e)
            }
    }

    private fun filterTasks(category: String) {

        val filteredResult = when (category) {

            "ทั้งหมด" -> allTasks

            "งาน" -> allTasks.filter {
                (it.category ?: "") == "งาน"
            }

            "วันเกิด" -> allTasks.filter {
                (it.category ?: "") == "วันเกิด"
            }

            "รายการโปรด" -> allTasks.filter {
                (it.category ?: "") == "รายการโปรด"
            }

            else -> allTasks
        }

        taskAdapter.updateList(filteredResult)
    }
}