pluginManagement {
    repositories {
        google()             // for Android Gradle Plugin
        mavenCentral()       // for dependencies
        gradlePluginPortal() // for Gradle plugins
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()       // for Play Services / Firebase
        mavenCentral() // for standard dependencies
    }
}

rootProject.name = "Parking finder"
include(":app")
