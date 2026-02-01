package com.vwatek.apply.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.vwatek.apply.db.VwaTekDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = VwaTekDatabase.Schema,
            context = context,
            name = "vwatek_apply.db"
        )
    }
}
