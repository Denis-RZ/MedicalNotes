package com.medicalnotes.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.medicalnotes.app.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        
        // Открываем главную активность
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivityAndCollapse(intent)
    }

    private fun updateTile() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "Медицинские заметки"
            contentDescription = "Открыть приложение для управления лекарствами"
            updateTile()
        }
    }
} 