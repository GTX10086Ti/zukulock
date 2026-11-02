package com.tiendnm.zukulock

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SleepService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ShizukuState.check() == ShizukuState.GRANTED) {
            ShellRunner.run("input", "keyevent", "223")
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
