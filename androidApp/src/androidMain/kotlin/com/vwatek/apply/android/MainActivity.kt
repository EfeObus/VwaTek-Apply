package com.vwatek.apply.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.vwatek.apply.android.ui.VwaTekApp
import com.vwatek.apply.android.ui.theme.VwaTekApplyTheme

class MainActivity : ComponentActivity() {
    
    /** The deep link URI from the launch or new intent. */
    private val deepLinkUri = mutableStateOf<String?>(null)
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle initial deep link
        handleDeepLink(intent)
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            VwaTekApplyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VwaTekApp(
                        windowSizeClass = windowSizeClass,
                        deepLinkUri = deepLinkUri.value,
                        onDeepLinkConsumed = { deepLinkUri.value = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data?.toString()
        if (uri != null && uri.startsWith("vwatekapply://")) {
            deepLinkUri.value = uri
        }
    }
}
