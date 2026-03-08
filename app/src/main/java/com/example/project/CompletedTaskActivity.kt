package com.example.project

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CompletedTaskActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var taskAdapter: StatusTaskSectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_completed_task)

        val isFinished = intent.getBooleanExtra(EXTRA_IS_FINISHED, true)
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: if (isFinished) "งานที่เสร็จสมบูรณ์" else "งานที่ยังไม่เสร็จ"

        findViewById<TextView>(R.id.detitle).text = title
        findViewById<ImageView>(R.id.stabtnBack).setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.recyclerCompleted)
        recycler.layoutManager = LinearLayoutManager(this)
        taskAdapter = StatusTaskSectionAdapter()
        recycler.adapter = taskAdapter

        loadTasksByStatus(isFinished)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadTasksByStatus(isFinished: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        db.collection("tasks")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isFinished", isFinished)
            .get()
            .addOnSuccessListener { result ->
                val tasks = mutableListOf<Task>()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    task.id = document.id
                    tasks.add(task)
                }
                taskAdapter.updateTasks(tasks, strikeThrough = isFinished)
            }
    }

    companion object {
        const val EXTRA_IS_FINISHED = "extra_is_finished"
        const val EXTRA_TITLE = "extra_title"
    }
}
