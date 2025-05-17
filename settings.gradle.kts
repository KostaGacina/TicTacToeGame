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

//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
//    versionCatalogs{
//        create("libs"){
//            version("okhttp", "4.12.0" )
//            library("okhttp-ws", "com.squareup.okhttp3", "okhttp-ws").versionRef("okhttp")
//        }
//    }
}

rootProject.name = "TicTacToeGame"
include(":app")
