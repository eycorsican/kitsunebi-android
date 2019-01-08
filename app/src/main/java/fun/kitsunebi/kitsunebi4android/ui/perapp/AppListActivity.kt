package `fun`.kitsunebi.kitsunebi4android.ui.perapp

import `fun`.kitsunebi.kitsunebi4android.R
import `fun`.kitsunebi.kitsunebi4android.storage.Preferences
import android.Manifest
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class AppListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var appList: ArrayList<PackageInfo>
    private lateinit var selectedList: ArrayList<String>
    private var perAppMode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.extras?.let {
            perAppMode = it.getInt("per_app_mode")
        }

        when (perAppMode) {
            0 -> {
                setTitle(R.string.allowed_list)
            }
            1 -> {
                setTitle(R.string.disallowed_list)
            }
            else -> {
            }
        }

        setContentView(R.layout.activity_app_list)

        appList = arrayListOf()
        selectedList = arrayListOf()

        viewManager = LinearLayoutManager(this)
        viewAdapter = AppAdapter(appList)

        recyclerView = findViewById<RecyclerView>(R.id.app_list_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        thread(start = true) {
            var tmpAppList = ArrayList(packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS).filter {
                it.requestedPermissions?.contains(Manifest.permission.INTERNET) ?: false
            })

            when (perAppMode) {
                0 -> {
                    val allowedAppList = Preferences.getString(applicationContext, getString(R.string.per_app_allowed_app_list), null)
                    selectedList.addAll(ArrayList(allowedAppList.split(",")))
                }
                1 -> {
                    val disallowedAppList = Preferences.getString(applicationContext, getString(R.string.per_app_disallowed_app_list), null)
                    selectedList.addAll(ArrayList(disallowedAppList.split(",")))
                }
                else -> {
                    runOnUiThread { finish() }
                    return@thread
                }
            }

            tmpAppList = ArrayList(tmpAppList.sortedWith(compareBy<PackageInfo> {
                !selectedList.contains(it.packageName)
            }.thenBy {
                packageManager.getApplicationLabel(it.applicationInfo).toString().toLowerCase()
            }))

            appList.addAll(tmpAppList)

            runOnUiThread { viewAdapter.notifyDataSetChanged() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_btn -> {
                val selectedAppList = selectedList.joinToString(",")
                when (perAppMode) {
                    0 -> {
                        Preferences.putString(applicationContext, getString(R.string.per_app_allowed_app_list), selectedAppList)
                    }
                    1 -> {
                        Preferences.putString(applicationContext, getString(R.string.per_app_disallowed_app_list), selectedAppList)
                    }
                    else -> {
                    }
                }
                NavUtils.navigateUpFromSameTask(this)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class AppOnClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            val packageName = v.findViewById<TextView>(R.id.app_package).text.toString()
            val clickedPkgInfo = appList.filter { it.packageName == packageName }.single()
            val pos = appList.indexOf(clickedPkgInfo)
            if (!selectedList.contains(packageName)) {
                selectedList.add(packageName)
                viewAdapter.notifyItemChanged(pos)
            } else {
                selectedList.remove(packageName)
                viewAdapter.notifyItemChanged(pos)
            }
        }
    }

    inner class AppAdapter(private val myDataset: ArrayList<PackageInfo>) :
            RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

        inner class AppViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            var appIcon: ImageView = view.findViewById(R.id.app_icon)
            var appLabel: TextView = view.findViewById(R.id.app_label)
            var appPackage: TextView = view.findViewById(R.id.app_package)
            var appSwitch: Switch = view.findViewById(R.id.app_switch)

            fun bind(pkgInfo: PackageInfo) {
                appIcon.setImageDrawable(packageManager.getApplicationIcon(pkgInfo.applicationInfo))
                appLabel.text = packageManager.getApplicationLabel(pkgInfo.applicationInfo)
                appPackage.text = pkgInfo.packageName
                appSwitch.isChecked = selectedList.contains(pkgInfo.packageName)
            }

            fun clear() {
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): AppAdapter.AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_list_item, parent, false)
            view.setOnClickListener(AppOnClickListener())
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            var pkgInfo = appList[position]
            if (pkgInfo == null) {
                holder.clear()
            } else {
                holder.bind(pkgInfo)
            }
        }

        override fun getItemCount() = myDataset.size
    }
}