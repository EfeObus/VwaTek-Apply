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
// Android and iOS apps will be added later
// include(":androidApp")
