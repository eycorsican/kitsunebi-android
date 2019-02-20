package `fun`.kitsunebi.kitsunebi4android.common

import android.app.AlertDialog
import android.content.Context

open class SingletonHolder<out T, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val i = instance
        if (i != null) {
            return i
        }

        return synchronized(this) {
            val i2 = instance
            if (i2 != null) {
                i2
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

public fun humanReadableByteCount(bytes: Long, si: Boolean): String {
    val unit = if (si) 1000 else 1024
    if (bytes < unit) return bytes.toString() + " B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sB", bytes / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

public fun showAlert(context: Context, msg: String) {
    val dialog = AlertDialog.Builder(context).setTitle("Message").setMessage(msg)
            .setPositiveButton("Ok", { dialog, i ->
            })
    dialog.show()
}