package com.aacheck.app

import android.app.Application
import com.aacheck.app.core.AppContainer

class AaCheckApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
