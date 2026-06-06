package com.rpm.app

import android.app.Application
import com.rpm.app.fcm.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RpmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
