pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "mediator"
include("core")
include("desktop")
include("pick-or-not")
include("jetbrains-theme")