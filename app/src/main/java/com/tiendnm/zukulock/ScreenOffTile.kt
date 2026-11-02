package com.tiendnm.zukulock

/**
 * One tap in the shade turns the screen off immediately.
 * Uses KEYCODE_SLEEP (223) rather than KEYCODE_POWER (26): the latter toggles,
 * so it would light the screen back up when it is already off.
 */
class ScreenOffTile : TileBase() {

    override fun perform() {
        ShellRunner.run("input", "keyevent", "223")
    }
}
