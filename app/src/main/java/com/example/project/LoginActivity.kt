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
                Toast.makeText(this, "Please enter your email address.", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Email was empty!")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter your password.", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Password was empty!")
                return@setOnClickListener
            }

            mAuth!!.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->

                if (!task.isSuccessful) {

                    if (password.length < 6) {

                        userPass?.error = "Please check your password.Password must have minimum 6 characters."
                        Log.d(TAG, "Enter password less than 6 characters.")
                    } else {
                        Toast.makeText(this, "Authentication Failed: " + task.exception!!.message, Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Authentication Failed: " + task.exception!!.message)
                    }

                } else {
                    Toast.makeText(this, "Sign in successfully!", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "Sign in successfully!")
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