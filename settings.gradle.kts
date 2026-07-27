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
        maven(url = "https://jitpack.io")
        maven(url = "https://dl.google.com/dl/android/maven2")
    }
}
rootProject.name = "Camera Studio Photo Editor"
include(":app")
// base-application chưa gắn ở giai đoạn build core feature (xem M0 trong kế hoạch) — gắn lại ở M8.
// include(":base-application")
