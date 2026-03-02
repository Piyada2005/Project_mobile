package com.example.project

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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
    private lateinit var tvRepeat: TextView
    private lateinit var tvNote: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvLink: TextView

    private var taskId: String? = null
    private var attachedLink: String = ""
    private var attachedImageUri: String = ""

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
        tvRepeat = findViewById(R.id.detxtRepeatValue)
        tvNote = findViewById(R.id.detvNoteValue)
        tvCategory = findViewById(R.id.detvCategoryValue)
        tvLink = findViewById(R.id.detvLinkValue)

        findViewById<ImageView>(R.id.debtnBack).setOnClickListener { finish() }
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
        tvRepeat.setOnClickListener { openRepeatBottomSheet() }
        tvNote.setOnClickListener { openNoteBottomSheet() }
        tvCategory.setOnClickListener { openCategoryDialog() }
        tvLink.setOnClickListener { showAttachmentBottomSheet() }
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
                tvRepeat.text = task.repeat
                tvNote.text = task.note
                tvCategory.text = task.category
                tvLink.text = task.link

                attachedLink = task.link ?: ""
                attachedImageUri = task.imageUri ?: ""
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
        val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.btnDone).setOnClickListener {

            if (!switchNotify.isChecked) {
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

    // ================= REPEAT =================
    private fun openRepeatBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_repeat, null)
        dialog.setContentView(view)

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val content = view.findViewById<FrameLayout>(R.id.contentContainer)

        val tabs = listOf("ชั่วโมง", "รายวัน", "รายสัปดาห์", "รายเดือน", "รายปี")
        tabs.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }

        fun loadLayout(layoutId: Int) {
            content.removeAllViews()
            val child = layoutInflater.inflate(layoutId, content, false)
            content.addView(child)
        }

        loadLayout(R.layout.repeat_hourly)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> loadLayout(R.layout.repeat_hourly)
                    1 -> loadLayout(R.layout.repeat_daily)
                    2 -> loadLayout(R.layout.repeat_weekly)
                    3 -> loadLayout(R.layout.repeat_monthly)
                    4 -> loadLayout(R.layout.repeat_yearly)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        view.findViewById<ImageView>(R.id.btnDone).setOnClickListener {
            val value = tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text.toString()
            tvRepeat.text = value
            updateTask("repeat", value)
            dialog.dismiss()
        }

        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
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
        etNote.setText(tvNote.text)

        view.findViewById<ImageView>(R.id.btnSave).setOnClickListener {
            val text = etNote.text.toString()
            tvNote.text = text
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

        AlertDialog.Builder(this)
            .setTitle("แนบลิงก์")
            .setView(view)
            .setPositiveButton("บันทึก") { _, _ ->
                val link = edit.text.toString().trim()
                attachedLink = link
                tvLink.text = link
                updateTask("link", link)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                attachedImageUri = uri.toString()
                tvLink.text = "เลือกรูปแล้ว"
                updateTask("imageUri", attachedImageUri)
            }
        }

    // ================= CATEGORY =================
    private fun openCategoryDialog() {

        val categories = arrayOf("ไม่มีหมวดหมู่", "งาน", "รายการโปรด", "วันเกิด")

        AlertDialog.Builder(this)
            .setTitle("เลือกหมวดหมู่")
            .setItems(categories) { _, which ->
                tvCategory.text = categories[which]
                updateTask("category", categories[which])
            }
            .show()
    }

    private fun updateTask(field: String, value: String) {
        taskId?.let {
            db.collection("tasks").document(it).update(field, value)
        }
    }
}