package com.vwatek.apply.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.vwatek.apply.db.tables.*
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
    
    // Cloud Run detection - check for K_SERVICE env var which Cloud Run sets
    private val isRunningOnCloudRun by lazy { System.getenv("K_SERVICE") != null }
    
    // Cloud SQL Instance Connection Name (format: project:region:instance)
    private val CLOUD_SQL_INSTANCE by lazy { getConfig("CLOUD_SQL_INSTANCE", "vwatek-apply:us-central1:vwatekapply") }
    
    // Google Cloud SQL MySQL Configuration
    private val CLOUD_SQL_HOST by lazy { getConfig("CLOUD_SQL_HOST", "34.134.196.247") }
    private val CLOUD_SQL_PORT by lazy { getConfig("CLOUD_SQL_PORT", "3306").toInt() }
    private val CLOUD_SQL_DATABASE by lazy { getConfig("CLOUD_SQL_DATABASE", "vwatekapply") }
    private val CLOUD_SQL_USER by lazy { getConfig("CLOUD_SQL_USER", "root") }
    private val CLOUD_SQL_PASSWORD by lazy { getConfig("CLOUD_SQL_PASSWORD", "") }
    
    // Local MySQL fallback configuration
    private val LOCAL_HOST by lazy { getConfig("LOCAL_MYSQL_HOST", "localhost") }
    private val LOCAL_PORT by lazy { getConfig("LOCAL_MYSQL_PORT", "3306").toInt() }
    private val LOCAL_DATABASE by lazy { getConfig("LOCAL_MYSQL_DATABASE", "vwatekapply") }
    private val LOCAL_USER by lazy { getConfig("LOCAL_MYSQL_USER", "root") }
    private val LOCAL_PASSWORD by lazy { getConfig("LOCAL_MYSQL_PASSWORD", "") }
    
    private var dataSource: HikariDataSource? = null
    private var isCloudConnected = false
    
    fun init() {
        // Try Cloud SQL first, fall back to local
        if (!tryConnectCloudSQL()) {
            logger.warn("Cloud SQL connection failed, falling back to local MySQL")
            if (!tryConnectLocalMySQL()) {
                logger.error("Both Cloud SQL and local MySQL connections failed!")
                throw RuntimeException("Unable to connect to any database")
            }
        }
        
        // Run migrations
        runMigrations()
    }
    
    private fun tryConnectCloudSQL(): Boolean {
        return try {
            if (isRunningOnCloudRun) {
                logger.info("Running on Cloud Run - connecting via Unix socket to $CLOUD_SQL_INSTANCE...")
                connectViaUnixSocket()
            } else {
                logger.info("Attempting to connect to Google Cloud SQL at $CLOUD_SQL_HOST:$CLOUD_SQL_PORT...")
                connectViaTcp()
            }
        } catch (e: Exception) {
            logger.error("❌ Cloud SQL connection failed: ${e.message}")
            dataSource?.close()
            dataSource = null
            false
        }
    }
    
    private fun connectViaUnixSocket(): Boolean {
        val socketPath = "/cloudsql/$CLOUD_SQL_INSTANCE"
        logger.info("Using socket path: $socketPath")
        
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql:///$CLOUD_SQL_DATABASE?cloudSqlInstance=$CLOUD_SQL_INSTANCE&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = CLOUD_SQL_USER
            password = CLOUD_SQL_PASSWORD
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000
            connectionTimeout = 30000  // Longer timeout for Cloud SQL
            maxLifetime = 1800000
            connectionTestQuery = "SELECT 1"
        }
        
        dataSource = HikariDataSource(config)
        Database.connect(dataSource!!)
        
        // Test connection
        transaction {
            exec("SELECT 1")
        }
        
        isCloudConnected = true
        logger.info("✅ Successfully connected to Google Cloud SQL via Unix socket!")
        return true
    }
    
    private fun connectViaTcp(): Boolean {
        val config = createHikariConfig(
            host = CLOUD_SQL_HOST,
            port = CLOUD_SQL_PORT,
            database = CLOUD_SQL_DATABASE,
            user = CLOUD_SQL_USER,
            password = CLOUD_SQL_PASSWORD,
            useSSL = true  // Cloud SQL requires SSL for TCP
        )
        
        dataSource = HikariDataSource(config)
        Database.connect(dataSource!!)
        
        // Test connection
        transaction {
            exec("SELECT 1")
        }
        
        isCloudConnected = true
        logger.info("✅ Successfully connected to Google Cloud SQL via TCP!")
        return true
    }
    
    private fun tryConnectLocalMySQL(): Boolean {
        return try {
            logger.info("Attempting to connect to local MySQL at $LOCAL_HOST:$LOCAL_PORT...")
            
            val config = createHikariConfig(
                host = LOCAL_HOST,
                port = LOCAL_PORT,
                database = LOCAL_DATABASE,
                user = LOCAL_USER,
                password = LOCAL_PASSWORD
            )
            
            dataSource = HikariDataSource(config)
            Database.connect(dataSource!!)
            
            // Test connection
            transaction {
                exec("SELECT 1")
            }
            
            isCloudConnected = false
            logger.info("✅ Successfully connected to local MySQL!")
            true
        } catch (e: Exception) {
            logger.error("❌ Local MySQL connection failed: ${e.message}")
            dataSource?.close()
            dataSource = null
            false
        }
    }
    
    private fun createHikariConfig(
        host: String,
        port: Int,
        database: String,
        user: String,
        password: String,
        useSSL: Boolean = false
    ): HikariConfig {
        val sslParams = if (useSSL) {
            "useSSL=true&requireSSL=true&sslMode=REQUIRED"
        } else {
            "useSSL=false"
        }
        
        return HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://$host:$port/$database?$sslParams&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = user
            this.password = password
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 30000
            connectionTimeout = 10000
            maxLifetime = 1800000
            
            // Connection validation
            connectionTestQuery = "SELECT 1"
        }
    }
    
    private fun runMigrations() {
        logger.info("Running database migrations...")
        
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                ResumesTable,
                ResumeVersionsTable,
                ResumeAnalysesTable,
                CoverLettersTable,
                InterviewSessionsTable,
                InterviewQuestionsTable,
                SettingsTable
            )
        }
        
        logger.info("✅ Database migrations completed!")
    }
    
    fun isUsingCloudSQL(): Boolean = isCloudConnected
    
    fun close() {
        dataSource?.close()
    }
}
