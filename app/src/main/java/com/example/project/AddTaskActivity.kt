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
import android.app.TimePickerDialog
import android.content.res.Resources
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.widget.LinearLayout
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class AddTaskActivity : AppCompatActivity() {

    private val subtaskList = mutableListOf<String>()
    private lateinit var adapter: SubtaskAdapter

    private var selectedCategory = "ไม่มีหมวดหมู่"

    // private val txtDueValue = findViewById<TextView>(R.id.txtDueValue)
    // private val txtTimeValue = findViewById<TextView>(R.id.txtTimeValue)
    // private val txtNotifyValue = findViewById<TextView>(R.id.txtNotifyValue)
    // private val txtRepeatValue = findViewById<TextView>(R.id.txtRepeatValue)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_task)

        val tvLinkValue = findViewById<TextView>(R.id.tvLinkValue)

        tvLinkValue.setOnClickListener {
            showAttachmentBottomSheet()
        }

        val txtDueValue = findViewById<TextView>(R.id.txtDueValue)
        val txtTimeValue = findViewById<TextView>(R.id.txtTimeValue)
        val txtNotifyValue = findViewById<TextView>(R.id.txtNotifyValue)
        val txtRepeatValue = findViewById<TextView>(R.id.txtRepeatValue)
        val txtNote = findViewById<TextView>(R.id.tvNoteValue)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerSubtask)
        val btnAddSubtask = findViewById<TextView>(R.id.addSubtask)

        adapter = SubtaskAdapter(subtaskList)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
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
                        recycler.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
        }

        txtDueValue.setOnClickListener {

            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("เลือกวันที่กำหนดส่ง")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(supportFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val date = sdf.format(Date(selection))

                txtDueValue.text = date
            }
        }

        txtTimeValue.setOnClickListener {

            val calendar = Calendar.getInstance()

            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("ตั้งเวลา")
                .build()

            picker.show(supportFragmentManager, "TIME_PICKER")

            picker.addOnPositiveButtonClickListener {
                val hour = picker.hour
                val minute = picker.minute
                txtTimeValue.text = String.format("%02d:%02d", hour, minute)
            }
        }

        txtNotifyValue.setOnClickListener {

            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottomsheet_reminder, null)
            dialog.setContentView(view)

            val btnClose = view.findViewById<ImageView>(R.id.btnClose)
            val btnDone = view.findViewById<ImageView>(R.id.btnDone)
            val switchNotify = view.findViewById<Switch>(R.id.switchNotify)
            val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroup)

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnDone.setOnClickListener {

                if (!switchNotify.isChecked) {
                    txtNotifyValue.text = "ปิดการแจ้งเตือน"
                } else {
                    val selectedId = radioGroup.checkedRadioButtonId

                    if (selectedId != -1) {
                        val selected =
                            view.findViewById<RadioButton>(selectedId)
                        txtNotifyValue.text = selected.text
                    }
                }

                dialog.dismiss()
            }

            dialog.show()
        }

        txtRepeatValue.setOnClickListener {

            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.bottomsheet_repeat, null)
            dialog.setContentView(view)

            val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
            val content = view.findViewById<FrameLayout>(R.id.contentContainer)

            val tabs = listOf("ชั่วโมง", "รายวัน", "รายสัปดาห์", "รายเดือน", "รายปี")

            tabs.forEach { tabLayout.addTab(tabLayout.newTab().setText(it)) }

            fun loadLayout(layoutId: Int) {content.removeAllViews()
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

            view.findViewById<ImageView>(R.id.btnClose).setOnClickListener {
                dialog.dismiss()
            }

            view.findViewById<ImageView>(R.id.btnDone).setOnClickListener {
                txtRepeatValue.text = tabLayout.getTabAt(tabLayout.selectedTabPosition)?.text
                dialog.dismiss()
            }

            dialog.show()
        }

        txtNote.setOnClickListener {

            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_note, null)
            dialog.setContentView(view)
            dialog.window?.setLayout(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            val etNote = view.findViewById<EditText>(R.id.etNote)
            val tvCounter = view.findViewById<TextView>(R.id.tvCounter)
            val btnClose = view.findViewById<ImageView>(R.id.btnClose)
            val btnSave = view.findViewById<ImageView>(R.id.btnSave)

            etNote.addTextChangedListener {
                tvCounter.text = "${it?.length}/3000"
            }

            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val noteText = etNote.text.toString()
                txtNote.text = if (noteText.isEmpty()) "หมายเหตุ" else noteText
                dialog.dismiss()
            }

            dialog.show()

            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                it.layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()   // ปิดหน้านี้ กลับหน้าก่อนหน้า
        }


        val saveButton = findViewById<ImageView>(R.id.saveButton)

        saveButton.setOnClickListener {

            val auth = FirebaseAuth.getInstance()

            if (auth.currentUser == null) {
                Toast.makeText(this, "กรุณาเข้าสู่ระบบก่อน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val title = findViewById<EditText>(R.id.addProjName).text.toString()
            val detail = findViewById<EditText>(R.id.addProjDetail).text.toString()

            if (title.isEmpty()) {
                findViewById<EditText>(R.id.addProjName).error = "กรุณากรอกชื่องาน"
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val userId = auth.currentUser!!.uid

            val task = hashMapOf(
                "title" to title,
                "description" to detail,
                "category" to selectedCategory,
                "dueDate" to txtDueValue.text.toString(),
                "time" to txtTimeValue.text.toString(),
                "notify" to txtNotifyValue.text.toString(),
                "repeat" to txtRepeatValue.text.toString(),
                "note" to txtNote.text.toString(),
                "userId" to userId,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("tasks")
                .add(task)
                .addOnSuccessListener {
                    Toast.makeText(this, "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        val tvCategoryValue = findViewById<TextView>(R.id.tvCategoryValue)

        tvCategoryValue.setOnClickListener {

            val categories = arrayOf(
                "ไม่มีหมวดหมู่",
                "งาน",
                "รายการโปรด",
                "วันเกิด"
            )

            AlertDialog.Builder(this)
                .setTitle("เลือกหมวดหมู่")
                .setItems(categories) { _, which ->
                    selectedCategory = categories[which]
                    tvCategoryValue.text = selectedCategory
                }
                .show()
        }

    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                findViewById<TextView>(R.id.tvLinkValue).text = "เลือกรูปแล้ว"
            }
        }

    private fun showAttachmentBottomSheet() {

        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottomsheet_attachment, null)
        dialog.setContentView(view)

        val btnGallery = view.findViewById<LinearLayout>(R.id.btnGallery)
        val btnLink = view.findViewById<LinearLayout>(R.id.btnLink)

        btnGallery.setOnClickListener {
            dialog.dismiss()
            openGallery()
        }

        btnLink.setOnClickListener {
            dialog.dismiss()
            showLinkDialog()
        }

        dialog.show()
    }

    private fun openGallery() {
        imagePicker.launch("image/*")
    }

    private fun showLinkDialog() {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_link, null)

        val edit = view.findViewById<EditText>(R.id.editLink)

        AlertDialog.Builder(this)
            .setTitle("แนบลิงก์")
            .setView(view)
            .setPositiveButton("เพิ่ม") { _, _ ->
                val link = edit.text.toString()
                if (link.isNotEmpty()) {
                    findViewById<TextView>(R.id.tvLinkValue).text = link
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    // txtDueValue.setOnClickListener {
    //     // ยังไม่ต้องทำ action
    // }



    // txtNotifyValue.setOnClickListener {
    // }

    // txtRepeatValue.setOnClickListener {
    // }
}