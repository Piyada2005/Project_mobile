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
import com.google.android.material.card.MaterialCardView

class ProfileActivity : BaseActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etName: TextView
    private lateinit var btnEditName: ImageView
    private lateinit var tvEmail: TextView
    private lateinit var btnEditEmail: ImageView
    private lateinit var tvCompletedCount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var pieChart: PieChart
    private lateinit var cardCompletedTasks: MaterialCardView
    private lateinit var cardPendingTasks: MaterialCardView

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
        btnEditName = findViewById(R.id.btnEditName)
        tvEmail = findViewById(R.id.tvEmail)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        pieChart = findViewById(R.id.pieChart)
        cardCompletedTasks = findViewById(R.id.cardCompletedTasks)
        cardPendingTasks = findViewById(R.id.cardPendingTasks)
        setupPieChart()
        setupStatusCardNavigation()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ===== โหลดข้อมูล =====
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("username") ?: doc.getString("name") ?: "")
                tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: ""

                val savedImage = doc.getString("imageUri")
                if (!savedImage.isNullOrEmpty()) {
                    try {
                        imgProfile.setImageURI(Uri.parse(savedImage))
                        imageUri = savedImage
                    } catch (e: Exception) {
                        imgProfile.setImageResource(R.drawable.profile)
                    }
                }
            }
        loadTaskStats(userId)

        val pref = getSharedPreferences("profile", MODE_PRIVATE)
        val savedImage = pref.getString("imageUri", null)

        if (!savedImage.isNullOrEmpty()) {
            try {
                imgProfile.setImageURI(Uri.parse(savedImage))
                imageUri = savedImage
            } catch (e: Exception) {
                imgProfile.setImageResource(R.drawable.profile)
            }
        }

        // ===== กดรูป =====
        imgProfile.setOnClickListener {
            showImageDialog()
        }

        // ===== กดชื่อ =====
        btnEditName.setOnClickListener { showNameDialog() }
        etName.setOnClickListener { showNameDialog() }

        btnEditEmail.setOnClickListener { showEmailDialog() }
        tvEmail.setOnClickListener { showEmailDialog() }


    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().currentUser?.uid?.let { loadTaskStats(it) }
    }

    private fun showEmailDialog() {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_email)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val edit = dialog.findViewById<EditText>(R.id.etEditEmail)
        val btnSave = dialog.findViewById<MaterialButton>(R.id.btnSaveEmail)

        edit.setText(tvEmail.text.toString())

        btnSave.setOnClickListener {

            val newEmail = edit.text.toString()
            val user = FirebaseAuth.getInstance().currentUser

            user?.verifyBeforeUpdateEmail(newEmail)
                ?.addOnSuccessListener {
                    tvEmail.text = newEmail
                    dialog.dismiss()
                }
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ===== Image Picker =====
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                imgProfile.setImageURI(uri)
                imageUri = uri.toString()

                val pref = getSharedPreferences("profile", MODE_PRIVATE)
                pref.edit().putString("imageUri", imageUri).apply()

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

        edit.setText(etName.text.toString())

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
                var workCategoryCount = 0
                var favoriteCategoryCount = 0
                var birthdayCategoryCount = 0
                for (document in result) {
                    val isFinished = document.getBoolean("isFinished") ?: false
                    val category = document.getString("category").orEmpty()
                    if (isFinished) completed++ else pending++
                    when (category) {
                        "งาน" -> workCategoryCount++
                        "รายการโปรด" -> favoriteCategoryCount++
                        "วันเกิด", "วันเกิน" -> birthdayCategoryCount++
                    }
                }
                tvCompletedCount.text = completed.toString()
                tvPendingCount.text = pending.toString()
                updatePieChart(workCategoryCount, favoriteCategoryCount, birthdayCategoryCount)
            }
    }

    private fun setupStatusCardNavigation() {
        cardCompletedTasks.setOnClickListener {
            val intent = Intent(this, CompletedTaskActivity::class.java).apply {
                putExtra(CompletedTaskActivity.EXTRA_IS_FINISHED, true)
                putExtra(CompletedTaskActivity.EXTRA_TITLE, "งานที่เสร็จสมบูรณ์")
            }
            startActivity(intent)
        }

        cardPendingTasks.setOnClickListener {
            val intent = Intent(this, CompletedTaskActivity::class.java).apply {
                putExtra(CompletedTaskActivity.EXTRA_IS_FINISHED, false)
                putExtra(CompletedTaskActivity.EXTRA_TITLE, "งานที่ยังไม่เสร็จ")
            }
            startActivity(intent)
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

    private fun updatePieChart(workCategoryCount: Int, favoriteCategoryCount: Int, birthdayCategoryCount: Int) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        if (workCategoryCount > 0) {
            entries.add(PieEntry(workCategoryCount.toFloat(), "งาน"))
            colors.add(Color.parseColor("#2F80ED"))
        }
        if (favoriteCategoryCount > 0) {
            entries.add(PieEntry(favoriteCategoryCount.toFloat(), "รายการโปรด"))
            colors.add(Color.parseColor("#9FC5FF"))
        }
        if (birthdayCategoryCount > 0) {
            entries.add(PieEntry(birthdayCategoryCount.toFloat(), "วันเกิน"))
            colors.add(Color.parseColor("#5D9CFF"))
        }

        if (entries.isEmpty()) {
            entries.add(PieEntry(1f, "ไม่มีงาน"))
            colors.add(Color.parseColor("#DCEBFF"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 3f
        }

        pieChart.data = PieData(dataSet).apply {
            setDrawValues(false)
        }
        pieChart.centerText = "${workCategoryCount + favoriteCategoryCount + birthdayCategoryCount}\nงาน"
        pieChart.invalidate()
    }
}
