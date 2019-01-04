package `fun`.kitsunebi.kitsunebi4android.service

import `fun`.kitsunebi.kitsunebi4android.R
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.os.ParcelFileDescriptor
import tun2socks.PacketFlow
import tun2socks.Tun2socks
import tun2socks.VpnService as Tun2socksVpnService
import kotlin.concurrent.thread
import com.beust.klaxon.Klaxon
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

open class SimpleVpnService: VpnService() {
    private var configString: String = ""
    private var pfd: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var buffer = ByteBuffer.allocate(1501)
    var isStopped = false
    
    private val cm by lazy { this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    @TargetApi(28)
    private var underlyingNetwork: Network? = null
        @TargetApi(28)
        set(value) {
            setUnderlyingNetworks(if (value == null) null else arrayOf(value))
            field = value
        }

    companion object {
        @TargetApi(28)
        private val defaultNetworkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
    }

    @TargetApi(28)
    private val defaultNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            underlyingNetwork = network
        }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities?) {
            underlyingNetwork = network
        }
        override fun onLost(network: Network) {
            underlyingNetwork = null
        }
    }

    data class Config(val outbounds: List<Outbound>? = null,
                      val outboundDetour: List<Outbound>? = null,
                      val outbound: Outbound? = null,
                      val dns: Dns? = null)
    data class Dns(val servers: List<Any>? = null)
    data class Outbound(val protocol: String = "", val settings: Settings? = null)
    data class Settings(val vnext: List<Server?>? = null)
    data class Server(val address: String? = null)

    private val broadcastReceiver = object : BroadcastReceiver() {
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
        Tun2socks.stopV2Ray()
        pfd?.close()
        pfd = null
        inputStream = null
        outputStream = null
        sendBroadcast(Intent("vpn_stopped"))
        stopSelf()
    }

    class Flow(stream: FileOutputStream?): PacketFlow {
        private val flowOutputStream = stream
        override fun writePacket(pkt: ByteArray?) {
            flowOutputStream?.write(pkt)
        }
    }

    class Service(service: VpnService): Tun2socksVpnService {
        private val vpnService = service
        override fun protect(fd: Long): Boolean {
            return vpnService.protect(fd.toInt())
        }
    }

    private fun handlePackets() {
        while (!isStopped) {
            val n = inputStream?.read(buffer.array())
            n?.let { it } ?: return
            if (n > 0) {
                buffer.limit(n)
                Tun2socks.inputPacket(buffer.array())
                buffer.clear()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(broadcastReceiver, IntentFilter("stop_vpn"))
        registerReceiver(broadcastReceiver, IntentFilter("ping"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configString = intent?.extras?.get("config").toString()

        thread (start = true) {
            val config = Klaxon().parse<Config>(configString)
            if (config != null) {
                if (config.dns == null || config.dns.servers == null || config.dns.servers.size == 0) {
                    println("must configure dns servers since v2ray will use localhost if there isn't any dns servers")
                    sendBroadcast(Intent("vpn_start_err_dns"))
                    return@thread
                }

                config.dns.servers.forEach {
                    val dnsServer = it as? String
                    if ( dnsServer != null && dnsServer == "localhost") {
                        println("using local dns resolver is not allowed since it will cause infinite loop")
                        sendBroadcast(Intent("vpn_start_err_dns"))
                        return@thread
                    }
                }
            } else {
                println("parsing v2ray config failed")
                sendBroadcast(Intent("vpn_start_err"))
                return@thread
            }


            pfd = Builder().setSession("vv")
                    .setMtu(1500)
                    .addAddress("10.233.233.233", 30)
                    .addDnsServer("223.5.5.5")
                    .addSearchDomain("local")
                    .addRoute("0.0.0.0", 0)
                    .establish()

            // Put the tunFd in blocking mode. Since we are reading packets from this fd in the
            // main loop, failing to do this will cause very high CPU utilization, which is
            // absolutely not what we want. Doing this in Go code because Android only has
            // limited support for this feature, which requires API level >= 21.
            if ((pfd == null) || !Tun2socks.setNonblock(pfd!!.fd.toLong(), false)) {
                println("failed to put tunFd in blocking mode")
                sendBroadcast(Intent("vpn_start_err"))
                return@thread
            }

            if (Build.VERSION.SDK_INT >= 28) {
                cm.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            }

            inputStream = FileInputStream(pfd!!.fileDescriptor)
            outputStream = FileOutputStream(pfd!!.fileDescriptor)

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

            Tun2socks.setLocalDNS("223.5.5.5:53")
            Tun2socks.startV2Ray(flow, service, configString.toByteArray(), filesDir.absolutePath)

            sendBroadcast(Intent("vpn_started"))

            handlePackets()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVPN()
    }
}