plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
                outputFileName = "vwatek-apply.js"
            }
            binaries.executable()
        }
    }
    
    sourceSets {
        jsMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.html.core)
            implementation(compose.runtime)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
