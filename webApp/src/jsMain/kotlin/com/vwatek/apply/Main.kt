package com.vwatek.apply

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vwatek.apply.di.platformModule
import com.vwatek.apply.di.sharedModule
import com.vwatek.apply.ui.App
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.koin.core.context.startKoin

fun main() {
    initKoin()
    
    renderComposable(rootElementId = "root") {
        App()
    }
}

private fun initKoin() {
    startKoin {
        modules(sharedModule, platformModule())
    }
}
