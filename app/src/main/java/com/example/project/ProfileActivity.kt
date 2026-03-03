package com.example.project

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ProfileActivity : BaseActivity() {

    private lateinit var imgProfile: ImageView
    private lateinit var etName: EditText

    private val db = FirebaseFirestore.getInstance()
    private var imageUri: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        setupBottomBar()

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

        // ===== กดรูป =====
        imgProfile.setOnClickListener {
            showImageDialog()
        }

        // ===== กดชื่อ =====
        etName.setOnClickListener {
            showNameDialog()
        }
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
}
