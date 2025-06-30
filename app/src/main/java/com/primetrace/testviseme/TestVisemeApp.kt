package com.primetrace.testviseme

import android.app.Application
import android.util.Log

class TestVisemeApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("TestVisemeApp", "Application created")
    }
}
