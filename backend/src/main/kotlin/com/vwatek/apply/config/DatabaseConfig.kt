package com.vwatek.apply.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.vwatek.apply.db.tables.*
import com.vwatek.apply.routes.OrganizationSettingsTable
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    
    // Load secrets from properties file or environment
    private val secrets: Properties by lazy { loadSecrets() }
    
    private fun loadSecrets(): Properties {
        val props = Properties()
        
        // Try multiple locations for secrets.properties
        val possiblePaths = listOf(
            "secrets.properties",
            "../secrets.properties",
            System.getProperty("user.dir") + "/secrets.properties",
            System.getProperty("user.dir") + "/../secrets.properties"
        )
        
        for (path in possiblePaths) {
            val secretsFile = File(path)
            if (secretsFile.exists()) {
                logger.info("Loading database configuration from ${secretsFile.absolutePath}")
                secretsFile.inputStream().use { props.load(it) }
                return props
            }
        }
        
        logger.info("No secrets.properties found in any location, using environment variables")
        return props
    }
    
    private fun getConfig(key: String, default: String = ""): String {
        // Environment variables take precedence over properties file
        return System.getenv(key) ?: secrets.getProperty(key, default)
    }
    
    // Railway PostgreSQL configuration (via DATABASE_URL or individual env vars)
    private val DB_HOST by lazy { getConfig("DB_HOST", "mainline.proxy.rlwy.net") }
    private val DB_PORT by lazy { getConfig("DB_PORT", "56544").toInt() }
    private val DB_NAME by lazy { getConfig("DB_NAME", "railway") }
    private val DB_USER by lazy { getConfig("DB_USER", "postgres") }
    private val DB_PASSWORD by lazy { getConfig("DB_PASSWORD", "") }
    
    // Local PostgreSQL fallback configuration
    private val LOCAL_HOST by lazy { getConfig("LOCAL_DB_HOST", "localhost") }
    private val LOCAL_PORT by lazy { getConfig("LOCAL_DB_PORT", "5432").toInt() }
    private val LOCAL_DATABASE by lazy { getConfig("LOCAL_DB_NAME", "vwatek_apply") }
    private val LOCAL_USER by lazy { getConfig("LOCAL_DB_USER", "postgres") }
    private val LOCAL_PASSWORD by lazy { getConfig("LOCAL_DB_PASSWORD", "") }
    
    private var dataSource: HikariDataSource? = null
    
    fun init() {
        // Try DATABASE_PUBLIC_URL first (works both inside and outside Railway network)
        val publicUrl = System.getenv("DATABASE_PUBLIC_URL")
        // Then try DATABASE_URL (Railway internal network)
        val databaseUrl = System.getenv("DATABASE_URL")
        
        val connected = when {
            publicUrl != null -> {
                logger.info("Found DATABASE_PUBLIC_URL, connecting to Railway PostgreSQL (public)...")
                tryConnectWithUrl(publicUrl)
            }
            databaseUrl != null -> {
                logger.info("Found DATABASE_URL, connecting to Railway PostgreSQL (internal)...")
                tryConnectWithUrl(databaseUrl)
            }
            else -> false
        }
        
        if (!connected) {
            logger.warn("Railway URLs not available or failed, trying explicit config...")
            if (!tryConnectPostgres(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, useSSL = true)) {
                logger.warn("Remote PostgreSQL failed, falling back to local PostgreSQL")
                if (!tryConnectPostgres(LOCAL_HOST, LOCAL_PORT, LOCAL_DATABASE, LOCAL_USER, LOCAL_PASSWORD, useSSL = false)) {
                    throw RuntimeException("Unable to connect to any database")
                }
            }
        }
        
        // Run migrations
        runMigrations()
    }
    
    private fun tryConnectWithUrl(url: String): Boolean {
        return try {
            // Railway DATABASE_URL format: postgresql://user:pass@host:port/db
            // JDBC needs: jdbc:postgresql://host:port/db
            val jdbcUrl = if (url.startsWith("jdbc:")) url
                else "jdbc:${url.replace("postgres://", "postgresql://")}"
            
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 5
                minimumIdle = 1
                idleTimeout = 30000
                connectionTimeout = 15000
                maxLifetime = 1800000
                leakDetectionThreshold = 60000
                connectionTestQuery = "SELECT 1"
            }
            
            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)
            transaction { exec("SELECT 1") }
            logger.info("✅ Connected to PostgreSQL via DATABASE_URL")
            true
        } catch (e: Exception) {
            logger.error("❌ DATABASE_URL connection failed: ${e.message}")
            dataSource?.close()
            dataSource = null
            false
        }
    }
    
    private fun tryConnectPostgres(
        host: String, port: Int, database: String,
        user: String, password: String, useSSL: Boolean
    ): Boolean {
        return try {
            logger.info("Connecting to PostgreSQL at $host:$port/$database...")
            val sslParam = if (useSSL) "?sslmode=require" else ""
            
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:postgresql://$host:$port/$database$sslParam"
                driverClassName = "org.postgresql.Driver"
                username = user
                this.password = password
                maximumPoolSize = 5
                minimumIdle = 1
                idleTimeout = 30000
                connectionTimeout = 15000
                maxLifetime = 1800000
                leakDetectionThreshold = 60000
                connectionTestQuery = "SELECT 1"
            }
            
            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)
            transaction { exec("SELECT 1") }
            logger.info("✅ Connected to PostgreSQL at $host:$port")
            true
        } catch (e: Exception) {
            logger.error("❌ PostgreSQL connection to $host:$port failed: ${e.message}")
            dataSource?.close()
            dataSource = null
            false
        }
    }
    
    private fun runMigrations() {
        logger.info("Running database migrations...")
        
        // Run Flyway versioned migrations first
        dataSource?.let { ds ->
            try {
                FlywayMigration.migrate(ds)
            } catch (e: Exception) {
                logger.warn("Flyway migration failed, falling back to Exposed SchemaUtils: ${e.message}")
            }
        }
        
        // Exposed SchemaUtils as safety net — creates any tables Flyway missed
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                // Core tables
                UsersTable,
                ResumesTable,
                ResumeVersionsTable,
                ResumeAnalysesTable,
                CoverLettersTable,
                InterviewSessionsTable,
                InterviewQuestionsTable,
                SettingsTable,
                // Phase 2: Job Tracker tables
                JobApplicationsTable,
                JobApplicationNotesTable,
                JobApplicationRemindersTable,
                JobApplicationInterviewsTable,
                JobApplicationStatusHistoryTable,
                JobApplicationDocumentsTable,
                // Phase 3: NOC tables
                NOCCodesTable,
                NOCMainDutiesTable,
                NOCEmploymentRequirementsTable,
                NOCAdditionalInfoTable,
                NOCSkillsTable,
                NOCProvincialDemandTable,
                NOCImmigrationPathwaysTable,
                UserNOCMatchesTable,
                // Sync tables
                DevicesTable,
                SyncLogsTable,
                SyncMetadataTable,
                DeviceSyncStateTable,
                OfflineOperationsTable,
                SyncConflictsTable,
                UserDataRegionsTable,
                ChangeFeedTable,
                // Privacy/PIPEDA tables
                ConsentRecordsTable,
                ConsentAuditLogTable,
                DataAccessRequestsTable,
                DataRetentionTable,
                DataSharingLogTable,
                // Phase 4: Subscription tables
                SubscriptionsTable,
                PaymentsTable,
                StripeCustomersTable,
                UsageTrackingTable,
                SubscriptionEventsTable,
                StripeWebhookEventsTable,
                PriceConfigurationTable,
                PromotionsTable,
                PromotionRedemptionsTable,
                // Phase 4: Salary Intelligence tables
                SalaryDataTable,
                SalaryComparisonHistoryTable,
                JobOffersTable,
                NegotiationSessionsTable,
                NegotiationMessagesTable,
                SalaryDataImportLogTable,
                SavedSalarySearchesTable,
                // Phase 5: Enterprise/Organization tables
                OrganizationsTable,
                OrganizationSettingsTable,
                OrganizationMembersTable,
                OrganizationInvitationsTable,
                OrganizationTemplatesTable,
                OrganizationActivityLogTable,
                LinkedInProfilesTable,
                LinkedInAnalysisHistoryTable,
                SSOSessionsTable,
                AdminReportsTable,
                OrganizationSubscriptionHistoryTable
            )
        }
        
        logger.info("✅ Database migrations completed!")
    }
    
    fun close() {
        dataSource?.close()
    }
}
