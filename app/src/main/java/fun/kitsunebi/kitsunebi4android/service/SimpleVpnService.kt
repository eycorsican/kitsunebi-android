package `fun`.kitsunebi.kitsunebi4android.service

import `fun`.kitsunebi.kitsunebi4android.R
import `fun`.kitsunebi.kitsunebi4android.common.Constants
import `fun`.kitsunebi.kitsunebi4android.storage.PROXY_LOG_DB_NAME
import `fun`.kitsunebi.kitsunebi4android.storage.Preferences
import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLog
import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLogDatabase
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import android.os.ParcelFileDescriptor
import com.beust.klaxon.Klaxon
import tun2socks.PacketFlow
import tun2socks.Tun2socks
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import tun2socks.DBService as Tun2socksDBService
import tun2socks.VpnService as Tun2socksVpnService


open class SimpleVpnService : VpnService() {

    private var configString: String = ""
    private var pfd: ParcelFileDescriptor? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private var buffer = ByteBuffer.allocate(1501)
    @Volatile
    private var running = false
    private lateinit var bgThread: Thread

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
                    if (running) {
                        sendBroadcast(Intent("pong"))
                    }
                }
            }
        }
    }

    private fun stopVPN() {
        Tun2socks.stopV2Ray()
        pfd?.close()
        pfd = null
        inputStream = null
        outputStream = null
        running = false
        sendBroadcast(Intent("vpn_stopped"))
        Preferences.putBool(applicationContext, getString(R.string.vpn_is_running), false)
        stopSelf()
    }

    class Flow(stream: FileOutputStream?) : PacketFlow {
        private val flowOutputStream = stream
        override fun writePacket(pkt: ByteArray?) {
            flowOutputStream?.write(pkt)
        }
    }

    class Service(service: VpnService) : Tun2socksVpnService {
        private val vpnService = service
        override fun protect(fd: Long): Boolean {
            return vpnService.protect(fd.toInt())
        }
    }

    class DBService(db: ProxyLogDatabase) : Tun2socksDBService {
        private val db = db
        override fun insertProxyLog(p0: String?, p1: String?, p2: Long, p3: Long, p4: Int, p5: Int, p6: Int, p7: Int, p8: String?, p9: String?, p10: Int) {
            db.proxyLogDao().insertAll(ProxyLog(0,
                    p0,
                    p1,
                    p2,
                    p3,
                    p4,
                    p5,
                    p6,
                    p7,
                    p8,
                    p9,
                    p10))
        }
    }

    private fun handlePackets() {
        while (running) {
            try {
                val n = inputStream?.read(buffer.array())
                n?.let { it } ?: return
                if (n > 0) {
                    buffer.limit(n)
                    Tun2socks.inputPacket(buffer.array())
                    buffer.clear()
                }
            } catch (e: Exception) {
                println("failed to read bytes from TUN fd")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(broadcastReceiver, IntentFilter("stop_vpn"))
        registerReceiver(broadcastReceiver, IntentFilter("ping"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        configString = Preferences.getString(applicationContext, Constants.PREFERENCE_CONFIG_KEY, Constants.DEFAULT_CONFIG)

        bgThread = thread(start = true) {
            val config = try { Klaxon().parse<Config>(configString) } catch (e: Exception) {
                sendBroadcast(android.content.Intent("vpn_start_err_config"))
                stopVPN()
                return@thread
            }
            if (config != null) {
                if (config.dns == null || config.dns.servers == null || config.dns.servers.size == 0) {
                    println("must configure dns servers since v2ray will use localhost if there isn't any dns servers")
                    sendBroadcast(Intent("vpn_start_err_dns"))
                    stopVPN()
                    return@thread
                }

                config.dns.servers.forEach {
                    val dnsServer = it as? String
                    if (dnsServer != null && dnsServer == "localhost") {
                        println("using local dns resolver is not allowed since it will cause infinite loop")
                        sendBroadcast(Intent("vpn_start_err_dns"))
                        stopVPN()
                        return@thread
                    }
                }
            } else {
                println("parsing v2ray config failed")
                sendBroadcast(Intent("vpn_start_err"))
                stopVPN()
                return@thread
            }

            val localDns = Preferences.getString(applicationContext, getString(R.string.local_dns), "223.5.5.5")

            val builder = Builder().setSession("Kitsunebi")
                    .setMtu(1500)
                    .addAddress("10.233.233.233", 30)
                    .addDnsServer(localDns)
                    .addRoute("0.0.0.0", 0)

            val isEnablePerAppVpn = Preferences.getBool(applicationContext, getString(R.string.is_enable_per_app_vpn), null)
            @TargetApi(21)
            if (isEnablePerAppVpn) {
                val perAppMode = Preferences.getString(applicationContext, getString(R.string.per_app_mode), null)
                when (Integer.parseInt(perAppMode)) {
                    0 -> {
                        val allowedAppList = Preferences.getString(applicationContext, getString(R.string.per_app_allowed_app_list), null)
                        for (packageName in allowedAppList.split(",")) {
                            builder.addAllowedApplication(packageName)
                        }
                    }
                    1 -> {
                        val disallowedAppList = Preferences.getString(applicationContext, getString(R.string.per_app_disallowed_app_list), null)
                        for (packageName in disallowedAppList.split(",")) {
                            builder.addDisallowedApplication(packageName)
                        }
                    }
                    else -> {
                    }
                }
            }

            pfd = builder.establish()

            // Put the tunFd in blocking mode. Since we are reading packets from this fd in the
            // main loop, failing to do this will cause very high CPU utilization, which is
            // absolutely not what we want. Doing this in Go code because Android only has
            // limited support for this feature, which requires API level >= 21.
            if ((pfd == null) || !Tun2socks.setNonblock(pfd!!.fd.toLong(), false)) {
                println("failed to put tunFd in blocking mode")
                sendBroadcast(Intent("vpn_start_err"))
                stopVPN()
                return@thread
            }

            if (Build.VERSION.SDK_INT >= 28) {
                cm.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            }

            inputStream = FileInputStream(pfd!!.fileDescriptor)
            outputStream = FileOutputStream(pfd!!.fileDescriptor)

            val flow = Flow(outputStream)
            val service = Service(this)
            var dbService: DBService? = null
            val enableProxyLogging = Preferences.getBool(applicationContext, getString(R.string.is_enable_proxy_logging), null)
            if (enableProxyLogging) {
                dbService = DBService(ProxyLogDatabase.getInstance(applicationContext))
            }

            val files = filesDir.list()
            // FIXME  copy only when update
            val geoipBytes = resources.openRawResource(R.raw.geoip).readBytes()
            val fos = openFileOutput("geoip.dat", Context.MODE_PRIVATE)
            fos.write(geoipBytes)
            fos.close()

            val geositeBytes = resources.openRawResource(R.raw.geosite).readBytes()
            val fos2 = openFileOutput("geosite.dat", Context.MODE_PRIVATE)
            fos2.write(geositeBytes)
            fos2.close()

//            if (!files.contains("geoip.dat") || !files.contains("geosite.dat")) {
//                val geoipBytes = resources.openRawResource(R.raw.geoip).readBytes()
//                val fos = openFileOutput("geoip.dat", Context.MODE_PRIVATE)
//                fos.write(geoipBytes)
//                fos.close()
//
//                val geositeBytes = resources.openRawResource(R.raw.geosite).readBytes()
//                val fos2 = openFileOutput("geosite.dat", Context.MODE_PRIVATE)
//                fos2.write(geositeBytes)
//                fos2.close()
//            }

            ProxyLogDatabase.getInstance(applicationContext).proxyLogDao().getAllCount()

            var sniffing = Preferences.getString(applicationContext, getString(R.string.sniffing), "http,tls")
            // Just ensure no whitespaces in the the string.
            val sniffingList = sniffing.split(",")
            var sniffings = ArrayList<String>()
            for (s in sniffingList) {
                sniffings.add(s.trim())
            }
            sniffing = sniffings.joinToString(",")

            val inboundTag = Preferences.getString(applicationContext, getString(R.string.inbound_tag), "tun2socks")

            Tun2socks.setLocalDNS("$localDns:53")
            val ret = Tun2socks.startV2Ray(flow, service, dbService, configString.toByteArray(), inboundTag, sniffing, filesDir.absolutePath)
            if (ret.toInt() != 0) {
                sendBroadcast(Intent("vpn_start_err_config"))
                stopVPN()
                return@thread
            }

            sendBroadcast(Intent("vpn_started"))
            Preferences.putBool(applicationContext, getString(R.string.vpn_is_running), true)

            running = true
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