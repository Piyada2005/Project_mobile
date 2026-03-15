package com.example.project

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.util.Log
import android.net.Uri
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import java.text.ParseException
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
    private var taskId: String? = null
    private val attachments = mutableListOf<Attachment>()
    private var attachmentDialogAdapter: AttachmentAdapter? = null
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
        requestNotificationPermissionIfNeeded()

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

        findViewById<ImageView>(R.id.debtnBack).setOnClickListener { finish() }
        btnAddSubtask = findViewById(R.id.deaddSubtask)
        btnDeleteTask = findViewById(R.id.debtnDeleteTask)
        btnCompleteTask = findViewById(R.id.debtnCompleteTask)
        recyclerSubtask = findViewById(R.id.derecyclerSubtask)

        adapter = SubtaskAdapter(subtaskList)
        recyclerSubtask.adapter = adapter
        recyclerSubtask.layoutManager = LinearLayoutManager(this)
        recyclerSubtask.visibility = View.GONE

        setTodayAsDefaultDueDate()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
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
        findViewById<View>(R.id.attachmentSection).setOnClickListener { showAttachmentListModal() }
        tvLinkAdd.setOnClickListener { showAttachmentListModal() }

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
                
                // ใช้ฟังก์ชันกลางเพื่อแสดงผลเหมือน Attachment
                updateNoteSectionVisibility(note)

                tvCategory.text = task.category

                val attachmentList = doc.get("attachments") as? List<Map<String, Any>>

                attachments.clear()

                attachmentList?.forEach {
                    val type = it["type"]?.toString() ?: ""
                    val value = it["value"]?.toString() ?: ""
                    attachments.add(Attachment(type, value))
                }

                updateAttachmentSectionVisibility()
                attachmentDialogAdapter?.notifyDataSetChanged()

                // ✅ โหลด subtasks ตรงนี้เท่านั้น
                val subtasks = doc.get("subtasks") as? List<String>

                subtaskList.clear()
                subtasks?.let {
                    subtaskList.addAll(it)
                }

                recyclerSubtask.visibility =
                    if (subtaskList.isEmpty()) View.GONE else View.VISIBLE

                adapter.notifyDataSetChanged()
                scheduleTaskReminder()
            }
    }

    private fun setTodayAsDefaultDueDate() {
        val dateNow = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        tvDue.text = dateNow
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
            scheduleTaskReminder()
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
            scheduleTaskReminder()
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
                cancelTaskReminder()
            } else {
                val selectedId = radioGroup.checkedRadioButtonId
                if (selectedId != -1) {
                    val selected = view.findViewById<RadioButton>(selectedId)
                    tvNotify.text = selected.text
                    updateTask("notify", selected.text.toString())
                    scheduleTaskReminder()
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
            updateNoteSectionVisibility(text)

            updateTask("note", text)
            dialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateNoteSectionVisibility(note: String) {
        if (note.isNotEmpty()) {
            tvNoteAdd.visibility = View.GONE
            tvNoteValue.visibility = View.VISIBLE
            tvNoteValue.text = note
        } else {
            tvNoteAdd.visibility = View.VISIBLE
            tvNoteValue.visibility = View.GONE
        }
    }

    // ================= ATTACHMENT =================
    private fun showAttachmentBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_attachment, null)
        dialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.btnGallery).setOnClickListener {
            dialog.dismiss()
            imagePicker.launch(arrayOf("image/*"))
        }

        view.findViewById<LinearLayout>(R.id.btnLink).setOnClickListener {
            dialog.dismiss()
            showLinkDialog()
        }

        dialog.show()
    }

    private fun showAttachmentListModal() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_attachment_list, null)
        dialog.setContentView(view)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerAttachmentDialog)
        val tvEmpty = view.findViewById<TextView>(R.id.tvAttachmentEmpty)
        val btnAdd = view.findViewById<MaterialButton>(R.id.btnAddAttachmentModal)
        val btnClose = view.findViewById<ImageView>(R.id.btnCloseAttachmentModal)

        attachmentDialogAdapter = AttachmentAdapter(
            attachments,
            { position ->
                attachments.removeAt(position)
                attachmentDialogAdapter?.notifyDataSetChanged()
                updateAttachmentModalState(tvEmpty, recycler)
                updateAttachmentSectionVisibility()
                updateAttachments()
            },
            { imageIndex ->
                openImageViewer(imageIndex)
            }
        )

        recycler.adapter = attachmentDialogAdapter
        recycler.layoutManager = LinearLayoutManager(this)
        updateAttachmentModalState(tvEmpty, recycler)

        btnAdd.setOnClickListener {
            dialog.dismiss()
            showAttachmentBottomSheet()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateAttachmentModalState(tvEmpty: TextView, recycler: RecyclerView) {
        if (attachments.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        }
    }

    private fun showLinkDialog() {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_link, null)

        val edit = view.findViewById<EditText>(R.id.editLink)
        edit.setText("")

        val dialog = AlertDialog.Builder(this)
            .setTitle("แนบลิงก์")
            .setView(view)
            .setPositiveButton("บันทึก") { _, _ ->
                val link = edit.text.toString().trim()
                attachments.add(
                    Attachment("link", link)
                )

                attachmentDialogAdapter?.notifyDataSetChanged()

                updateAttachmentSectionVisibility()
                updateAttachments()
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
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

            if (uri != null) {

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                attachments.add(
                    Attachment("image", uri.toString())
                )

                attachmentDialogAdapter?.notifyDataSetChanged()

                updateAttachmentSectionVisibility()
                updateAttachments()
            }
        }


    private fun showFullImageDialog(uriString: String) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_full_image)
        
        val fullImageView = dialog.findViewById<ImageView>(R.id.fullImageView)
        val btnClose = dialog.findViewById<ImageView>(R.id.btnCloseImage)
        
        try {
            fullImageView.setImageURI(Uri.parse(uriString))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.show()
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
        cancelTaskReminder()
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
        cancelTaskReminder()
        db.collection("tasks")
            .document(id)
            .update("isFinished", true)
            .addOnSuccessListener {
                Toast.makeText(this, "งานเสร็จสิ้นแล้ว", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateAttachments() {
        taskId?.let {
            db.collection("tasks")
                .document(it)
                .update("attachments", attachments)
        }
    }

    private fun updateAttachmentSectionVisibility() {
        if (attachments.isEmpty()) {
            tvLinkAdd.text = "เพิ่ม"
        } else {
            tvLinkAdd.text = "ดู"
        }
    }

    private fun scheduleTaskReminder() {
        val notifyText = tvNotify.text.toString()
        if (!isNotifyEnabled || notifyText == "ปิดการแจ้งเตือน") {
            cancelTaskReminder()
            return
        }

        val dueAtMillis = parseDueDateTimeMillis() ?: return
        val offsetMillis = reminderOffsetMillis(notifyText)
        val scheduledTime = maxOf(System.currentTimeMillis() + 2000L, dueAtMillis - offsetMillis)

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildReminderPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTime, pendingIntent)
        }
    }

    private fun cancelTaskReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildReminderPendingIntent(PendingIntent.FLAG_NO_CREATE)
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun buildReminderPendingIntent(flag: Int): PendingIntent? {
        val id = taskId ?: return null
        val title = etTitle.text?.toString()?.trim().orEmpty().ifBlank { "งานที่กำหนด" }
        val due = "${tvDue.text} ${tvTime.text}"

        val intent = Intent(this, TaskReminderReceiver::class.java).apply {
            putExtra(TaskReminderReceiver.EXTRA_TITLE, title)
            putExtra(TaskReminderReceiver.EXTRA_DUE, due)
            putExtra(TaskReminderReceiver.EXTRA_NOTIFICATION_ID, id.hashCode())
        }

        val pendingFlag = flag or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, id.hashCode(), intent, pendingFlag)
    }

    private fun parseDueDateTimeMillis(): Long? {
        val dueDate = tvDue.text.toString().trim()
        val dueTime = tvTime.text.toString().trim()

        if (dueDate.isEmpty() || dueTime.isEmpty() || dueTime == "ไม่") return null

        val parser = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        parser.isLenient = false

        return try {
            parser.parse("$dueDate $dueTime")?.time
        } catch (_: ParseException) {
            null
        }
    }

    private fun reminderOffsetMillis(notifyText: String): Long {
        return when {
            notifyText.contains("5 นาที") -> 5L * 60L * 1000L
            notifyText.contains("15 นาที") -> 15L * 60L * 1000L
            notifyText.contains("30 นาที") -> 30L * 60L * 1000L
            notifyText.contains("1 วัน") -> 24L * 60L * 60L * 1000L
            notifyText.contains("2 วัน") -> 2L * 24L * 60L * 60L * 1000L
            notifyText.contains("3 วัน") -> 3L * 24L * 60L * 60L * 1000L
            else -> 0L
        }
    }

    private fun openImageViewer(startPosition: Int) {

        val dialog = android.app.Dialog(
            this,
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )

        val view = layoutInflater.inflate(R.layout.dialog_image_viewer, null)
        dialog.setContentView(view)

        val viewPager = view.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerImages)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)

        val imageList = attachments
            .filter { it.type == "image" }
            .map { it.value }

        val adapter = ImageViewerAdapter(imageList)

        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
