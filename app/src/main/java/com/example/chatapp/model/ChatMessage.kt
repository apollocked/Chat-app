package com.example.chatapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val user: User? = null,
    val messageText: String = "",
    val messageImage: String? = null, // This will now be a URL
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val status: Int = 0 // 0 = sending, 1 = sent, 2 = read
)