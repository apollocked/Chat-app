package com.example.chatapp.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var pickProfileImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val db = FirebaseFirestore.getInstance()
    private lateinit var imageIv: ImageView
    private lateinit var btnSave: Button
    private var pendingBase64Image: String? = null
    private var isOwnProfile = false
    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { handleBackAction() }

        uid = intent.getStringExtra("uid")
        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        val imageBase64 = intent.getStringExtra("image")

        val nameTv: TextView = findViewById(R.id.profileNameLarge)
        val emailTv: TextView = findViewById(R.id.profileEmailLarge)
        imageIv = findViewById(R.id.profileImageLarge)
        val btnChange: Button = findViewById(R.id.btnChangeProfilePicture)
        btnSave = findViewById(R.id.btnSaveProfile)

        nameTv.text = name ?: "Unknown"
        emailTv.text = email ?: ""

        displayImage(imageBase64)

        // Show change button only if it's the current user's profile
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        isOwnProfile = (uid == currentUid)
        if (isOwnProfile) {
            btnChange.visibility = View.VISIBLE
        }

        pickProfileImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                processSelectedImage(uri)
            }
        }

        btnChange.setOnClickListener {
            pickProfileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSave.setOnClickListener {
            if (pendingBase64Image != null && uid != null) {
                saveProfilePicture(uid!!)
            }
        }

        // Handle system back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })
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
                imageIv.setImageBitmap(bitmap)
            } catch (e: Exception) {
                imageIv.setImageResource(R.drawable.profile)
            }
        } else {
            imageIv.setImageResource(R.drawable.profile)
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
                    btnSave.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun saveProfilePicture(uid: String) {
        btnSave.isEnabled = false
        Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        
        db.collection("users").document(uid).update("profileImage", pendingBase64Image)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                // Restart app to refresh everything
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                btnSave.isEnabled = true
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}