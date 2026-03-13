package com.example.chatapp.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val senderId: String = "",
    val senderName: String = "",
    val messageText: String = "",
    val senderImage: String = "", // This holds the Base64 string
    val timestamp: Timestamp? = null
)