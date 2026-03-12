package com.example.chatapp

import android.os.Bundle
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

        mBinding.signup.setOnClickListener { creatAccount() }

        mBinding.textViewRegister.setOnClickListener {
            // FIXED TYPO: slide_out_right
            mBinding.viewFlipper.setInAnimation(this, R.anim.slide_in_left)
            mBinding.viewFlipper.setOutAnimation(this, R.anim.slide_out_righ)
            mBinding.viewFlipper.showNext()
        }

        mBinding.textViewSignin.setOnClickListener {
            mBinding.viewFlipper.setInAnimation(this, R.anim.slide_in_right)
            mBinding.viewFlipper.setOutAnimation(this, R.anim.slide_out_left)
            mBinding.viewFlipper.showPrevious()
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
            Toast.makeText(this, "Could not sign in\n${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
            }
    }
    private fun creatAccount() {
        val email = mBinding.signupInputEmail.editText?.text.toString().trim()
        val password = mBinding.signupInputPassword.editText?.text.toString().trim()

    }


}
