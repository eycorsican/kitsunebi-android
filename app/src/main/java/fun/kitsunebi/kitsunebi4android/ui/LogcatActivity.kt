package `fun`.kitsunebi.kitsunebi4android.ui

import `fun`.kitsunebi.kitsunebi4android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_logcat.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.scheduleAtFixedRate


class LogcatActivity : AppCompatActivity() {

    private lateinit var logcatTextView: TextView
    private lateinit var bgThread: Thread
    private lateinit var logBuilder: StringBuilder
    private lateinit var bgTimer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logcat)
        logcatTextView = findViewById<TextView>(R.id.logcat_text)
        logcatScroll.isSmoothScrollingEnabled = true

        bgThread = object : Thread() {
            override fun run() {
                try {
                    logBuilder = StringBuilder()
                    bgTimer = Timer()
                    val process = Runtime.getRuntime().exec("logcat")
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    fun readAndDisplay() {
                        while (bufferedReader.ready()) {
                            val line = bufferedReader.readLine()
                            logBuilder.append(line + "\n")

                        }
                        runOnUiThread {
                            logcatTextView.text = logBuilder.toString()
                        }
                    }
                    bgTimer.schedule(1000) {
                        readAndDisplay()
                    }
                    bgTimer.scheduleAtFixedRate(0, 5000) {
                        readAndDisplay()
                    }
                } catch (e: IOException) {
                    println(e)
                }
            }
        }
        bgThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        bgTimer.cancel()
        bgThread.interrupt()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_logcat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.copy_btn -> {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip: ClipData = ClipData.newPlainText("logcat text", logBuilder.toString())
                clipboard.primaryClip = clip
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}