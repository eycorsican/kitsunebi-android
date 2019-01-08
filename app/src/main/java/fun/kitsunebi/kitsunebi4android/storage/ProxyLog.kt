package `fun`.kitsunebi.kitsunebi4android.storage

import `fun`.kitsunebi.kitsunebi4android.common.SingletonHolder
import android.content.Context
import androidx.paging.DataSource
import androidx.room.*

public const val PROXY_LOG_DB_NAME = "proxy_log.sqlite3"

@Entity(tableName = "proxy_log", indices = arrayOf(Index(value = ["record_type", "end_time"])))
data class ProxyLog(
        @PrimaryKey(autoGenerate = true) var id: Int,
        @ColumnInfo(name = "target") var target: String?,
        @ColumnInfo(name = "tag") var tag: String?,
        @ColumnInfo(name = "start_time") var startTime: Long?,
        @ColumnInfo(name = "end_time") var endTime: Long?,
        @ColumnInfo(name = "upload_bytes") var uploadBytes: Int?,
        @ColumnInfo(name = "download_bytes") var downloadBytes: Int?,
        @ColumnInfo(name = "record_type") var recordType: Int?,
        @ColumnInfo(name = "dns_query_type") var dnsQueryType: Int?,
        @ColumnInfo(name = "dns_request") var dnsRequest: String?,
        @ColumnInfo(name = "dns_response") var dnsResponse: String?,
        @ColumnInfo(name = "dns_num_ips") var dnsNumIPs: Int?
)

@Dao
interface ProxyLogDao {
    @Query("SELECT * FROM proxy_log")
    fun getAll(): List<ProxyLog>

    @Query("SELECT * FROM proxy_log ORDER BY end_time DESC")
    fun getAllPaged(): DataSource.Factory<Int, ProxyLog>

    @Query("SELECT * FROM proxy_log WHERE record_type != 1 ORDER BY end_time DESC")
    fun getAllNonDnsPaged(): DataSource.Factory<Int, ProxyLog>

    @Insert
    fun insertAll(proxyLogs: ProxyLog)

    @Delete
    fun delete(proxyLog: ProxyLog)

    @Query("DELETE FROM proxy_log")
    fun deleteAll()

    @Query("SELECT COUNT(1) FROM proxy_log")
    fun getAllCount(): Int
}

@Database(entities = arrayOf(ProxyLog::class), version = 5)
abstract class ProxyLogDatabase : RoomDatabase() {
    abstract fun proxyLogDao(): ProxyLogDao

    companion object : SingletonHolder<ProxyLogDatabase, Context>({
        Room.databaseBuilder(it.applicationContext,
                ProxyLogDatabase::class.java, PROXY_LOG_DB_NAME)
                .fallbackToDestructiveMigration()
                .enableMultiInstanceInvalidation()
                .build()
    })
}