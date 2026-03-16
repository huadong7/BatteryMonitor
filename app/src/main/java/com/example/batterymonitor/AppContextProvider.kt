package com.example.batterymonitor

import android.content.Context

object AppContextProvider {
    private lateinit var applicationContext: Context
    
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
    
    fun getContext(): Context {
        return applicationContext
    }
}