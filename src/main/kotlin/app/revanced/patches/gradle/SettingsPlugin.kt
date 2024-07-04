package app.revanced.patches.gradle

import org.gradle.api.Plugin
import org.gradle.api.UnknownProjectException
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import java.net.URI
import javax.inject.Inject

@Suppress("unused")
abstract class SettingsPlugin @Inject constructor(
    private val objectFactory: ObjectFactory,
) : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val extension = settings.extensions.create("settings", SettingsExtension::class.java)

        settings.configureDependencies()
        settings.configureIncludeProjects(extension)
        settings.configurePlugins(extension)
    }

    /**
     * Add required repositories.
     */
    private fun Settings.configureDependencies() {
        @Suppress("UnstableApiUsage")
        dependencyResolutionManagement.repositories.apply {
            mavenCentral()
            google()
            maven { repository ->
                // A repository must be specified. "registry" is a dummy.
                repository.url = URI("https://maven.pkg.github.com/revanced/registry")
                repository.credentials {
                    it.username = providers.gradleProperty("gpr.user")
                        .orElse(System.getenv("GITHUB_ACTOR")).get()
                    it.password = providers.gradleProperty("gpr.key")
                        .orElse(System.getenv("GITHUB_TOKEN")).get()
                }
            }
        }
    }

    /**
     * Add the patches and extension projects to the root project.
     */
    private fun Settings.configureIncludeProjects(extension: SettingsExtension) {
        include(extension.patchesProjectPath.get())

        objectFactory.fileTree().from(settingsDir.resolve(extension.extensionsProjectPath.get())).matching {
            it.include("**/build.gradle.kts")
        }.forEach {
            include(it.relativeTo(settingsDir).toPath().joinToString(":"))
        }
    }

    /**
     * Adds the required plugins to the patches and extension projects.
     */
    private fun Settings.configurePlugins(extension: SettingsExtension) {
        gradle.rootProject { rootProject ->
            rootProject.project(extension.patchesProjectPath.get()).pluginManager.apply(PatchesPlugin::class.java)

            try {
                rootProject.project(extension.extensionsProjectPath.get())
            } catch (e: UnknownProjectException) {
                return@rootProject
            }.let { extensionsProject ->
                extensionsProject.subprojects { extensionProject ->
                    if (extensionProject.parent != extensionsProject) return@subprojects

                    extensionProject.pluginManager.apply(ExtensionPlugin::class.java)
                }
            }
        }
    }
}
