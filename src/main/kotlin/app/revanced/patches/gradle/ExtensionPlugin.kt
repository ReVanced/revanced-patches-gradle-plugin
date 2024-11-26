package app.revanced.patches.gradle

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPluginWrapper
import kotlin.io.path.Path
import kotlin.io.path.pathString

@Suppress("unused")
abstract class ExtensionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("extension", ExtensionExtension::class.java)

        val settingsExtensionProvider = project.gradle.sharedServices.registrations
            .findByName("settingsExtensionProvider")!!.service.get() as SettingsExtensionProvider

        project.configureAndroid(settingsExtensionProvider)
        project.configureArtifactSharing(extension)
    }

    /**
     * Setup sharing the extension dex file with the consuming patches project.
     */
    private fun Project.configureArtifactSharing(extension: ExtensionExtension) {
        val androidExtension = extensions.getByType<BaseAppModuleExtension>()
        val syncExtensionTask = tasks.register<Sync>("syncExtension") {
            val dexTaskName = if (androidExtension.buildTypes.getByName("release").isMinifyEnabled) {
                "minifyReleaseWithR8"
            } else {
                "mergeDexRelease"
            }

            val dexTask = tasks.getByName(dexTaskName)

            dependsOn(dexTask)

            val extensionName = if (extension.name != null) {
                Path(extension.name!!)
            } else {
                projectDir.resolveSibling(project.name + ".rve").relativeTo(rootDir).toPath()
            }

            from(dexTask.outputs.files.asFileTree.matching { include("**/*.dex") })
            into(layout.buildDirectory.dir("revanced/${extensionName.parent.pathString}"))

            rename { extensionName.fileName.toString() }
        }

        configurations.create("extensionConfiguration").apply {
            isCanBeResolved = false
            isCanBeConsumed = true

            outgoing.artifact(layout.buildDirectory.dir("revanced")) {
                it.builtBy(syncExtensionTask)
            }
        }
    }

    /**
     * Set up the Android plugin for the extension project.
     */
    private fun Project.configureAndroid(settingsExtensionProvider: SettingsExtensionProvider) {
        pluginManager.apply {
            apply(AppPlugin::class.java)
            apply(KotlinAndroidPluginWrapper::class.java)
        }

        extensions.configure(BaseAppModuleExtension::class.java) {
            it.apply {
                compileSdk = 34
                namespace = settingsExtensionProvider.parameters.defaultNamespace

                defaultConfig {
                    minSdk = 23
                    multiDexEnabled = false
                }

                buildTypes {
                    release {
                        isMinifyEnabled = settingsExtensionProvider.parameters.proguardFiles.isNotEmpty()

                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            *settingsExtensionProvider.parameters.proguardFiles.toTypedArray(),
                        )
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                this as ExtensionAware
                this.extensions.configure<KotlinJvmOptions>("kotlinOptions") { options ->
                    options.jvmTarget = JavaVersion.VERSION_17.toString()
                }
            }
        }
    }
}
