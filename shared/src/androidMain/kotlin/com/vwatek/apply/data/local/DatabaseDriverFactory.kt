package com.vwatek.apply.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vwatek.apply.db.VwaTekDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VwaTekDatabase.Schema,
            context = context,
            name = "vwatek_apply.db",
            callback = object : AndroidSqliteDriver.Callback(VwaTekDatabase.Schema) {
                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // Migrate existing databases that don't have userId column
                    try {
                        db.execSQL("ALTER TABLE Resume ADD COLUMN userId TEXT")
                    } catch (e: Exception) {
                        // Column already exists, ignore
                    }
                }
            }
        )
    }
}

private var appContext: Context? = null

fun initDatabaseContext(context: Context) {
    appContext = context.applicationContext
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    return AndroidDatabaseDriverFactory(
        appContext ?: throw IllegalStateException("Call initDatabaseContext() first")
    )
}
