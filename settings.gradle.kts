pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VwaTekApply"

include(":shared")
include(":webApp")
include(":backend")
include(":androidApp")
// Note: iOS app is built via Xcode, not Gradle
