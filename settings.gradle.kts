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
        google()
        mavenCentral()
        // Pleos SDK(ai.pleos.playground:Vehicle)는 Playground 콘솔이 안내하는 저장소를 여기에 추가한다.
    }
}

rootProject.name = "pleos-fe"

include(":core-api")
include(":core-android")
include(":phone-app")
include(":vehicle-app")
