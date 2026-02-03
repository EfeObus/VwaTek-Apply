package com.vwatek.apply.data.local

import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactory {
    suspend fun createDriver(): SqlDriver
}

expect fun createDatabaseDriverFactory(): DatabaseDriverFactory
