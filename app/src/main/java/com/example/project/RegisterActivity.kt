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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    var mAuth: FirebaseAuth? = null
    private val TAG: String = "Register Activity"
    var regisEmail: EditText? = null
    var regisUsername: EditText? = null
    var regisPass: EditText? = null
    var regisCPass: EditText? = null
    var createAcc : Button? = null
    var rSignin : TextView? = null
    var backR : ImageView? = null

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
        if (mAuth!!.currentUser != null) {
            startActivity(Intent(this@RegisterActivity, ProfileActivity::class.java))
            finish()
        }
        createAcc?.setOnClickListener {
            val email = regisEmail?.text.toString().trim { it <= ' ' }
            val username = regisUsername?.text.toString().trim { it <= ' ' }
            val password = regisPass?.text.toString().trim { it <= ' ' }

            if (username.isEmpty()) {
                Toast.makeText(this,"Please enter your username.", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Username was empty!")
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(this,"Please enter your email address.", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Email was empty!")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this,"Please enter your password.",Toast.LENGTH_LONG).show()
                Log.d(TAG, "Password was empty!")
                return@setOnClickListener
            }

            mAuth!!.createUserWithEmailAndPassword(email,
                password).addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    if (password.length < 6) { // ตรวจสอบความยาวของ password
                        Toast.makeText(this,"Password too short! Please enter minimum 6 characters.",Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Enter password less than 6 characters.")
                    } else {
                        Toast.makeText(this,"Authentication Failed: " + task.exception!!.message,Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Authentication Failed: " + task.exception!!.message)
                    }
                } else {
                    val userId = mAuth?.currentUser?.uid ?: return@addOnCompleteListener
                    val userData = hashMapOf(
                        "username" to username,
                        "userId" to userId,
                        "email" to email,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this,"Create account successfully!",Toast.LENGTH_LONG).show()
                            Log.d(TAG, "Create account and user profile successfully!")
                            startActivity(Intent(this@RegisterActivity, ProfileActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this,"Save user profile failed: ${e.message}",Toast.LENGTH_LONG).show()
                            Log.d(TAG, "Save user profile failed: ${e.message}")
                        }
                }
            }
        }

        backR?.setOnClickListener { onBackPressed() }

        rSignin?.setOnClickListener {
            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java)) }
    }

    fun init(){
        regisEmail = findViewById(R.id.redt_email)
        regisUsername = findViewById(R.id.redt_username)
        regisPass = findViewById(R.id.redt_pass)
        regisCPass = findViewById(R.id.redt_cPass)
        createAcc = findViewById(R.id.rbtn_acc)
        rSignin = findViewById(R.id.txt_signin)
        backR = findViewById(R.id.rimg_back)
    }
}
