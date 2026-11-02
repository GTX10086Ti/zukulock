package com.tiendnm.zukulock

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.service.quicksettings.TileService
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

/**
 * Authorization screen. The original implementation called
 * Shizuku.requestPermission() from a Service, but that API needs an Activity to
 * host the system dialog, so the first authorization could never succeed.
 *
 * This is also the entry point for diagnosing "tapping a tile does nothing":
 * it distinguishes Shizuku-not-running from not-authorized, and offers the
 * battery optimization whitelist, since Shizuku being reclaimed by the system
 * is the number one cause of dead tiles.
 */
class GrantActivity : Activity() {

    companion object {
        private const val REQ_PERMISSION = 1001
    }

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
        rearmTiles()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_PERMISSION) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.msg_granted, Toast.LENGTH_SHORT).show()
            rearmTiles()
        } else {
            Toast.makeText(this, R.string.msg_denied, Toast.LENGTH_LONG).show()
        }
        refresh()
    }

    private fun buildUi(): View {
        val density = resources.displayMetrics.density

        fun px(value: Int) = (value * density).toInt()

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(24), px(32), px(24), px(32))
        }

        column.addView(TextView(this).apply {
            text = getString(R.string.grant_title)
            textSize = 22f
        })

        statusView = TextView(this).apply {
            textSize = 14f
            setPadding(0, px(16), 0, px(8))
        }
        column.addView(statusView)

        column.addView(TextView(this).apply {
            text = getString(R.string.grant_help)
            textSize = 13f
        })

        column.addView(button(R.string.action_grant) { requestPermission() })
        column.addView(button(R.string.action_open_shizuku) { openShizuku() })
        column.addView(button(R.string.action_battery) { requestIgnoreBatteryOptimization() })
        column.addView(button(R.string.action_close) { finish() })

        return ScrollView(this).apply { addView(column) }
    }

    private fun button(labelRes: Int, action: () -> Unit): Button = Button(this).apply {
        text = getString(labelRes)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = (8 * resources.displayMetrics.density).toInt()
        }
        setOnClickListener { action() }
    }

    /** Ask SystemUI to rebind the tiles so a stale "not running" state is cleared. */
    private fun rearmTiles() {
        try {
            TileService.requestListeningState(this, ComponentName(this, ScreenOffTile::class.java))
            TileService.requestListeningState(this, ComponentName(this, PowerOffTile::class.java))
        } catch (t: Throwable) {
            // Only refreshing state, a failure does not affect the main flow.
        }
    }

    private fun requestPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, R.string.msg_shizuku_not_running, Toast.LENGTH_LONG).show()
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.msg_granted, Toast.LENGTH_SHORT).show()
                refresh()
                return
            }
            Shizuku.requestPermission(REQ_PERMISSION)
        } catch (t: Throwable) {
            Toast.makeText(this, t.message ?: "Shizuku error", Toast.LENGTH_LONG).show()
        }
    }

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage(TileBase.SHIZUKU_PACKAGE)
        if (intent == null) {
            Toast.makeText(this, R.string.msg_shizuku_missing, Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        try {
            val power = getSystemService(POWER_SERVICE) as PowerManager
            if (power.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, R.string.msg_battery_done, Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (t: Throwable) {
            Toast.makeText(this, R.string.msg_battery_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun refresh() {
        statusView.text = when (ShizukuState.check()) {
            ShizukuState.GRANTED -> getString(R.string.status_granted)
            ShizukuState.NO_PERMISSION -> getString(R.string.status_not_granted)
            else -> getString(R.string.status_not_running)
        }
    }
}
