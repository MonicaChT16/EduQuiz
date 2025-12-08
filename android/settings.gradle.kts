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
    }
}

rootProject.name = "EduQuiz"
include(
    ":app",
    ":core",
    ":data",
    ":domain",
    ":feature-auth",
    ":feature-pack",
    ":feature-exam",
    ":feature-profile",
    ":feature-store",
    ":feature-ranking"
)
