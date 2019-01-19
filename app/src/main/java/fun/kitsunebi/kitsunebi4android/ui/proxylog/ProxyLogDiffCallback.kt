package `fun`.kitsunebi.kitsunebi4android.ui.proxylog

import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLog
import androidx.recyclerview.widget.DiffUtil

class ProxyLogDiffCallback : DiffUtil.ItemCallback<ProxyLog>() {

    override fun areItemsTheSame(oldItem: ProxyLog, newItem: ProxyLog): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ProxyLog, newItem: ProxyLog): Boolean {
        return oldItem == newItem
    }
}