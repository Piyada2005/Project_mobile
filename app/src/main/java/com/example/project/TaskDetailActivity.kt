package com.example.project

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etDetail: EditText
    private lateinit var tvDue: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvNotify: TextView
    private lateinit var tvNoteAdd: TextView
    private lateinit var tvNoteValue: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvLinkAdd: TextView
    private lateinit var tvLinkValue: TextView
    private var taskId: String? = null
    private var attachedLink: String = ""
    private var attachedImageUri: String = ""
    private var isNotifyEnabled = false
    private lateinit var btnAddSubtask: TextView
    private lateinit var btnDeleteTask: MaterialButton
    private lateinit var btnCompleteTask: MaterialButton
    private lateinit var recyclerSubtask: RecyclerView
    private lateinit var adapter: SubtaskAdapter
    private val subtaskList = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        bindViews()

        taskId = intent.getStringExtra("taskId")
        taskId?.let { loadTask(it) }

        setupListeners()
    }

    private fun bindViews() {
        etTitle = findViewById(R.id.deaddProjName)
        etDetail = findViewById(R.id.deaddProjDetail)
        tvDue = findViewById(R.id.detxtDueValue)
        tvTime = findViewById(R.id.detxtTimeValue)
        tvNotify = findViewById(R.id.detxtNotifyValue)
        tvNoteAdd = findViewById(R.id.detvNoteAdd)
        tvNoteValue = findViewById(R.id.detvNoteValue)
        tvCategory = findViewById(R.id.detxtCategoryValue)
        tvLinkAdd = findViewById(R.id.detvLinkAdd)
        tvLinkValue = findViewById(R.id.detvLinkValue)

        findViewById<ImageView>(R.id.debtnBack).setOnClickListener { finish() }
        btnAddSubtask = findViewById(R.id.deaddSubtask)
        btnDeleteTask = findViewById(R.id.debtnDeleteTask)
        btnCompleteTask = findViewById(R.id.debtnCompleteTask)
        recyclerSubtask = findViewById(R.id.derecyclerSubtask)

        adapter = SubtaskAdapter(subtaskList)
        recyclerSubtask.adapter = adapter
        recyclerSubtask.layoutManager = LinearLayoutManager(this)
        recyclerSubtask.visibility = View.GONE
    }

    private fun setupListeners() {

        etTitle.addTextChangedListener {
            updateTask("title", it.toString())
        }

        etDetail.addTextChangedListener {
            updateTask("description", it.toString())
        }

        tvDue.setOnClickListener { openDatePicker() }
        tvTime.setOnClickListener { openTimePicker() }
        tvNotify.setOnClickListener { openReminderBottomSheet() }
        tvNoteAdd.setOnClickListener { openNoteBottomSheet() }
        tvNoteValue.setOnClickListener { openNoteBottomSheet() }
        tvCategory.setOnClickListener { openCategoryDialog() }
        tvLinkAdd.setOnClickListener { showAttachmentBottomSheet() }
        tvLinkValue.setOnClickListener { showAttachmentBottomSheet() }

        btnAddSubtask.setOnClickListener {
            openAddSubtaskDialog()
        }
        btnDeleteTask.setOnClickListener { deleteTask() }
        btnCompleteTask.setOnClickListener { completeTask() }
    }

    private fun loadTask(id: String) {

        db.collection("tasks").document(id).get()
            .addOnSuccessListener { doc ->

                val task = doc.toObject(Task::class.java) ?: return@addOnSuccessListener

                etTitle.setText(task.title)
                etDetail.setText(task.description)
                tvDue.text = task.dueDate
                tvTime.text = task.time
                tvNotify.text = task.notify
                isNotifyEnabled = task.notify != "ปิดการแจ้งเตือน"

                val note = task.note ?: ""

                if (note.isEmpty()) {
                    tvNoteAdd.visibility = View.VISIBLE
                    tvNoteValue.visibility = View.GONE
                } else {
                    tvNoteAdd.visibility = View.GONE
                    tvNoteValue.visibility = View.VISIBLE
                    tvNoteValue.text = note
                }
                tvCategory.text = task.category
                val link = task.link ?: ""

                if (link.isEmpty()) {
                    tvLinkAdd.visibility = View.VISIBLE
                    tvLinkValue.visibility = View.GONE
                } else {
                    tvLinkAdd.visibility = View.GONE
                    tvLinkValue.visibility = View.VISIBLE
                    tvLinkValue.text = link
                }

                attachedLink = task.link ?: ""
                attachedImageUri = task.imageUri ?: ""

                // ✅ โหลด subtasks ตรงนี้เท่านั้น
                val subtasks = doc.get("subtasks") as? List<String>

                subtaskList.clear()
                subtasks?.let {
                    subtaskList.addAll(it)
                }

                recyclerSubtask.visibility =
                    if (subtaskList.isEmpty()) View.GONE else View.VISIBLE

                adapter.notifyDataSetChanged()
            }
    }

    // ================= DATE =================
    private fun openDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("เลือกวันที่กำหนดส่ง")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.show(supportFragmentManager, "DATE")

        picker.addOnPositiveButtonClickListener {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = sdf.format(Date(it))
            tvDue.text = date
            updateTask("dueDate", date)
        }
    }

    // ================= TIME =================
    private fun openTimePicker() {
        val now = Calendar.getInstance()

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText("ตั้งเวลา")
            .build()

        picker.show(supportFragmentManager, "TIME")

        picker.addOnPositiveButtonClickListener {
            val time = String.format("%02d:%02d", picker.hour, picker.minute)
            tvTime.text = time
            updateTask("time", time)
        }
    }

    // ================= REMINDER =================
    private fun openReminderBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_reminder, null)
        dialog.setContentView(view)

        val switchNotify = view.findViewById<Switch>(R.id.switchNotify)
        switchNotify.isChecked = isNotifyEnabled

        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)

        val radios = listOf(
            view.findViewById<RadioButton>(R.id.radio5),
            view.findViewById<RadioButton>(R.id.radio15),
            view.findViewById<RadioButton>(R.id.radio30),
            view.findViewById<RadioButton>(R.id.radio1day),
            view.findViewById<RadioButton>(R.id.radio2day),
            view.findViewById<RadioButton>(R.id.radio3day)
        )

        fun updateRadioState(enabled: Boolean) {
            radios.forEach {
                it.isEnabled = enabled
                it.alpha = if (enabled) 1f else 0.4f
            }
        }

        updateRadioState(switchNotify.isChecked)

        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            isNotifyEnabled = isChecked
            updateRadioState(isChecked)

            if (!isChecked) {
                radioGroup.clearCheck()
            }
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.btnDone).setOnClickListener {

            if (!isNotifyEnabled) {
                tvNotify.text = "ปิดการแจ้งเตือน"
                updateTask("notify", "ปิดการแจ้งเตือน")
            } else {
                val selectedId = radioGroup.checkedRadioButtonId
                if (selectedId != -1) {
                    val selected = view.findViewById<RadioButton>(selectedId)
                    tvNotify.text = selected.text
                    updateTask("notify", selected.text.toString())
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    // ================= NOTE =================
    private fun openNoteBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_note, null)
        dialog.setContentView(view)

        val etNote = view.findViewById<EditText>(R.id.etNote)
        etNote.setText(tvNoteValue.text)

        view.findViewById<ImageView>(R.id.btnSave).setOnClickListener {
            val text = etNote.text.toString()
            if (text.isEmpty()) {
                tvNoteAdd.visibility = View.VISIBLE
                tvNoteValue.visibility = View.GONE
            } else {
                tvNoteAdd.visibility = View.GONE
                tvNoteValue.visibility = View.VISIBLE
                tvNoteValue.text = text
            }

            updateTask("note", text)
            dialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ================= ATTACHMENT =================
    private fun showAttachmentBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_attachment, null)
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.btnGallery).setOnClickListener {
            dialog.dismiss()
            imagePicker.launch("image/*")
        }

        view.findViewById<LinearLayout>(R.id.btnLink).setOnClickListener {
            dialog.dismiss()
            showLinkDialog()
        }

        dialog.show()
    }

    private fun showLinkDialog() {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_link, null)

        val edit = view.findViewById<EditText>(R.id.editLink)
        edit.setText(attachedLink)

        val dialog = AlertDialog.Builder(this)
            .setTitle("แนบลิงก์")
            .setView(view)
            .setPositiveButton("บันทึก") { _, _ ->
                val link = edit.text.toString().trim()
                attachedLink = link
                if (link.isEmpty()) {
                    tvLinkAdd.visibility = View.VISIBLE
                    tvLinkValue.visibility = View.GONE
                } else {
                    tvLinkAdd.visibility = View.GONE
                    tvLinkValue.visibility = View.VISIBLE
                    tvLinkValue.text = link
                }
                updateTask("link", link)
            }
            .setNegativeButton("ยกเลิก", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_bg)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                attachedImageUri = uri.toString()
                tvLinkAdd.visibility = View.GONE
                tvLinkValue.visibility = View.VISIBLE
                tvLinkValue.text = "เลือกรูปแล้ว"
                updateTask("imageUri", attachedImageUri)
            }
        }

    // ================= CATEGORY =================
    private fun openCategoryDialog() {

        val categories = arrayOf("ไม่มีหมวดหมู่", "งาน", "รายการโปรด", "วันเกิด")

        val dialog = AlertDialog.Builder(this)
            .setTitle("เลือกหมวดหมู่")
            .setItems(categories) { _, which ->
                tvCategory.text = categories[which]
                updateTask("category", categories[which])
            }
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_bg)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ================= SUBTASK =================

    private fun openAddSubtaskDialog() {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_subtask, null)

        val edit = view.findViewById<EditText>(R.id.editSubtask)

        val dialog = AlertDialog.Builder(this)
            .setTitle("เพิ่มงานย่อย")
            .setView(view)
            .setPositiveButton("เพิ่ม") { _, _ ->
                val text = edit.text.toString().trim()
                if (text.isNotEmpty()) {
                    addSubtask(text)
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_bg)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addSubtask(text: String) {
        subtaskList.add(text)
        recyclerSubtask.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
        updateSubtasks()
    }

    private fun updateTask(field: String, value: String) {
        taskId?.let {
            db.collection("tasks").document(it).update(field, value)
        }
    }

    private fun updateSubtasks() {
        taskId?.let {
            db.collection("tasks")
                .document(it)
                .update("subtasks", subtaskList)
        }
    }

    private fun deleteTask() {
        val id = taskId ?: return
        db.collection("tasks")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "ลบงานเรียบร้อย", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun completeTask() {
        val id = taskId ?: return
        db.collection("tasks")
            .document(id)
            .update("isFinished", true)
            .addOnSuccessListener {
                Toast.makeText(this, "งานเสร็จสิ้นแล้ว", Toast.LENGTH_SHORT).show()
                finish()
            }
    }


}
