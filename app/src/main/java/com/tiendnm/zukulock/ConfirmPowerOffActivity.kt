package com.tiendnm.zukulock

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle

/**
 * Confirmation screen launched from the power off tile. The theme is fully
 * transparent, so it looks like an ordinary dialog.
 */
class ConfirmPowerOffActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
            .setTitle(R.string.tile_power_off)
            .setMessage(R.string.confirm_power_off)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                ShellRunner.run("reboot", "-p")
                finish()
            }
            .setNegativeButton(R.string.action_cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
