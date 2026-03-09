package com.vwatek.apply.config

import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Flyway database migration manager.
 * 
 * Runs versioned SQL migrations from src/main/resources/db/migration/
 * Migration naming: V{version}__{description}.sql (e.g., V2__add_indexes.sql)
 * 
 * For existing databases: call baseline() first to mark V1 as already applied.
 * For new databases: all migrations run from V1 forward.
 */
object FlywayMigration {
    private val logger = LoggerFactory.getLogger(FlywayMigration::class.java)
    
    /**
     * Run all pending Flyway migrations.
     * Calls baseline if this is the first time Flyway runs on an existing DB.
     */
    fun migrate(dataSource: DataSource) {
        logger.info("Starting Flyway database migrations...")
        
        try {
            val flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)  // Auto-baseline existing databases
                .baselineVersion("1")     // Treat V1 as baseline for existing DBs
                .validateOnMigrate(true)
                .cleanDisabled(true)      // Never allow clean in production
                .table("flyway_schema_history")
                .load()
            
            val result = flyway.migrate()
            
            if (result.migrationsExecuted > 0) {
                logger.info(" Flyway: ${result.migrationsExecuted} migration(s) applied successfully")
                result.migrations.forEach { migration ->
                    logger.info("  Applied: ${migration.version} - ${migration.description}")
                }
            } else {
                logger.info(" Flyway: Database is up to date (no pending migrations)")
            }
        } catch (e: Exception) {
            logger.error(" Flyway migration failed: ${e.message}", e)
            throw e
        }
    }
}
