package com.android.myapplication

import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tencent.mmkv.MMKV

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        MMKV.initialize(this)
        putSomeKey()
        getInfo()
    }

    fun putSomeKey(){
        MMKV.defaultMMKV().putBoolean("HUNG", true)
        MMKV.defaultMMKV().encodeListParcelable("HUNG2", listOf(Car(name = "HUng", age = 32, id = 1)))
        MMKV.defaultMMKV().encode("HUNG3", Car(name = "HUng", age = 32, id = 1))

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            applicationContext
        )

        val prefsOne = getSharedPreferences("countPrefOne", MODE_PRIVATE)
        val prefsTwo = getSharedPreferences("countPrefTwo", MODE_PRIVATE)

        sharedPreferences.edit().putString("testOne", "one").commit()
        sharedPreferences.edit().putInt("testTwo", 2).commit()
        sharedPreferences.edit().putLong("testThree", 100000L).commit()
        sharedPreferences.edit().putFloat("testFour", 3.01f).commit()
        sharedPreferences.edit().putBoolean("testFive", true).commit()
    }

    fun getInfo(){
        Log.e(TAG, "getInfo: ${MMKV.defaultMMKV().allKeys()?.map { it }}")
        Log.e(TAG, "getInfo: ${MMKV.defaultMMKV().decodeParcelable("HUNG3", Car::class.java)}")
        val arr = MMKV.defaultMMKV().decodeBytes("HUNG3") ?: ByteArray(0)
        Log.e(TAG, "getInfo2: ${arr.toString()}" )


    }
}