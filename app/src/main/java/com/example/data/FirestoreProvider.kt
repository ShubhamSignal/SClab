package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreProvider {
    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: run {
                val dbId = context.getString(R.string.firestore_database_id)
                val instance = FirebaseFirestore.getInstance(dbId)
                firestoreInstance = instance
                instance
            }
        }
    }

    val auth: FirebaseAuth get() = Firebase.auth
}
