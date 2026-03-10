package com.example.chatapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.chatapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mBinding.signin.setOnClickListener {
            // Handle sign in
        }

        mBinding.signup.setOnClickListener {
            // Handle sign up
        }

        mBinding.textVeiwRegister.setOnClickListener {
            mBinding.viewFlipper.setInAnimation(this@MainActivity, R.anim.slide_in_left)
            mBinding.viewFlipper.setOutAnimation(this@MainActivity, R.anim.slide_out_righ)
            mBinding.viewFlipper.showNext()
        }

        mBinding.textVeiwSignin.setOnClickListener {
            mBinding.viewFlipper.setInAnimation(this@MainActivity, R.anim.slide_in_right)
            mBinding.viewFlipper.setOutAnimation(this@MainActivity, R.anim.slide_out_left)
            mBinding.viewFlipper.showPrevious()
        }
    }
}
