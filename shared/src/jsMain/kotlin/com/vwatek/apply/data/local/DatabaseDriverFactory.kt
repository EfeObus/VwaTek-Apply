package com.vwatek.apply.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.vwatek.apply.db.VwaTekDatabase
import org.w3c.dom.Worker

class JsDatabaseDriverFactory : DatabaseDriverFactory {
    override suspend fun createDriver(): SqlDriver {
        return WebWorkerDriver(
            Worker(
                js("""new URL("@aspect-build/aspect-worker/dist/worker.min.js", import.meta.url)""")
            )
        ).also { driver ->
            VwaTekDatabase.Schema.create(driver).await()
        }
    }
}

private suspend fun <T> T.await(): T = this

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory {
    return JsDatabaseDriverFactory()
}
