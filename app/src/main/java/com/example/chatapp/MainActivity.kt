package com.example.chatapp

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize Binding First
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // 2. Then enable edge-to-edge
        enableEdgeToEdge()

        mBinding.signin.setOnClickListener { signIn() }

        mBinding.signup.setOnClickListener { createAccount() }

        mBinding.textViewRegister.setOnClickListener {
            showNextAnimation()
        }

        mBinding.textViewSignin.setOnClickListener {
            showPreviousAnimation()
        }
        mBinding.textViewGotoOrofile.setOnClickListener {
            showNextAnimation()
        }
        mBinding.textViewSignup.setOnClickListener {
            showPreviousAnimation()
        }
    }

    private fun signIn() {
        val email = mBinding.signinInputEmail.editText?.text.toString().trim()
        val password = mBinding.signinInputPassword.editText?.text.toString().trim()
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


}
