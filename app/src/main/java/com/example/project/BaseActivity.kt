package com.example.project

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        // บังคับภาษาไทยทั้งแอป
        val locale = LocaleListCompat.forLanguageTags("th")
        AppCompatDelegate.setApplicationLocales(locale)

        super.onCreate(savedInstanceState)
    }

    protected fun setupBottomBar() {

        val menuTask = findViewById<LinearLayout>(R.id.menu_tasks)
        val menuCalendar = findViewById<LinearLayout>(R.id.menu_calendar)
        val menuProfile = findViewById<LinearLayout>(R.id.menu_profile)

        menuTask?.setOnClickListener {
            if (this !is TaskActivity) {
                startActivity(Intent(this, TaskActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }

        menuCalendar?.setOnClickListener {
            if (this !is CalenderActivity) {
                startActivity(Intent(this, CalenderActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
            }
        }

        menuProfile?.setOnClickListener {
            if (this !is ProfileActivity) {
                startActivity(Intent(this, ProfileActivity::class.java))
                overridePendingTransition(0, 0)

                finish()
            }
        }
    }
}
