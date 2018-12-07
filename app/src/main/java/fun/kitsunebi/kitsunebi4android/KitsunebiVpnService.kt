package `fun`.kitsunebi.kitsunebi4android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import tun2socks.PacketFlow
import tun2socks.VpnService as Tun2socksVpnService
import kotlin.concurrent.thread
import com.beust.klaxon.Klaxon
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer

open class KitsunebiVpnService: VpnService() {
    var configString: String = ""
    var pfd: ParcelFileDescriptor? = null
    var inputStream: FileInputStream? = null
    var outputStream: FileOutputStream? = null
    var buffer = ByteBuffer.allocate(1501)
    var isStopped = false

//    data class Config(val outbounds: List<Outbound>?)
//    data class Outbound(val protocol: String = "", val settings: Settings? = null)
//    data class Settings(val vnext: List<Server?>? = emptyList())
//    data class Server(val address: String? = null)

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            when (intent?.action) {
                "stop_vpn" -> {
                    stopVPN()
                }
                "ping" -> {
                    if (!isStopped) {
                        sendBroadcast(Intent("pong"))
                    }
                }

            }
        }
    }

    private fun stopVPN() {
        isStopped = true
        tun2socks.Tun2socks.stopV2Ray()
        pfd?.close()
        pfd = null
        inputStream = null
        outputStream = null
        sendBroadcast(Intent("vpn_stopped"))
        stopSelf()
    }

    class Flow(stream: FileOutputStream?): PacketFlow {
        val flowOutputStream = stream
        override fun writePacket(pkt: ByteArray?) {
            flowOutputStream?.write(pkt)
        }
    }

    class Service(service: VpnService): Tun2socksVpnService {
        val vpnService = service
        override fun protect(fd: Long) {
            vpnService.protect(fd.toInt())
        }
    }

    fun handlePackets() {
        while (!isStopped) {
            val n = inputStream?.read(buffer.array())
            n?.let { it } ?: return
            if (n > 0) {
                buffer.limit(n)
                tun2socks.Tun2socks.inputPacket(buffer.array())
                buffer.clear()
            }
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                // In non-blocking mode
                Thread.sleep(50)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(broadcastReceiver, IntentFilter("stop_vpn"))
        registerReceiver(broadcastReceiver, IntentFilter("ping"))
    }

    override public fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configString = intent?.extras?.get("config").toString()

        thread (start = true) {
            val builder = Builder()
            builder.setSession("vv")
                    .setMtu(1500)
                    .addAddress("10.233.233.233", 30)
                    .addDnsServer("223.5.5.5")
                    .addSearchDomain("local")
                    .addRoute("0.0.0.0", 0)
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        builder.setBlocking(true)
                    }
            pfd = builder.establish()

            inputStream = FileInputStream(pfd?.fileDescriptor)
            outputStream = FileOutputStream(pfd?.fileDescriptor)

            val flow = Flow(outputStream)
            val service = Service(this)

            val files = filesDir.list()
            if (!files.contains("geoip.dat") || !files.contains("geosite.dat")) {
                val geoipBytes = resources.openRawResource(R.raw.geoip).readBytes()
                val fos = openFileOutput("geoip.dat", Context.MODE_PRIVATE)
                fos.write(geoipBytes)
                fos.close()

                val geositeBytes = resources.openRawResource(R.raw.geosite).readBytes()
                val fos2 = openFileOutput("geosite.dat", Context.MODE_PRIVATE)
                fos2.write(geositeBytes)
                fos2.close()
            }

            tun2socks.Tun2socks.startV2Ray(flow, service, configString.toByteArray(), filesDir.absolutePath)

            sendBroadcast(Intent("vpn_started"))

            handlePackets()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }
}