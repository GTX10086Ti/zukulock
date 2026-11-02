package com.tiendnm.zukulock

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

/**
 * Shared logic for both tiles.
 *
 * A tile does not need the app to stay resident: SystemUI rebinds this service
 * every time the shade is pulled down. But when a tile is started from an empty
 * process, Shizuku's binder has not been pushed in by ShizukuProvider yet, and
 * reporting "not ready" at that moment is what caused the tiles to only start
 * working after the app had been opened once. So wait for the binder before
 * doing anything.
 */
abstract class TileBase : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        ShizukuState.awaitBinder(800L) { state -> applyState(state) }
    }

    override fun onClick() {
        super.onClick()
        ShizukuState.awaitBinder(1500L) { state ->
            when (state) {
                ShizukuState.GRANTED -> perform()
                ShizukuState.NO_PERMISSION -> openActivity(Intent(this, GrantActivity::class.java))
                else -> {
                    Log.w(TAG, "tile clicked but shizuku binder unavailable")
                    openShizuku()
                }
            }
        }
    }

    protected abstract fun perform()

    private fun applyState(state: Int) {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (state) {
                ShizukuState.GRANTED -> null
                ShizukuState.NO_PERMISSION -> getString(R.string.tile_subtitle_no_permission)
                else -> getString(R.string.tile_subtitle_not_running)
            }
        }
        tile.updateTile()
    }

    /** Shizuku really is not running: send the user straight to it to tap "Start". */
    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent == null) {
            openActivity(Intent(this, GrantActivity::class.java))
        } else {
            openActivity(intent)
        }
    }

    protected fun openActivity(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private const val TAG = "TileBase"
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
