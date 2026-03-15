package com.example.chatapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.adaptors.MessageAdapter
import com.example.chatapp.databinding.ActivityChatBinding
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import java.text.SimpleDateFormat
import java.util.Locale

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val userRef = db.collection("users")
    private val messagesRef = db.collection("messages")
    
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<ChatMessage>
    private lateinit var messageIds: MutableList<String>
    private val userMap = mutableMapOf<String, User>()
    private lateinit var currentUser: User
    
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private var messagesRegistration: ListenerRegistration? = null
    private var usersRegistration: ListenerRegistration? = null
    private var typingRegistration: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        setSupportActionBar(binding.toolbar)

        initRecyclerView()
        getCurrentUser()
        listenToUsers()
        listenToTypingStatus()
        
        binding.sendButton.setOnClickListener {
            insertMessage()
        }
        
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                uploadImageMessage(uri)
            }
        }
        
        binding.attachImageButton.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        setupTypingListener()
    }
    
    override fun onStart() {
        super.onStart()
        updateStatus("Online")
        listenToMessages()
    }

    override fun onStop() {
        super.onStop()
        updateStatus("Offline")
        messagesRegistration?.remove()
        usersRegistration?.remove()
        typingRegistration?.remove()
    }

    private fun updateStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        val map = mutableMapOf<String, Any>()
        map["status"] = status
        if (status == "Offline") {
            map["lastSeen"] = System.currentTimeMillis()
        }
        userRef.document(uid).update(map)
    }

    private fun setupTypingListener() {
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setTypingStatus(s?.isNotEmpty() == true)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setTypingStatus(isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("typing").document(uid).set(mapOf("isTyping" to isTyping))
    }

    private fun listenToTypingStatus() {
        typingRegistration = db.collection("typing").addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            val typingUsers = mutableListOf<String>()
            value?.documents?.forEach { doc ->
                if (doc.id != auth.currentUser?.uid && doc.getBoolean("isTyping") == true) {
                    val user = userMap[doc.id]
                    user?.let { typingUsers.add(it.name) }
                }
            }
            if (typingUsers.isNotEmpty()) {
                binding.tvTypingStatus.visibility = View.VISIBLE
                binding.tvTypingStatus.text = "${typingUsers.joinToString(", ")} is typing..."
            } else {
                binding.tvTypingStatus.visibility = View.GONE
            }
        }
    }

    private fun listenToMessages() {
        messagesRegistration?.remove()
        messageList.clear()
        messageIds.clear()
        messageAdapter.notifyDataSetChanged()

        messagesRegistration = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            value?.let {
                for (documentChange in it.documentChanges) {
                    val docId = documentChange.document.id
                    when (documentChange.type) {
                        DocumentChange.Type.ADDED -> {
                            if (!messageIds.contains(docId)) {
                                val chatMessage = documentChange.document.toObject(ChatMessage::class.java)
                                messageList.add(chatMessage)
                                messageIds.add(docId)
                                messageAdapter.notifyItemInserted(messageList.size - 1)
                                binding.messageRecyclerView.smoothScrollToPosition(messageList.size - 1)
                                
                                // Mark as read if received
                                if (chatMessage.user?.uid != auth.currentUser?.uid && chatMessage.status < 2) {
                                    messagesRef.document(docId).update("status", 2)
                                }
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val index = messageIds.indexOf(docId)
                            if (index != -1) {
                                val chatMessage = documentChange.document.toObject(ChatMessage::class.java)
                                messageList[index] = chatMessage
                                messageAdapter.notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = messageIds.indexOf(docId)
                            if (index != -1) {
                                messageList.removeAt(index)
                                messageIds.removeAt(index)
                                messageAdapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclerView() {
        messageList = mutableListOf()
        messageIds = mutableListOf()
        messageAdapter = MessageAdapter(
            this, 
            messageList, 
            messageIds, 
            userMap,
            onProfileClick = { user ->
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", user.uid)
                intent.putExtra("name", user.name)
                intent.putExtra("email", user.email)
                intent.putExtra("image", user.profileImage)
                startActivity(intent)
            },
            onOwnProfileClick = {
                if (::currentUser.isInitialized) {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("uid", currentUser.uid)
                    intent.putExtra("name", currentUser.name)
                    intent.putExtra("email", currentUser.email)
                    intent.putExtra("image", currentUser.profileImage)
                    startActivity(intent)
                }
            },
            onDeleteClick = { messageId ->
                deleteMessage(messageId)
            }
        )
        binding.messageRecyclerView.adapter = messageAdapter
        binding.messageRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.messageRecyclerView.setHasFixedSize(true)
    }

    private fun listenToUsers() {
        usersRegistration = userRef.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener
            
            value?.let {
                val newUserMap = mutableMapOf<String, User>()
                for (doc in it.documents) {
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        newUserMap[user.uid] = user
                    }
                }
                userMap.clear()
                userMap.putAll(newUserMap)
                messageAdapter.updateUsers(userMap)
                
                // Update online status label for the "other" person
                val myUid = auth.currentUser?.uid
                val otherUser = newUserMap.values.find { it.uid != myUid }
                if (otherUser != null) {
                    binding.tvOnlineStatus.visibility = View.VISIBLE
                    if (otherUser.status == "Online") {
                        binding.tvOnlineStatus.text = "Online"
                    } else {
                        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                        binding.tvOnlineStatus.text = "Last seen: ${sdf.format(java.util.Date(otherUser.lastSeen))}"
                    }
                }

                if (myUid != null && newUserMap.containsKey(myUid)) {
                    currentUser = newUserMap[myUid]!!
                }
            }
        }
    }

    private fun deleteMessage(messageId: String) {
        messagesRef.document(messageId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentUser() {
        val currentUid = auth.currentUser?.uid
        if (currentUid != null) {
            userRef.document(currentUid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        currentUser = document.toObject(User::class.java)!!
                    }
                }
        }
    }

    private fun insertMessage() {
        val text = binding.messageEditText.text.toString().trim()
        if (text.isNotEmpty()) {
            if (::currentUser.isInitialized) {
                binding.sendButton.isEnabled = false
                val chatMessage = ChatMessage(currentUser, text, null, status = 1)
                messagesRef.document().set(chatMessage)
                    .addOnSuccessListener {
                        binding.messageEditText.setText("")
                        binding.sendButton.isEnabled = true
                    }
                    .addOnFailureListener {
                        binding.sendButton.isEnabled = true
                    }
            }
        }
    }
    
    private fun uploadImageMessage(uri: Uri) {
        if (!::currentUser.isInitialized) return
        
        binding.uploadingLayout.visibility = View.VISIBLE
        binding.uploadingImageView.setImageURI(uri) 
        binding.attachImageButton.isEnabled = false
        
        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetWidth = 600
                val targetHeight = (targetWidth / ratio).toInt()
                val scaledBitmap = bitmap.scale(targetWidth, targetHeight, false)
                
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                
                runOnUiThread {
                    val chatMessage = ChatMessage(currentUser, "", base64Image, status = 1)
                    messagesRef.document().set(chatMessage)
                        .addOnSuccessListener {
                            binding.uploadingLayout.visibility = View.GONE
                            binding.attachImageButton.isEnabled = true
                        }
                        .addOnFailureListener {
                            binding.uploadingLayout.visibility = View.GONE
                            binding.attachImageButton.isEnabled = true
                        }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    binding.uploadingLayout.visibility = View.GONE
                    binding.attachImageButton.isEnabled = true
                }
            }
        }.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_sign_out -> {
                updateStatus("Offline")
                auth.signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}