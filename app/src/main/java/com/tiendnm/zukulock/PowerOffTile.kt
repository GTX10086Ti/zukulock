package com.tiendnm.zukulock

import android.content.Intent

/**
 * One tap in the shade shows a confirmation dialog, powering off on confirm.
 * Powering off is irreversible and especially risky on a device with a broken
 * power button, so an extra confirmation is always required.
 */
class PowerOffTile : TileBase() {

    override fun perform() {
        openActivity(Intent(this, ConfirmPowerOffActivity::class.java))
    }
}
