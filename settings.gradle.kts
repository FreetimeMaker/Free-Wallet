pluginManagement {
    repositories {
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
        google()
        mavenCentral()

        val properties = java.util.Properties()
        val localProps = File(rootDir.absolutePath, "local.properties")
        if (localProps.exists()) {
            properties.load(localProps.inputStream())
        } else {
            println("local.properties not found")
        }
        maven {
            url = uri("https://maven.pkg.github.com/trustwallet/wallet-core")
            credentials {
                username = properties.getProperty("gpr.user") ?: System.getenv("GITHUB_USER")
                password = properties.getProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "FreetimeWallet"
include(":app")
