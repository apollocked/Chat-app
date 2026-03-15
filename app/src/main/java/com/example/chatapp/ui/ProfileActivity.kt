package com.example.chatapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatapp.MainActivity
import com.example.chatapp.R
import com.example.chatapp.databinding.ActivityProfileBinding
import com.example.chatapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var pickProfileImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val db = FirebaseFirestore.getInstance()
    
    private var pendingBase64Image: String? = null
    private var isOwnProfile = false
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.profileRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { handleBackAction() }

        uid = intent.getStringExtra("uid")
        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        val imageBase64 = intent.getStringExtra("image")

        binding.profileNameLarge.text = name ?: "Unknown"
        binding.profileEmailLarge.text = email ?: ""

        displayImage(imageBase64)
        listenToUserStatus()

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        isOwnProfile = (uid == currentUid)
        if (isOwnProfile) {
            binding.btnChangeProfilePicture.visibility = View.VISIBLE
        }

        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                processSelectedImage(uri)
            }
        }

        binding.btnChangeProfilePicture.setOnClickListener {
            pickProfileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSaveProfile.setOnClickListener {
            if (pendingBase64Image != null && uid != null) {
                saveProfilePicture(uid!!)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
    }

    private fun listenToUserStatus() {
        if (uid == null) return
        
        db.collection("users").document(uid!!).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            
            val user = snapshot.toObject(User::class.java)
            user?.let {
                if (it.status == "Online") {
                    binding.profileStatus.text = "Online"
                    binding.profileStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                } else {
                    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    binding.profileStatus.text = "Last seen: ${sdf.format(java.util.Date(it.lastSeen))}"
                    binding.profileStatus.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
                }
            }
        }
    }

    private fun handleBackAction() {
        if (pendingBase64Image != null) {
            AlertDialog.Builder(this)
                .setTitle("Discard Changes?")
                .setMessage("You have unsaved changes. Are you sure you want to go back?")
                .setPositiveButton("Discard") { _, _ -> finish() }
                .setNegativeButton("Keep Editing", null)
                .show()
        } else {
            finish()
        }
    }

    private fun displayImage(base64: String?) {
        if (!base64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.profileImageLarge.setImageBitmap(bitmap)
            } catch (e: Exception) {
                binding.profileImageLarge.setImageResource(R.drawable.profile)
            }
        } else {
            binding.profileImageLarge.setImageResource(R.drawable.profile)
        }
    }

    private fun processSelectedImage(uri: Uri) {
        Thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val scaledBitmap = bitmap.scale(150, 150, false)
                
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                
                runOnUiThread {
                    pendingBase64Image = base64Image
                    displayImage(base64Image)
                    binding.btnSaveProfile.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveProfilePicture(uid: String) {
        binding.btnSaveProfile.isEnabled = false
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        
        db.collection("users").document(uid).update("profileImage", pendingBase64Image)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                binding.btnSaveProfile.isEnabled = true
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}