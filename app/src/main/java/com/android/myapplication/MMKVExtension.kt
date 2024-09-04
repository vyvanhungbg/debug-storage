package com.android.myapplication

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.tencent.mmkv.MMKV
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/***
Created by HungVV
Created at 11:55/26-01-2024
 ***/

const val TAG_MMKV_EXTENSION = "MMKVExtension"

// Parcelable

fun <T : Parcelable?> MMKV.encodeListParcelable(key: String, value: List<T>) {
    val byteArray = convertListParcelableToByteArray(value)
    this.encode(key, byteArray)
}

inline fun <reified T : Parcelable?> MMKV.decodeListParcelable(
    key: String,
    defaultValue: List<T>? = null
): List<T>? {
    val byteArray = this.decodeBytes(key)
    return convertByteArrayToListParcelable(byteArray, defaultValue)
}


private fun <T : Parcelable?> convertListParcelableToByteArray(list: List<T>): ByteArray {
    val parcel = Parcel.obtain()
    return try {
        parcel.writeList(list)
        parcel.marshall()
    } finally {
        parcel.recycle()
    }
}

inline fun <reified T : Parcelable?> convertByteArrayToListParcelable(
    byteArray: ByteArray?,
    defaultValue: List<T>? = null
): List<T>? {

    if (byteArray == null) {
        return defaultValue
    }
    val parcel = Parcel.obtain()
    return try {
        parcel.unmarshall(byteArray, 0, byteArray.size)
        parcel.setDataPosition(0)
        val list: ArrayList<T> = ArrayList()
        parcel.readList(list, T::class.java.classLoader)
        list
    } catch (e: Exception) {
        Log.e(TAG_MMKV_EXTENSION, "convertByteArrayToListSerializable: ${e.toString()}", )
        defaultValue
    } finally {
        parcel.recycle()
    }

}


// Serializable

fun <T : Serializable?> MMKV.encodeListSerializable(key: String, value: List<T>) {
    val byteArray = convertListSerializableToByteArray(value)
    this.encode(key, byteArray)
}

inline fun <reified T : Serializable?> MMKV.decodeListSerializable(
    key: String,
    defaultValue: List<T>? = null
): List<T>? {
    val byteArray = this.decodeBytes(key)
    return convertByteArrayToListSerializable(byteArray, defaultValue)
}


private fun <T : Serializable?> convertListSerializableToByteArray(list: List<T>): ByteArray {
    ByteArrayOutputStream().use { byteArrayOutputStream ->
        ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
            objectOutputStream.writeObject(list)
        }
        return byteArrayOutputStream.toByteArray()
    }
}

inline fun <reified T : Serializable?> convertByteArrayToListSerializable(
    byteArray: ByteArray?,
    defaultValue: List<T>? = null
): List<T>? {
    return try {
        ByteArrayInputStream(byteArray).use { byteArrayInputStream ->
            ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
                objectInputStream.readObject() as List<T>
            }
        }
    } catch (e: Exception) {
        Log.e(TAG_MMKV_EXTENSION, "convertByteArrayToListSerializable: ${e.toString()}", )
        defaultValue
    }
}