package com.example.motorapp

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class MotorApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val db = FirebaseDatabase.getInstance("https://motor-3ad5a-default-rtdb.asia-southeast1.firebasedatabase.app")
        db.setPersistenceEnabled(true)
    }
}