package com.tiendnm.zukulock

import java.lang.reflect.Method
import rikka.shizuku.ShizukuRemoteProcess

object ShizukuHelper {

    private val newProcessMethod: Method by lazy {
        Class.forName("rikka.shizuku.Shizuku")
            .getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            .apply { isAccessible = true }
    }

    /**
     * Hands the command to Shizuku (a single binder call) and returns as soon as
     * the remote process is available.
     *
     * This must happen synchronously on the calling thread: SystemUI unbinds
     * immediately after a tile click and the app process can be reclaimed at any
     * moment, so the IPC must not be deferred to a background thread.
     */
    fun start(
        cmd: Array<String>,
        env: Array<String>? = null,
        dir: String? = null
    ): ShizukuRemoteProcess {
        val result = newProcessMethod.invoke(null, cmd, env, dir)
        return result as? ShizukuRemoteProcess
            ?: throw IllegalStateException("unexpected result from Shizuku.newProcess: $result")
    }
}
