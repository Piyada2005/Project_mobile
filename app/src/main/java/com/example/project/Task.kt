package com.example.project

import com.google.firebase.Timestamp

data class Task(
    var title: String? = "",
    var description: String? = "",
    var category: String? = "",
    var dueDate: String? = "",
    var time: String? = "",
    var notify: String? = "",
    var repeat: String? = "",
    var note: String? = "",
    var userId: String? = "",
    var createdAt: Timestamp? = null,
    var isStarred: Boolean = false,
    var id: String = ""
)