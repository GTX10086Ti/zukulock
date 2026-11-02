package com.tiendnm.zukulock

import android.util.Log
import java.util.concurrent.Executors

/**
 * Runs a command through Shizuku.
 *
 * The order matters:
 *   1. Start the IPC (newProcess) synchronously on the calling thread, so the
 *      command is actually handed over to Shizuku;
 *   2. Only waitFor() goes to a background thread.
 *
 * Doing it the other way around (posting the whole flow to a thread pool) means
 * SystemUI unbinds right after the tile click, the service is destroyed and the
 * process reclaimed, so the background thread may never get to run at all. The
 * symptom is "tapped and nothing happened, works after opening the app once".
 */
object ShellRunner {

    private const val TAG = "ShellRunner"

    private val executor = Executors.newSingleThreadExecutor()

    fun run(vararg command: String) {
        val cmd = arrayOf(*command)
        try {
            val process = ShizukuHelper.start(cmd)
            executor.execute {
                try {
                    process.waitFor()
                } catch (t: Throwable) {
                    Log.w(TAG, "waitFor failed: ${cmd.joinToString(" ")}", t)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "failed to start: ${cmd.joinToString(" ")}", t)
        }
    }
}
