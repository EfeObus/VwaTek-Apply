package com.vwatek.apply.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.vwatek.apply.db.VwaTekDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = VwaTekDatabase.Schema,
            name = "vwatek_apply.db"
        )
    }
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    return IosDatabaseDriverFactory()
}
