package com.example.project

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AddTaskActivity : AppCompatActivity() {

    private val subtaskList = mutableListOf<String>()
    private lateinit var adapter: SubtaskAdapter
    private val txtDueValue = findViewById<TextView>(R.id.txtDueValue)
    private val txtTimeValue = findViewById<TextView>(R.id.txtTimeValue)
    private val txtNotifyValue = findViewById<TextView>(R.id.txtNotifyValue)
    private val txtRepeatValue = findViewById<TextView>(R.id.txtRepeatValue)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerSubtask)
        val btnAddSubtask = findViewById<TextView>(R.id.addSubtask)

        adapter = SubtaskAdapter(subtaskList)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)

        // ซ่อนก่อน
        recycler.visibility = View.GONE

        btnAddSubtask.setOnClickListener {

            val view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_subtask, null)

            val edit = view.findViewById<EditText>(R.id.editSubtask)

            AlertDialog.Builder(this)
                .setTitle("เพิ่มงานย่อย")
                .setView(view)
                .setPositiveButton("เพิ่ม") { _, _ ->

                    val text = edit.text.toString()

                    if (text.isNotEmpty()) {

                        subtaskList.add(text)

                        // แสดง RecyclerView เมื่อมีข้อมูล
                        recycler.visibility = View.VISIBLE

                        adapter.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
        }
    }
//    txtDueValue.setOnClickListener {
//        // ยังไม่ต้องทำ action
//    }
//
//    txtTimeValue.setOnClickListener {
//    }
//
//    txtNotifyValue.setOnClickListener {
//    }
//
//    txtRepeatValue.setOnClickListener {
//    }
}