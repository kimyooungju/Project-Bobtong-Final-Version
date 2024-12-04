pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        repositories {
            google() // Google's Maven repository
            mavenCentral()
            maven("https://repository.map.naver.com/archive/maven")
        }
}

rootProject.name = "Project_Bobtong"
include(":app")
 