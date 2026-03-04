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
    var link: String? = "",
    var imageUri: String? = "",

    var userId: String? = "",
    var createdAt: Timestamp? = null,
    @get:com.google.firebase.firestore.PropertyName("isStarred")
    @set:com.google.firebase.firestore.PropertyName("isStarred")
    var isStarred: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isFinished")
    @set:com.google.firebase.firestore.PropertyName("isFinished")
    var isFinished: Boolean = false,
    var id: String = ""
)
