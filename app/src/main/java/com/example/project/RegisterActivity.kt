package com.example.project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private val TAG: String = "RegisterActivity"

    private var regisEmail: EditText? = null
    private var regisUsername: EditText? = null
    private var regisPass: EditText? = null
    private var regisCPass: EditText? = null
    private var createAcc: Button? = null
    private var rSignin: TextView? = null
    private var backR: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()

        mAuth = FirebaseAuth.getInstance()

        createAcc?.setOnClickListener {

            val email = regisEmail?.text.toString().trim()
            val username = regisUsername?.text.toString().trim()
            val password = regisPass?.text.toString().trim()
            val confirmPassword = regisCPass?.text.toString().trim()

            // ตรวจสอบชื่อผู้ใช้
            if (username.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกชื่อผู้ใช้", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบอีเมล
            if (email.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกอีเมล", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "รูปแบบอีเมลไม่ถูกต้อง", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบรหัสผ่าน
            if (password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกรหัสผ่าน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบยืนยันรหัสผ่าน
            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "กรุณายืนยันรหัสผ่าน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "รหัสผ่านไม่ตรงกัน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ป้องกันการกดปุ่มซ้ำ
            createAcc?.isEnabled = false

            Toast.makeText(this, "กำลังสร้างบัญชี...", Toast.LENGTH_SHORT).show()

            mAuth!!.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (!task.isSuccessful) {

                        createAcc?.isEnabled = true

                        Toast.makeText(
                            this,
                            "สมัครสมาชิกไม่สำเร็จ: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.d(TAG, "Register Failed: ${task.exception?.message}")

                    } else {

                        val userId = mAuth?.currentUser?.uid ?: return@addOnCompleteListener

                        val userData = hashMapOf(
                            "username" to username,
                            "userId" to userId,
                            "email" to email,
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(userData)

                            .addOnSuccessListener {

                                Toast.makeText(
                                    this,
                                    "สร้างบัญชีสำเร็จ กรุณาเข้าสู่ระบบ",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Create account successfully")

                                // ออกจากระบบก่อน
                                FirebaseAuth.getInstance().signOut()

                                // กลับหน้า Login
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                                startActivity(intent)
                                finish()
                            }

                            .addOnFailureListener { e ->

                                createAcc?.isEnabled = true

                                Toast.makeText(
                                    this,
                                    "บันทึกข้อมูลผู้ใช้ไม่สำเร็จ: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                                Log.d(TAG, "Save user failed: ${e.message}")
                            }
                    }
                }
        }

        backR?.setOnClickListener {
            onBackPressed()
        }

        rSignin?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun init() {
        regisEmail = findViewById(R.id.redt_email)
        regisUsername = findViewById(R.id.redt_username)
        regisPass = findViewById(R.id.redt_pass)
        regisCPass = findViewById(R.id.redt_cPass)
        createAcc = findViewById(R.id.rbtn_acc)
        rSignin = findViewById(R.id.txt_signin)
        backR = findViewById(R.id.rimg_back)
    }
}