package com.example.project

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StarActivity : AppCompatActivity() {

    private lateinit var recyclerStar: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val starList = mutableListOf<Task>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_star)

        recyclerStar = findViewById(R.id.recyclerStar)
        recyclerStar.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(starList)
        recyclerStar.adapter = taskAdapter

        findViewById<ImageView>(R.id.stabtnBack).setOnClickListener {
            finish()
        }

        loadStarTasks()
    }

    private fun loadStarTasks() {

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("tasks")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isStarred", true)   // ⭐ ดึงเฉพาะที่ติดดาว
            .get()
            .addOnSuccessListener { result ->

                starList.clear()

                for (document in result) {

                    val task = document.toObject(Task::class.java)
                    task.id = document.id   // ⭐ สำคัญมาก

                    starList.add(task)
                }

                taskAdapter.notifyDataSetChanged()
            }
    }
}