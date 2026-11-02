package com.tiendnm.zukulock

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Splits "can a tile do its job" into three states. The original code only had
 * ready / not ready, so every failure looked like "tapped and nothing happened".
 *
 * Another key point: Shizuku's binder is *pushed* into the process by
 * ShizukuProvider when the process starts, which leaves a few hundred
 * milliseconds of dead air on a cold start. A tile started from an empty
 * process receives its click almost immediately, while pingBinder() is still
 * false. That is why the tiles only started working after opening the app once.
 * Hence awaitBinder(): wait a moment first, then get to work.
 */
object ShizukuState {

    private const val TAG = "ShizukuState"

    const val NOT_RUNNING = 0
    const val NO_PERMISSION = 1
    const val GRANTED = 2

    fun check(): Int = try {
        when {
            !Shizuku.pingBinder() -> NOT_RUNNING
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> GRANTED
            else -> NO_PERMISSION
        }
    } catch (t: Throwable) {
        NOT_RUNNING
    }

    /**
     * Reports back immediately when the state can be determined right away.
     * Otherwise (the binder has not arrived yet) a listener is registered: if it
     * arrives within [timeoutMs] the real state is reported, otherwise
     * NOT_RUNNING is. The callback is always delivered on the main thread.
     */
    fun awaitBinder(timeoutMs: Long = 1500L, callback: (Int) -> Unit) {
        val current = check()
        if (current != NOT_RUNNING) {
            callback(current)
            return
        }
        BinderWaiter(Handler(Looper.getMainLooper()), timeoutMs, callback).start()
    }

    private class BinderWaiter(
        private val main: Handler,
        private val timeoutMs: Long,
        private val callback: (Int) -> Unit
    ) : Shizuku.OnBinderReceivedListener {

        private var finished = false

        fun start() {
            try {
                Shizuku.addBinderReceivedListenerSticky(this)
            } catch (t: Throwable) {
                Log.w(TAG, "addBinderReceivedListenerSticky failed", t)
                finish(NOT_RUNNING)
                return
            }
            main.postDelayed({
                Log.w(TAG, "binder not received within ${timeoutMs}ms")
                finish(NOT_RUNNING)
            }, timeoutMs)
        }

        override fun onBinderReceived() {
            finish(check())
        }

        private fun finish(state: Int) {
            synchronized(this) {
                if (finished) return
                finished = true
            }
            try {
                Shizuku.removeBinderReceivedListener(this)
            } catch (t: Throwable) {
                // ignore
            }
            main.post { callback(state) }
        }
    }
}
