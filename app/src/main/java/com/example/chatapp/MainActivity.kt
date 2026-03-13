package com.example.chatapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding
import com.example.chatapp.model.User
import com.example.chatapp.ui.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private var imageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        enableEdgeToEdge()

        // 1. Photo Picker Setup
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                imageUri = uri
                mBinding.profileImage.setImageURI(uri)
                Log.d("DEBUG_APP", "Image Selected: $uri")
            }
        }

        // 2. Profile Image Click
        mBinding.profileImage.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 3. Auth Click Listeners
        mBinding.signup.setOnClickListener {
            Log.d("DEBUG_APP", "Signup button clicked")
            createAccount()
        }

        mBinding.signIn.setOnClickListener { signIn() }

        // 4. ViewFlipper Navigation (Animations)
        mBinding.textViewRegister.setOnClickListener { showNextAnimation() }
        mBinding.textViewGotoProfile.setOnClickListener { showNextAnimation() }

        mBinding.textViewSignIn.setOnClickListener { showPreviousAnimation() }
        mBinding.textViewSignup.setOnClickListener { showPreviousAnimation() }
    }

    private fun createAccount() {
        val email = mBinding.signupInputEmail.editText?.text.toString().trim()
        val password = mBinding.signupInputPassword.editText?.text.toString().trim()
        val username = mBinding.signupInputUsername.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent double clicks
        mBinding.signup.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""
                    Log.d("DEBUG_APP", "Auth success: $uid")

                    // Convert image and Save to Firestore
                    val base64Image = encodeImageToBase64(imageUri)
                    saveUserToFirestore(uid, username, base64Image)
                } else {
                    mBinding.signup.isEnabled = true
                    Log.e("DEBUG_APP", "Auth Failed", task.exception)
                    Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun encodeImageToBase64(uri: Uri?): String {
        if (uri == null) return ""
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Resize to 150x150 to keep the string small for Firestore
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 150, 150, false)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)

            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("DEBUG_APP", "Image Encoding Error", e)
            ""
        }
    }

    private fun saveUserToFirestore(uid: String, username: String, base64Image: String) {
        val user = User(username, base64Image, uid)

        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
                sendUserToNextActivity()
            }
            .addOnFailureListener { e ->
                mBinding.signup.isEnabled = true
                Log.e("DEBUG_APP", "Firestore Error", e)
                Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun signIn() {
        val email = mBinding.signInInputEmail.editText?.text.toString().trim()
        val password = mBinding.signInInputPassword.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) return

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) sendUserToNextActivity()
        }
    }

    private fun showNextAnimation() {
        mBinding.viewFlipper.setInAnimation(this, R.anim.slide_in_left)
        mBinding.viewFlipper.setOutAnimation(this, R.anim.slide_out_right)
        mBinding.viewFlipper.showNext()
    }

    private fun showPreviousAnimation() {
        mBinding.viewFlipper.setInAnimation(this, R.anim.slide_in_right)
        mBinding.viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
        mBinding.viewFlipper.showPrevious()
    }

    private fun sendUserToNextActivity() {
        startActivity(Intent(this, ChatActivity::class.java))
        finish()
    }
}