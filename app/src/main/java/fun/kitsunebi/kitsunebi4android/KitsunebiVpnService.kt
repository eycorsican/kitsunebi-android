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
    var proxyDomainIPMap: HashMap<String, String> = HashMap<String, String>()
    var pfd: ParcelFileDescriptor? = null
    var inputStream: FileInputStream? = null
    var outputStream: FileOutputStream? = null
    var buffer = ByteBuffer.allocate(1501)
    var isStopped = false

    data class Config(val outbounds: List<Outbound>? = null,
                      val outboundDetour: List<Outbound>? = null,
                      val outbound: Outbound? = null,
                      val dns: Dns? = null)
    data class Dns(val servers: List<Any>? = null)
    data class Outbound(val protocol: String = "", val settings: Settings? = null)
    data class Settings(val vnext: List<Server?>? = null)
    data class Server(val address: String? = null)

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
        override fun protect(fd: Long): Boolean {
            return vpnService.protect(fd.toInt())
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
            // FIXME: Support other protocols.
            fun resolveIP(outbound: Outbound) {
                if (outbound.protocol == "vmess") {
                    outbound.settings?.vnext?.forEach {
                        if (it != null && it.address != null) {
                            println("vmess server address: ${it.address}")
                            try {
                                // FIXME: Get all IPs.
                                val addr = InetAddress.getByName(it.address)
                                val ip = addr.getHostAddress()
                                if (it.address != ip) {
                                    // address is a domain name
                                    proxyDomainIPMap.put(it.address, ip)
                                }
                            } catch (e: Exception) {
                                println(e)
                                // FIXME: Handle error for the corresponding server, e.g. remove the
                                // from the config, notifying user that the server it not usable.
                            }
                        }
                    }
                }
            }

            val config = Klaxon().parse<Config>(configString)
            if (config != null) {
                if (config.dns == null || config.dns.servers == null || config.dns.servers.size == 0) {
                    println("must configure dns servers since v2ray will use localhost if there isn't any dns servers")
                    sendBroadcast(Intent("vpn_start_err_dns"))
                    return@thread
                }

                config.dns.servers.forEach {
                    var dnsServer = it as? String
                    if ( dnsServer != null && dnsServer == "localhost") {
                        println("using local dns resolver is not allowed since it will cause infinite loop")
                        sendBroadcast(Intent("vpn_start_err_dns"))
                        return@thread
                    }
                }

                config.outbounds?.forEach {
                    resolveIP(it)
                }

                // For legacy config format.
                config.outboundDetour?.forEach {
                    resolveIP(it)
                }
                if (config.outbound != null) {
                    resolveIP(config.outbound)
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
            if ((pfd == null) || !tun2socks.Tun2socks.setNonblock(pfd!!.fd.toLong(), false)) {
                println("failed to put tunFd in blocking mode")
                sendBroadcast(Intent("vpn_start_err"))
                return@thread
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
            val serverDomains = proxyDomainIPMap.keys.joinToString(separator = ",")
            val serverIPs = proxyDomainIPMap.values.joinToString(separator = ",")
            tun2socks.Tun2socks.startV2Ray(flow, service, configString.toByteArray(), filesDir.absolutePath, serverDomains, serverIPs)

            sendBroadcast(Intent("vpn_started"))

            handlePackets()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }
}