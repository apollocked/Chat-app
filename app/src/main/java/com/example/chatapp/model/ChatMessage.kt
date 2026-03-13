package com.example.chatapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val user: User,
    val messageText: String ,
    @ServerTimestamp
    val timestamp: Timestamp?
){
    constructor():this(User(),"",null)

}