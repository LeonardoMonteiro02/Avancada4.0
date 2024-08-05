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
        maven {
            url = uri("https://maven.pkg.github.com/LeonardoMonteiro02/Avancada3.0")
            credentials {
                username = "LeonardoMonteiro02"
                password = "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"
            }
        }
    }
}

rootProject.name = "Avanacada4.0"
include(":app")
 