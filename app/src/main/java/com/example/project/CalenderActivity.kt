package com.example.project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class CalenderActivity : BaseActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var recyclerTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val allTasks = mutableListOf<Task>()
    private var selectedDate: Calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calender)
        setupBottomBar()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        calendarView = findViewById(R.id.calendarView)
        recyclerTasks = findViewById(R.id.recyclerTasks)

        recyclerTasks.layoutManager = LinearLayoutManager(this)

        taskAdapter = TaskAdapter(mutableListOf())
        recyclerTasks.adapter = taskAdapter

        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_add)

        fab.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivity(intent)
        }

        calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                selectedDate = eventDay.calendar
                applyFilters()
            }
        })

        // เรียกใช้งานฟังก์ชันดึงข้อมูลมา mark ลงปฏิทิน
        fetchTasksAndMarkCalendar()
    }

    override fun onResume() {
        super.onResume()
        fetchTasksAndMarkCalendar()
    }

    private fun fetchTasksAndMarkCalendar() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("DEBUG_TASK", "User not logged in")
            return
        }

        db.collection("tasks")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                val events: MutableList<EventDay> = ArrayList()
                allTasks.clear()

                // 1. ตั้งค่ารูปแบบเป็น วัน(dd) เดือนย่อ(MMM) ปี(yyyy)
                // 2. ใช้ Locale.getDefault() เพื่อให้อ่านรูปแบบที่บันทึกมาจาก AddTaskActivity ได้ถูกต้อง
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    task.id = document.id
                    if (task.isFinished) {
                        continue
                    }
                    allTasks.add(task)

                    try {
                        val dueDateValue = document.get("dueDate")
                        val calendar = Calendar.getInstance()

                        if (dueDateValue is String) {
                            // ลองแปลงข้อความ (เช่น "27 ก.พ. 2026") เป็นตัวแปร Date
                            val date = sdf.parse(dueDateValue)

                            if (date != null) {
                                calendar.time = date
                                // เพิ่มจุดลงในปฏิทินตามวันที่นั้นๆ
                                events.add(EventDay(calendar, R.drawable.ic_dot_marker))
                            }
                        }
                        // เผื่อไว้กรณีมีข้อมูลเก่าที่เผลอบันทึกเป็น Timestamp
                        else if (dueDateValue is Timestamp) {
                            calendar.time = dueDateValue.toDate()
                            events.add(EventDay(calendar, R.drawable.ic_dot_marker))
                        }

                    } catch (e: Exception) {
                        // ถ้ามีเอกสารไหนวันที่ผิดฟอร์แมต มันจะไม่เด้งแล้ว แต่จะข้ามไปและแจ้งเตือนใน Log แทน
                        Log.e("CalenderActivity", "แปลงวันที่ไม่ได้: ${document.id}", e)
                    }
                }

                // นำจุดทั้งหมดไปแสดงบนปฏิทิน
                calendarView.setEvents(events)

                // เริ่มต้นให้แสดงทั้งหมดทุกครั้งหลัง fetch
                taskAdapter.updateList(allTasks)
            }
            .addOnFailureListener { exception ->
                Log.e("CalenderActivity", "Error getting documents: ", exception)
            }
    }

    private fun applyFilters() {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val filteredByDate = allTasks.filter { task ->
            try {
                if (task.dueDate is String) {
                    val taskDate = sdf.parse(task.dueDate as String)
                    Log.d("DEBUG_TASK_CALENDAR", "taskDate: $taskDate")
                    if (taskDate != null) {
                        val taskCalendar = Calendar.getInstance()
                        taskCalendar.time = taskDate

                        return@filter taskCalendar.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                               taskCalendar.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                    }
                }
            } catch (e: Exception) {
                Log.e("CalenderActivity", "Error parsing task date in filter", e)
            }
            false
        }

        taskAdapter.updateList(filteredByDate)
        recyclerTasks.scrollToPosition(0)
    }
}
