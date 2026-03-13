package com.example.chatapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.adaptors.MessageAdapter
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
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
        sendButton.setOnClickListener {
            insertMessage()
        }

    }
    override fun onStart() {
        super.onStart()
        // Added orderBy to keep chat in chronological order
        messagesRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            value?.let {
                for (documentChange in it.documentChanges) {
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val chatMessage = documentChange.document.toObject(ChatMessage::class.java)
                        messageList.add(chatMessage)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        // Auto-scroll to bottom
                        messageRecyclerView.smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageList = mutableListOf()
        messageAdapter = MessageAdapter(this, messageList)
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.setHasFixedSize(true)

    }

    private fun getCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            // Get user by document ID (which is the UID)
            userRef.document(currentUid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(User::class.java)!!
                    } else {
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun insertMessage() {
        val text = messageEditText.text.toString().trim()
        if (text.isNotEmpty()) {
            if (::currentUser.isInitialized) {
                // Disable button to prevent double clicks
                sendButton.isEnabled = false
                
                val chatMessage = ChatMessage(currentUser, text, null)
                messagesRef.document().set(chatMessage)
                    .addOnSuccessListener {
                        messageEditText.setText("")
                        // Re-enable button after success
                        sendButton.isEnabled = true
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send: ${it.message}", Toast.LENGTH_SHORT).show()
                        // Re-enable button after failure
                        sendButton.isEnabled = true
                    }
            } else {
                Toast.makeText(this, "Still loading user data...", Toast.LENGTH_SHORT).show()
                // Retry loading user if it failed before
                getCurrentUser()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.item_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                Intent(this@ChatActivity, MainActivity::class.java).also {
                    startActivity(it)
                }
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    }
