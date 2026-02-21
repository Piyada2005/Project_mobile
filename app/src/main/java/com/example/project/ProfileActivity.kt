package com.example.project

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import android.widget.*

class ProfileActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ===== Switch การแจ้งเตือน =====
        val notificationSwitch = findViewById<Switch>(R.id.switchNotification)

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "เปิดการแจ้งเตือน", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "ปิดการแจ้งเตือน", Toast.LENGTH_SHORT).show()
            }
        }

        // ===== ปุ่มออกจากระบบ =====
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            Toast.makeText(this, "ออกจากระบบ", Toast.LENGTH_SHORT).show()

            // ถ้าจะกลับหน้า Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}