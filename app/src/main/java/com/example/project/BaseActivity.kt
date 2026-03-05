package com.example.project

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomBar() {

        val menuTask = findViewById<LinearLayout>(R.id.menu_tasks)
        val menuCalendar = findViewById<LinearLayout>(R.id.menu_calendar)
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)

        fun resetMenu() {
            menuTask.setBackgroundResource(0)
            menuCalendar.setBackgroundResource(0)
            menuProfile.setBackgroundResource(0)
        }

        menuTask?.setOnClickListener {

            resetMenu()
            menuTask.setBackgroundResource(R.drawable.segmented_selected_bg)

            if (this !is TaskActivity) {
                startActivity(Intent(this, TaskActivity::class.java))
                overridePendingTransition(0,0)
                finish()
            }
        }

        menuCalendar?.setOnClickListener {

            resetMenu()
            menuCalendar.setBackgroundResource(R.drawable.segmented_selected_bg)

            if (this !is CalenderActivity) {
                startActivity(Intent(this, CalenderActivity::class.java))
                overridePendingTransition(0,0)
                finish()
            }
        }

        menuProfile?.setOnClickListener {

            resetMenu()
            menuProfile.setBackgroundResource(R.drawable.segmented_selected_bg)

            if (this !is ProfileActivity) {
                startActivity(Intent(this, ProfileActivity::class.java))
                overridePendingTransition(0,0)
                finish()
            }
        }
    }
}