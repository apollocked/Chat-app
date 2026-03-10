package com.example.chatapp

import android.databinding.DataBindingUtil
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.signin.setOnClickListener {

        }
        mBinding.signup.setOnClickListener {

        }
        mBinding.textVeiwSignin.setOnClickListener {
            mBinding.viewFlipper.showPrevious()
        }
        mBinding.textVeiwRegister.setOnClickListener {
            mBinding.viewFlipper.showNext()
        }


    }
}