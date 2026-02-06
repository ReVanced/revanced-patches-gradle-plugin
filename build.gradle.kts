@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.vanniktech.mavenPublish)
    `java-gradle-plugin`
}

group = "app.revanced"

dependencies {
    implementation(libs.android.application)
    implementation(libs.guava)
    implementation(libs.kotlin)
    implementation(libs.kotlin.android)
    implementation(libs.vanniktech.mavenPublish)

    implementation(gradleApi())
    implementation(gradleKotlinDsl())
}

kotlin {
    abiValidation {
        enabled = true
    }

    compilerOptions {
        jvmToolchain(17)
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

mavenPublishing {
    publishing {
        repositories {
            maven {
                name = "githubPackages"
                url = uri("https://maven.pkg.github.com/revanced/revanced-patches-gradle-plugin")
                credentials(PasswordCredentials::class)
            }
        }
    }

    signAllPublications()
    extensions.getByType<SigningExtension>().useGpgCmd()

    coordinates(group.toString(), project.name, version.toString())
}
