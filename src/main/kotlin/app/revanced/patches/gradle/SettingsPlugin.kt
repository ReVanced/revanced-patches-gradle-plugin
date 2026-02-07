package app.revanced.patches.gradle

import org.gradle.api.Plugin
import org.gradle.api.UnknownProjectException
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import java.net.URI
import javax.inject.Inject

@Suppress("unused")
abstract class SettingsPlugin @Inject constructor(
    private val objectFactory: ObjectFactory,
) : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.configureDependencies()

        val extension = settings.extensions.create("settings", SettingsExtension::class.java)

        settings.gradle.settingsEvaluated {
            settings.gradle.sharedServices.registerIfAbsent(
                "settingsExtensionProvider",
                SettingsExtensionProvider::class.java,
            ) {
                it.parameters.apply {
                    defaultNamespace = extension.extensions.defaultNamespace
                    proguardFiles = extension.extensions.proguardFiles
                }
            }
        }

        settings.configureProjects(extension)
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
                repository.name = "githubPackages"
                // A repository must be specified. "revanced" is a dummy.
                repository.url = URI("https://maven.pkg.github.com/revanced/revanced")
                repository.credentials(PasswordCredentials::class.java)
            }
        }
    }

    /**
     * Adds the required plugins to the patches and extension projects.
     */
    private fun Settings.configureProjects(extension: SettingsExtension) {
        // region Include the projects

        val extensionsProjectPath = extension.extensions.projectsPath

        if (extensionsProjectPath != null) {
            objectFactory.fileTree().from(rootDir.resolve(extensionsProjectPath)).matching {
                it.include("**/build.gradle.kts")
            }.forEach {
                include(it.relativeTo(rootDir).parentFile.toPath().joinToString(":"))
            }
        }

        include(extension.patchesProjectPath)

        // endregion

        // region Apply the plugins

        gradle.rootProject { rootProject ->
            if (extensionsProjectPath != null) {
                val extensionsProject = try {
                    rootProject.project(extensionsProjectPath)
                } catch (e: UnknownProjectException) {
                    null
                }

                extensionsProject?.subprojects { extensionProject ->
                    if (
                        extensionProject.buildFile.exists() &&
                        !extensionProject.parent!!.plugins.hasPlugin(ExtensionPlugin::class.java)
                    ) {
                        extensionProject.pluginManager.apply(ExtensionPlugin::class.java)
                    }
                }
            }

            // Needs to be applied after the extension plugin
            // so that their extensionConfiguration is available for consumption.
            rootProject.project(extension.patchesProjectPath).pluginManager.apply(PatchesPlugin::class.java)
        }

        // endregion
    }
}
