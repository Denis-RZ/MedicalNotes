package com.medicalnotes.app

import android.app.Application

class MedicalNotesApplication : Application() {
    
    companion object {
        lateinit var instance: MedicalNotesApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
} 