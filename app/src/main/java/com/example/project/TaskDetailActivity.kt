package com.example.project

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.util.Log
import android.net.Uri
import android.content.Intent
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
    private lateinit var ivAttachmentThumbnail: com.google.android.material.imageview.ShapeableImageView
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
        ivAttachmentThumbnail = findViewById(R.id.detvImageThumbnail)

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

        // findViewById<ViewGroup>(R.id.attachmentSection).setOnClickListener { 
        //    showAttachmentBottomSheet() 
        // }
        
        // tvLinkAdd.setOnClickListener { showAttachmentBottomSheet() }
        // tvLinkValue.setOnClickListener { showAttachmentBottomSheet() }

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

                attachedLink = task.link ?: ""
                attachedImageUri = task.imageUri ?: ""
                Log.d("TaskDetail", "loadTask: fetched imageUri = '$attachedImageUri'")
                updateAttachmentSectionVisibility()

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
                updateTask("link", link)
                updateAttachmentSectionVisibility()
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
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    attachedImageUri = uri.toString()
                    updateTask("imageUri", attachedImageUri)
                    updateAttachmentSectionVisibility()
                } catch (e: Exception) {
                    Log.e("TaskDetail", "Failed to take persistable permission: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

    private fun updateAttachmentSectionVisibility() {
        val hasLink = attachedLink.isNotEmpty()
        val hasImage = attachedImageUri.isNotEmpty()
        
        Log.d("TaskDetail", "updateAttachment: hasLink=$hasLink, hasImage=$hasImage, uri='$attachedImageUri'")
        
        val attachmentSection = findViewById<ViewGroup>(R.id.attachmentSection)

        if (hasLink || hasImage) {
            
            // ถ้ามีลิงก์ ห้ามกดที่แถว (กดได้แค่ตัวลิงก์)
            // ถ้ามีแค่รูป ให้กดที่แถวเพื่อแก้ไขได้
            if (hasLink) {
                attachmentSection.setOnClickListener(null)
                attachmentSection.isClickable = false
                attachmentSection.isFocusable = false
            } else {
                attachmentSection.setOnClickListener { showAttachmentBottomSheet() }
                attachmentSection.isClickable = true
                attachmentSection.isFocusable = true
            }

            when {
                hasLink && hasImage -> {
                    // ทั้งคู่: แสดงลิงก์ขวา, รูปเล็กขวาสุด
                    tvLinkAdd.visibility = View.GONE
                    tvLinkValue.visibility = View.VISIBLE
                    tvLinkValue.text = attachedLink
                    
                    ivAttachmentThumbnail.visibility = View.VISIBLE
                    try {
                        ivAttachmentThumbnail.setImageURI(Uri.parse(attachedImageUri))
                        ivAttachmentThumbnail.setOnClickListener { showFullImageDialog(attachedImageUri) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // ปรับ constraint ของ Link Value ให้ไม่ทับรูป
                    val params = tvLinkValue.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.endToStart = R.id.detvImageThumbnail
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    params.marginEnd = 8
                    tvLinkValue.layoutParams = params
                }
                hasLink -> {
                    // แค่ลิงก์: แสดงลิงก์ขวา, ซ่อนรูป
                    tvLinkAdd.visibility = View.GONE
                    tvLinkValue.visibility = View.VISIBLE
                    tvLinkValue.text = attachedLink
                    
                    ivAttachmentThumbnail.visibility = View.GONE
                    
                    // Reset constraint
                    val params = tvLinkValue.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    tvLinkValue.layoutParams = params
                }
                hasImage -> {
                    // แค่รูป: ลบคำว่า "ไม่มี", แสดงรูปเล็กขวาสุด
                    tvLinkAdd.visibility = View.GONE
                    tvLinkValue.visibility = View.GONE
                    
                    ivAttachmentThumbnail.visibility = View.VISIBLE
                    try {
                        ivAttachmentThumbnail.setImageURI(Uri.parse(attachedImageUri))
                        ivAttachmentThumbnail.setOnClickListener { showFullImageDialog(attachedImageUri) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            // ไม่มีข้อมูล: ห้ามกด!
            attachmentSection.setOnClickListener(null)
            attachmentSection.isClickable = false
            attachmentSection.isFocusable = false

            // แสดง "ไม่มี", ซ่อนค่า, ซ่อนรูป
            tvLinkAdd.visibility = View.VISIBLE
            tvLinkAdd.text = "ไม่มี"
            tvLinkValue.visibility = View.GONE
            ivAttachmentThumbnail.visibility = View.GONE
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
