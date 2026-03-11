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

class LoginActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null
    private val TAG: String = "LoginActivity"

    private var loginBtn: Button? = null
    private var userEmail: EditText? = null
    private var userPass: EditText? = null
    private var createUser: TextView? = null
    private var backLogin: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()

        mAuth = FirebaseAuth.getInstance()

        // ถ้ามีการ login อยู่แล้ว ให้เข้าแอปเลย
        if (mAuth!!.currentUser != null) {
            startActivity(Intent(this@LoginActivity, TaskActivity::class.java))
            finish()
        }

        loginBtn?.setOnClickListener {

            val email = userEmail?.text.toString().trim()
            val password = userPass?.text.toString().trim()

            // ตรวจสอบอีเมล
            if (email.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกอีเมล", Toast.LENGTH_LONG).show()
                Log.d(TAG, "อีเมลว่าง")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "รูปแบบอีเมลไม่ถูกต้อง", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบรหัสผ่าน
            if (password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกรหัสผ่าน", Toast.LENGTH_LONG).show()
                Log.d(TAG, "รหัสผ่านว่าง")
                return@setOnClickListener
            }

            if (password.length < 6) {
                userPass?.error = "รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
                return@setOnClickListener
            }

            loginBtn?.isEnabled = false

            Toast.makeText(this, "กำลังเข้าสู่ระบบ...", Toast.LENGTH_SHORT).show()

            mAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    loginBtn?.isEnabled = true

                    if (!task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "เข้าสู่ระบบไม่สำเร็จ: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.d(TAG, "Login Failed: ${task.exception?.message}")

                    } else {

                        Toast.makeText(this, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_LONG).show()

                        Log.d(TAG, "Login Success")

                        startActivity(Intent(this@LoginActivity, TaskActivity::class.java))
                        finish()
                    }
                }
        }

        // ไปหน้าสมัครสมาชิก
        createUser?.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        // ปุ่มย้อนกลับ
        backLogin?.setOnClickListener {
            onBackPressed()
        }
    }

    private fun init() {
        userEmail = findViewById(R.id.inp_email)
        userPass = findViewById(R.id.inp_pass)
        loginBtn = findViewById(R.id.btn_signin)
        createUser = findViewById(R.id.txt_create)
        backLogin = findViewById(R.id.img_back)
    }
}