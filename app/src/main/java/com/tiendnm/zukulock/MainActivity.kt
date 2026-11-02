package com.tiendnm.zukulock

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Launcher icon: when already authorized, turn the screen off straight away
 * (keeping the original "no UI" experience); otherwise forward to the
 * authorization screen instead of calling requestPermission from a Service and
 * failing silently.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ShizukuState.check() == ShizukuState.GRANTED) {
            startService(Intent(this, SleepService::class.java))
        } else {
            startActivity(Intent(this, GrantActivity::class.java))
        }
        finish()
    }
}
