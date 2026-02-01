package com.vwatek.apply.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory() {
    suspend fun createDriver(): SqlDriver
}
