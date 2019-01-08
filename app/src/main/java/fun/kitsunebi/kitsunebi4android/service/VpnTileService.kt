package `fun`.kitsunebi.kitsunebi4android.service

import `fun`.kitsunebi.kitsunebi4android.R
import `fun`.kitsunebi.kitsunebi4android.storage.Preferences
import `fun`.kitsunebi.kitsunebi4android.ui.StartVpnActivity
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

@TargetApi(24)
class VpnTileService : TileService() {

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                "vpn_stopped" -> {
                    qsTile.state = Tile.STATE_INACTIVE
                }
                "vpn_started" -> {
                    qsTile.state = Tile.STATE_ACTIVE
                }
                "vpn_start_err" -> {
                    qsTile.state = Tile.STATE_INACTIVE
                }
                "vpn_start_err_dns" -> {
                    qsTile.state = Tile.STATE_INACTIVE
                }
                "pong" -> {
                    qsTile.state = Tile.STATE_ACTIVE
                    Preferences.putBool(applicationContext, getString(R.string.vpn_is_running), true)
                }
                else -> {
                    qsTile.state = Tile.STATE_INACTIVE
                }
            }
            qsTile.updateTile()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, StartVpnActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun stopVpn() {
        sendBroadcast(Intent("stop_vpn"))
    }

    override fun onTileAdded() {
        super.onTileAdded()

        // Update state
        qsTile.state = Tile.STATE_INACTIVE

        // Update looks
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (qsTile.state == Tile.STATE_INACTIVE) {
            startVpn()
        } else {
            stopVpn()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile.state = Tile.STATE_INACTIVE
//        val vpnRunning = Preferences.getBool(applicationContext, getString(R.string.vpn_is_running), null)
//        vpnRunning?.let {
//            if (it) {
//                qsTile.state = Tile.STATE_ACTIVE
//            } else {
//                qsTile.state = Tile.STATE_INACTIVE
//            }
//        }
        qsTile.updateTile()

        registerReceiver(broadcastReceiver, IntentFilter("vpn_stopped"))
        registerReceiver(broadcastReceiver, IntentFilter("vpn_started"))
        registerReceiver(broadcastReceiver, IntentFilter("vpn_start_err"))
        registerReceiver(broadcastReceiver, IntentFilter("vpn_start_err_dns"))
        registerReceiver(broadcastReceiver, IntentFilter("pong"))

        sendBroadcast(Intent("ping"))
    }

    override fun onStopListening() {
        super.onStopListening()

        unregisterReceiver(broadcastReceiver)
    }
}
