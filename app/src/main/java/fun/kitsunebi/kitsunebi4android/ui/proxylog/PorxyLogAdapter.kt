package `fun`.kitsunebi.kitsunebi4android.ui.proxylog

import `fun`.kitsunebi.kitsunebi4android.R
import `fun`.kitsunebi.kitsunebi4android.common.humanReadableByteCount
import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView

class ProxyLogAdapter(val context: Context) : PagedListAdapter<ProxyLog, ProxyLogAdapter.ProxyLogViewHolder>(ProxyLogDiffCallback()) {
    override fun onBindViewHolder(logHolder: ProxyLogViewHolder, position: Int) {
        var log = getItem(position)

        if (log == null) {
            logHolder.clear()
        } else {
            logHolder.bind(log)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProxyLogViewHolder {
        return ProxyLogViewHolder(LayoutInflater.from(context).inflate(R.layout.proxy_log_record,
                parent, false), context)
    }

    class ProxyLogViewHolder(view: View, context: Context) : RecyclerView.ViewHolder(view) {
        private val context = context
        var row1: TextView = view.findViewById(R.id.row1)
        var row2_1: TextView = view.findViewById(R.id.row2_1)
        var row2_2: TextView = view.findViewById(R.id.row2_2)
        var row3: TextView = view.findViewById(R.id.row3)

        fun bind(proxyLog: ProxyLog) {
            proxyLog.recordType?.let {
                row1.text = proxyLog.target

                when (it) {
                    // TCP/UDP records
                    0 -> {
                        row2_1.text = "${proxyLog.tag}"

                        if (proxyLog.uploadBytes != null && proxyLog.downloadBytes != null) {
                            val up = humanReadableByteCount(proxyLog.uploadBytes!!.toLong(), true)
                            val down = humanReadableByteCount(proxyLog.downloadBytes!!.toLong(), true)
                            row2_2.text = "${up}↑, ${down}↓"
                            if (proxyLog.downloadBytes!!.toInt() == 0) {
                                row2_2.setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
                            } else {
                                row2_2.setTextColor(ContextCompat.getColor(context, R.color.colorDefaultText))
                            }

                        }

                        val sdf = java.text.SimpleDateFormat("HH:mm:ss")
                        val start = java.util.Date(proxyLog.startTime!! / 1000000)
                        val end = java.util.Date(proxyLog.endTime!! / 1000000)
                        row3.text = "${sdf.format(start)}, ${sdf.format(end)}"
                    }

                    // DNS records
                    1 -> {
                        if (proxyLog.tag == null || proxyLog.tag!!.isEmpty()) {
                            row2_1.text = "inherited"
                        } else {
                            row2_1.text = "${proxyLog.tag}"
                        }

                        proxyLog.dnsQueryType?.let {
                            val duration = (proxyLog.endTime!! - proxyLog.startTime!!) / 1000000
                            var durationRepr: String = ""
                            if (duration >= 1000) {
                                durationRepr = "${duration / 1000}s"
                            } else {
                                durationRepr = "${duration}ms"
                            }

                            when (it) {
                                // A
                                0 -> {
                                    row2_2.text = "A, ${durationRepr}, ${proxyLog.dnsNumIPs} results"
                                }
                                // AAAA
                                1 -> {
                                    row2_2.text = "AAAA, ${durationRepr}, ${proxyLog.dnsNumIPs} results"
                                }
                                // A,AAAA
                                2 -> {
                                    row2_2.text = "A,AAAA, ${durationRepr}, ${proxyLog.dnsNumIPs} results"
                                }
                                else -> {
                                }
                            }
                        }

                        row3.text = "${proxyLog.dnsRequest}"
                    }
                    else -> {
                    }
                }

                proxyLog.tag?.let {
                    if (it.toLowerCase().contains("proxy")) {
                        row2_1.setTextColor(ContextCompat.getColor(context, R.color.colorProxyTag))
                    } else if (it.toLowerCase().contains("direct") || it.toLowerCase().contains("free")) {
                        row2_1.setTextColor(ContextCompat.getColor(context, R.color.colorDirectTag))
                    } else if (it.toLowerCase().contains("block") || it.toLowerCase().contains("reject")) {
                        row2_1.setTextColor(ContextCompat.getColor(context, R.color.colorBlockTag))
                    } else {
                        row2_1.setTextColor(ContextCompat.getColor(context, R.color.colorDefaultText))
                    }
                }
            }

        }

        fun clear() {
            row1.text = null
            row2_1.text = null
            row2_2.text = null
            row3.text = null
        }
    }
}