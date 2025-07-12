pluginManagement {
    repositories {
        // ✅ Myket repo
        maven {
            url = uri("https://maven.myket.ir")
        }

        // ✅ Official repos with restricted group access
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }

        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ✅ Myket repo
        maven {
            url = uri("https://maven.myket.ir")
        }

        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "accounting_app"
include(":app")
