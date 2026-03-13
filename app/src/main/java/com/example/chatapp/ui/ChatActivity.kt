package com.example.chatapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale

class ChatActivity : AppCompatActivity() {
    private val db = FirebaseFirestore.getInstance()
    private val userRef = db.collection("users")
    private val messagesRef = db.collection("messages")
    
    private lateinit var sendButton: Button
    private lateinit var attachImageButton: ImageButton
    private lateinit var messageEditText: EditText
    private lateinit var messageRecyclerView: RecyclerView
    private lateinit var uploadingLayout: View
    private lateinit var uploadingImageView: ImageView
    private lateinit var sendingProgressBar: ProgressBar

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: MutableList<ChatMessage>
    private lateinit var messageIds: MutableList<String>
    private val userMap = mutableMapOf<String, User>()
    private lateinit var currentUser: User
    
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private var messagesRegistration: ListenerRegistration? = null
    private var usersRegistration: ListenerRegistration? = null


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
        attachImageButton = findViewById(R.id.attachImageButton)
        messageEditText = findViewById(R.id.messageEditText)
        uploadingLayout = findViewById(R.id.uploadingLayout)
        uploadingImageView = findViewById(R.id.uploadingImageView)
        sendingProgressBar = findViewById(R.id.sendingProgressBar)

        initRecyclerView()
        getCurrentUser()
        listenToUsers()
        
        sendButton.setOnClickListener {
            insertMessage()
        }
        
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                uploadImageMessage(uri)
            }
        }
        
        attachImageButton.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

    }
    
    override fun onStart() {
        super.onStart()
        
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
                                messageRecyclerView.smoothScrollToPosition(messageList.size - 1)
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

    override fun onStop() {
        super.onStop()
        messagesRegistration?.remove()
        messagesRegistration = null
        usersRegistration?.remove()
        usersRegistration = null
    }

    private fun initRecyclerView() {
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
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
        messageRecyclerView.adapter = messageAdapter
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.setHasFixedSize(true)
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
                
                // Keep currentUser updated
                val myUid = FirebaseAuth.getInstance().currentUser?.uid
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
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentUser() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
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
        val text = messageEditText.text.toString().trim()
        if (text.isNotEmpty()) {
            if (::currentUser.isInitialized) {
                sendButton.isEnabled = false
                val chatMessage = ChatMessage(currentUser, text, null)
                messagesRef.document().set(chatMessage)
                    .addOnSuccessListener {
                        messageEditText.setText("")
                        sendButton.isEnabled = true
                    }
                    .addOnFailureListener {
                        sendButton.isEnabled = true
                    }
            }
        }
    }
    
    private fun uploadImageMessage(uri: Uri) {
        if (!::currentUser.isInitialized) return
        
        uploadingLayout.visibility = View.VISIBLE
        uploadingImageView.setImageURI(uri) 
        attachImageButton.isEnabled = false
        
        val docRef = messagesRef.document()
        val pendingMessage = ChatMessage(currentUser, "", "PENDING")
        
        docRef.set(pendingMessage)
        
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
                    docRef.update("messageImage", base64Image)
                        .addOnSuccessListener {
                            uploadingLayout.visibility = View.GONE
                            attachImageButton.isEnabled = true
                        }
                        .addOnFailureListener {
                            uploadingLayout.visibility = View.GONE
                            attachImageButton.isEnabled = true
                        }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    uploadingLayout.visibility = View.GONE
                    attachImageButton.isEnabled = true
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
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}