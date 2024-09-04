package com.android.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/***
Create by HungVV
Create at 15:29/03-09-2024
 ***/
private const val TAG = "Car"

@Parcelize
data class Car(
    val name: String,
    val id: Int,
    val age: Int,
) : Parcelable