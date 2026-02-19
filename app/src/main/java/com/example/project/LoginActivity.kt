package com.example.project

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    var mAuth: FirebaseAuth? = null
    private val TAG: String = "Login Activity"
    var loginBtn: Button? = null
    var userEmail: EditText? = null
    var userPass: EditText? = null
    var createUser: TextView? = null
    var backLogin: ImageView? = null

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

        if (mAuth!!.currentUser != null) {
            startActivity(Intent(this@LoginActivity, ProfileActivity::class.java))
            finish()
        }

        loginBtn?.setOnClickListener {
            val email = userEmail?.text.toString().trim { it <= ' ' }
            val password = userPass?.text.toString().trim { it <= ' ' }

            if (email.isEmpty()) {
                Toast.makeText(this, "โปรดป้อนที่อยู่อีเมลของคุณ", Toast.LENGTH_LONG).show()
                Log.d(TAG, "อีเมลว่างเปล่า!")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกรหัสผ่านของคุณ", Toast.LENGTH_LONG).show()
                Log.d(TAG, "รหัสผ่านว่างเปล่า!")
                return@setOnClickListener
            }

            mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    if (password.length < 6) {

                        userPass?.error = "โปรดตรวจสอบรหัสผ่านของคุณ รหัสผ่านต้องมีอย่างน้อย 6 ตัวอักษร"
                        Log.d(TAG, "กรุณาป้อนรหัสผ่านที่มีความยาวไม่เกิน 6 ตัวอักษร")
                    } else {
                        Toast.makeText(this, "การตรวจสอบสิทธิ์ล้มเหลว: " + task.exception!!.message, Toast.LENGTH_LONG).show()
                        Log.d(TAG, "การตรวจสอบสิทธิ์ล้มเหลว: " + task.exception!!.message)
                    }

                } else {
                    Toast.makeText(this, "เข้าสู่ระบบสำเร็จ!", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "เข้าสู่ระบบสำเร็จ!")
                    startActivity(Intent(this@LoginActivity, ProfileActivity::class.java))
                    finish()
                }
            }
        }

        createUser?.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java)) }

        backLogin?.setOnClickListener { onBackPressed() }
    }

    fun init(){
        userEmail = findViewById(R.id.inp_email)
        userPass = findViewById(R.id.inp_pass)
        loginBtn = findViewById(R.id.btn_signin)
        createUser = findViewById(R.id.txt_create)
        backLogin = findViewById(R.id.img_back)
    }
}