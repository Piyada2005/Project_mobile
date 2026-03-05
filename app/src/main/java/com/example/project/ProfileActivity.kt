package com.example.project

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : BaseActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etName: EditText
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var pieChart: PieChart

    private val db = FirebaseFirestore.getInstance()
    private var imageUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        setupBottomBar()

        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)
        menuProfile.setBackgroundResource(R.drawable.segmented_selected_bg)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ===== Logout (ของเดิม ไม่ลบ) =====
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // ===== Profile Views =====
        imgProfile = findViewById(R.id.imgProfile)
        etName = findViewById(R.id.etName)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        pieChart = findViewById(R.id.pieChart)
        setupPieChart()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ===== โหลดข้อมูล =====
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("username") ?: doc.getString("name") ?: "")

                val savedImage = doc.getString("imageUri")
                if (!savedImage.isNullOrEmpty()) {
                    imgProfile.setImageURI(Uri.parse(savedImage))
                    imageUri = savedImage
                }
            }
        loadTaskStats(userId)

        // ===== กดรูป =====
        imgProfile.setOnClickListener {
            showImageDialog()
        }

        // ===== กดชื่อ =====
        etName.setOnClickListener {
            showNameDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().currentUser?.uid?.let { loadTaskStats(it) }
    }

    // ===== Image Picker =====
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imgProfile.setImageURI(uri)
                imageUri = uri.toString()
                saveProfile()
            }
        }

    // ===== Dialog ดูรูป (โค้งมน) =====
    private fun showImageDialog() {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_profile_image)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val imgPreview = dialog.findViewById<ImageView>(R.id.imgPreview)
        val btnChange = dialog.findViewById<MaterialButton>(R.id.btnChangeImage)

        if (imageUri.isNotEmpty()) {
            imgPreview.setImageURI(Uri.parse(imageUri))
        } else {
            imgPreview.setImageResource(R.drawable.profile)
        }

        btnChange.setOnClickListener {
            imagePicker.launch("image/*")
            dialog.dismiss()
        }

        dialog.show()

        // ✅ เพิ่มตรงนี้
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ===== Dialog แก้ชื่อ (โค้งมน) =====
    private fun showNameDialog() {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_name)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val edit = dialog.findViewById<EditText>(R.id.etEditName)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSaveName)

        edit.setText(etName.text)

        btnSave.setOnClickListener {
            etName.setText(edit.text.toString())
            saveProfile()
            dialog.dismiss()
        }

        dialog.show()

        // ✅ เพิ่มตรงนี้
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ===== Save Firestore =====
    private fun saveProfile() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "username" to etName.text.toString(),
            "imageUri" to imageUri
        )

        db.collection("users").document(userId)
            .set(data, SetOptions.merge())
    }

    private fun loadTaskStats(userId: String) {
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                var completed = 0
                var pending = 0
                for (document in result) {
                    val isFinished = document.getBoolean("isFinished") ?: false
                    if (isFinished) completed++ else pending++
                }
                tvCompletedCount.text = completed.toString()
                tvPendingCount.text = pending.toString()
                updatePieChart(completed, pending)
            }
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.setUsePercentValues(false)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 62f
        pieChart.transparentCircleRadius = 66f
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setDrawEntryLabels(false)
        pieChart.setCenterTextSize(12f)
        pieChart.setCenterTextColor(Color.parseColor("#666666"))
    }

    private fun updatePieChart(completed: Int, pending: Int) {
        val entries = mutableListOf<PieEntry>()
        if (completed > 0) entries.add(PieEntry(completed.toFloat(), "เสร็จ"))
        if (pending > 0) entries.add(PieEntry(pending.toFloat(), "ค้าง"))
        if (entries.isEmpty()) entries.add(PieEntry(1f, "ไม่มีงาน"))

        val colors = if (completed == 0 && pending == 0) {
            listOf(Color.parseColor("#DCEBFF"))
        } else {
            listOf(Color.parseColor("#2F80ED"), Color.parseColor("#9FC5FF"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
        }

        pieChart.data = PieData(dataSet).apply {
            setDrawValues(false)
        }
        pieChart.centerText = "${completed + pending}\nงาน"
        pieChart.invalidate()
    }
}
