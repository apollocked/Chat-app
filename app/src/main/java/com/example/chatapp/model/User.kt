package com.example.chatapp.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profileImage: String = "",
    val status: String = "Offline",
    val lastSeen: Long = 0L
)