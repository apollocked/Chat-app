package com.example.chatapp.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.R
import com.example.chatapp.adaptors.MessageAdapter
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val userRef = db.collection("users")
    private val messagesRef = db.collection("messages")
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<ChatMessage>
    private lateinit var currentUser: User



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sendButton = findViewById(R.id.sendButton)
        messageEditText = findViewById(R.id.messageEditText)

        initRecyclerView()
        getCurrentUser()



    }
    private fun initRecyclerView() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageList = mutableListOf()
        messageAdapter = MessageAdapter(this, messageList)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.setHasFixedSize(true)

    }
    private fun getCurrentUser(){
        userRef.whereEqualTo("id", FirebaseAuth.getInstance().currentUser?.uid)
            .get().addOnSuccessListener {
                for(snapshot in it){
                    currentUser = snapshot.toObject(User::class.java)
                }
                }

    }
}