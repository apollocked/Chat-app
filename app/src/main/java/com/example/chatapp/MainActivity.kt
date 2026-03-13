package com.example.chatapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var getResult: ActivityResultLauncher<Intent>
    private val STORAGE_REQUEST_CODE = 415541


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Binding First
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // 2. Then enable edge-to-edge
        enableEdgeToEdge()

        mBinding.signIn.setOnClickListener { signIn() }

        mBinding.signup.setOnClickListener { createAccount() }

        mBinding.textViewRegister.setOnClickListener {
            showNextAnimation()
        }

        mBinding.textViewSignIn.setOnClickListener {
            showPreviousAnimation()
        }
        mBinding.textViewGotoProfile.setOnClickListener {
            showNextAnimation()
        }
        mBinding.textViewSignup.setOnClickListener {
            showPreviousAnimation()
        }
        mBinding.profileImage.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this@MainActivity,android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
                requestPermissions()}else{
              uploadImage()
            }

        }
        getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {if (it.resultCode == RESULT_OK) {

        mBinding.profileImage.setImageURI(it.data?.data)
        }

        }
    }

    private fun signIn() {
        val email = mBinding.signInInputEmail.editText?.text.toString().trim()
        val password = mBinding.signInInputPassword.editText?.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_LONG).show()
            return
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign in successful", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this,
                        "Could not sign in\n${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun createAccount() {
        val email = mBinding.signupInputEmail.editText?.text.toString().trim()
        val password = mBinding.signupInputPassword.editText?.text.toString().trim()
        val confirmPassword = mBinding.signupInputConfirmPassword.editText?.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_LONG).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_LONG).show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_LONG).show()
                } else {
                    val rootView = findViewById<View>(android.R.id.content)
                    com.google.android.material.snackbar.Snackbar.make(
                        rootView,
                        task.exception?.message ?: "Authentication Failed",
                        com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
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
private fun getImage() {
val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    getResult.launch(intent)
}
private fun requestPermissions() {
if (ActivityCompat.shouldShowRequestPermissionRationale(
        this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
AlertDialog.Builder(this@MainActivity)
    .setPositiveButton("yes"){_,_->
        ActivityCompat.requestPermissions(this, arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE),STORAGE_REQUEST_CODE
        )}.setNegativeButton("no") { dialog, _ ->

            dialog.dismiss()
    }.setTitle("Permission is required")
    .setMessage("Permission is required to select image")
    .create().show()

    }else{
        ActivityCompat.requestPermissions(this, arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE),STORAGE_REQUEST_CODE)
    }
}
private fun uploadImage() {

}
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
        ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getImage()
            } else {
                Toast.makeText(this@MainActivity, "Permission denied",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

}
