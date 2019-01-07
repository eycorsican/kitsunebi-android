package `fun`.kitsunebi.kitsunebi4android.ui

import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLog
import `fun`.kitsunebi.kitsunebi4android.storage.ProxyLogDatabase
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList

class ProxyLogViewModel constructor(application: Application)
    : AndroidViewModel(application) {

    private var proxyLogLiveData: LiveData<PagedList<ProxyLog>>

    init {
        val factory: DataSource.Factory<Int, ProxyLog> =
                ProxyLogDatabase.getInstance(getApplication()).proxyLogDao().getAllPaged()

        val pagedListBuilder: LivePagedListBuilder<Int, ProxyLog> = LivePagedListBuilder<Int, ProxyLog>(factory,
                50)
        proxyLogLiveData = pagedListBuilder.build()
    }

    fun getProxyLogLiveData() = proxyLogLiveData
}