package org.soralis.shuttersound

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class ShutterTileService : TileService() {

    override fun onStartListening() {
        val tile = qsTile ?: return
        if (ShutterSound.status() != ShutterSound.Status.READY) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
            return
        }
        ShutterSound.query(this) { result ->
            val current = qsTile ?: return@query
            current.state = result.fold(
                { if (isChanged(it)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE },
                { Tile.STATE_UNAVAILABLE }
            )
            current.updateTile()
        }
    }

    override fun onClick() {
        if (ShutterSound.status() != ShutterSound.Status.READY) return
        ShutterSound.query(this) { result ->
            val current = result.getOrNull() ?: return@query
            val next = if (isChanged(current)) ForceUse.deviceDefault() else released()
            ShutterSound.set(this, next) { onStartListening() }
        }
    }

    companion object {
        /** The value that lets the ringer mode silence the shutter. */
        fun released() = ForceUse.FORCE_NONE

        fun isChanged(config: Int) = config != ForceUse.deviceDefault()
    }
}
