package com.ljx.example.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ljx.example.IMyAidlInterface
import f.KtTopTest1
import com.ljx.example.R
import f.K
import f.i
import f.j
import f.k
import f.ktTopFun
import f.m
import f.toB

class MainActivity : AppCompatActivity() {

    val list = mutableListOf<IMyAidlInterface>()

    private val s = "test field"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Toast.makeText(this, K.h5, Toast.LENGTH_LONG).show()
        ktTopFun()
        val i = i
        val j = j
        val k = k
        val m = m
        m.toB
        KtTopTest1()
        test(this)
    }

    fun test(main: com.ljx.example.activity.MainActivity) {
        Log.d("LJX", main::class.java.name)
    }
}