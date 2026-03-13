package com.example.chatapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val user: User? = null,
    val messageText: String = "",
    val messageImage: String? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)