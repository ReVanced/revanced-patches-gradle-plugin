import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.binary.compatibility.validator)
    `java-gradle-plugin`
    `maven-publish`
    signing
}

group = "app.revanced"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(libs.android.application)
    implementation(libs.binary.compatibility.validator)
    implementation(libs.guava)
    implementation(libs.kotlin)
    implementation(libs.kotlin.android)

    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

gradlePlugin {
    website = "https://revanced.app"
    vcsUrl = "ssh://git@github.com:revanced/revanced-patches-gradle-plugin.git"

    plugins {
        create("patchesSettingsPlugin") {
            id = "app.revanced.patches"
            implementationClass = "app.revanced.patches.gradle.SettingsPlugin"
            version = version
            description = "Plugin to configure a ReVanced Patches project."
            displayName = "ReVanced Patches Gradle settings plugin"
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/revanced/revanced-patches-gradle-plugin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    useGpgCmd()

    sign(publishing.publications)
}
