package com.example.project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat

class TaskActivity : BaseActivity() {

    private lateinit var recyclerCategory: RecyclerView
    private lateinit var recyclerTasks: RecyclerView
    private val allTasks = mutableListOf<Task>()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private val db = FirebaseFirestore.getInstance()
    private lateinit var drawerLayout: DrawerLayout
    private val categoryList = listOf("ทั้งหมด", "งาน", "รายการโปรด", "วันเกิด")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        setupBottomBar()

        val menuTask = findViewById<LinearLayout>(R.id.menu_tasks)
        menuTask.setBackgroundResource(R.drawable.segmented_selected_bg)

        recyclerCategory = findViewById(R.id.recyclerCategory)
        recyclerTasks = findViewById(R.id.recyclerTasks)

        recyclerCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        recyclerTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(mutableListOf())
        recyclerTasks.adapter = taskAdapter

        // ✅ ตั้งค่า CategoryAdapter แบบใหม่
        categoryAdapter = CategoryAdapter(categoryList) { selectedCategory ->
            selectCategory(selectedCategory)
        }
        recyclerCategory.adapter = categoryAdapter

        drawerLayout = findViewById(R.id.drawerLayout)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // ✅ Drawer เรียก selectCategory เหมือนกันหมด
        findViewById<LinearLayout>(R.id.menuAll).setOnClickListener {
            selectCategory("ทั้งหมด")
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menuWork).setOnClickListener {
            selectCategory("งาน")
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menuFavorite).setOnClickListener {
            selectCategory("รายการโปรด")
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menuBirthday).setOnClickListener {
            selectCategory("วันเกิด")
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menuProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<LinearLayout>(R.id.menuStar).setOnClickListener {
            startActivity(Intent(this, StarActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }

        loadTasks()
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
                    task.id = document.id
                    if (!task.isFinished) {
                        allTasks.add(task)
                    }
                }

                Log.d("DEBUG_TASK", "Loaded ${allTasks.size} tasks")

                // ✅ โหลดเสร็จให้เลือก "ทั้งหมด"
                selectCategory("ทั้งหมด")
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG_TASK", "Error loading tasks", e)
            }
    }

    // 🔥 ฟังก์ชันกลางควบคุมทุกอย่าง
    private fun selectCategory(category: String) {

        // 1️⃣ กรองข้อมูล
        val filteredResult = when (category) {
            "ทั้งหมด" -> {
                allTasks
            }
            "รายการโปรด" -> {
                allTasks.filter { it.isStarred }
            }
            else -> {
                allTasks.filter { it.category == category }
            }
        }

        taskAdapter.updateList(filteredResult)

        // 2️⃣ ไฮไลต์หมวด
        categoryAdapter.setSelectedCategory(category)

        // 3️⃣ เลื่อนไปตำแหน่งหมวดนั้น
        val position = categoryList.indexOf(category)
        if (position != -1) {
            recyclerCategory.smoothScrollToPosition(position)
        }

        // 4️⃣ เลื่อนรายการงานขึ้นบนสุด
        recyclerTasks.scrollToPosition(0)
    }
}
