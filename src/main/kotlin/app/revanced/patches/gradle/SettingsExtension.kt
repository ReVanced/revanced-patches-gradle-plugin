package app.revanced.patches.gradle

import org.gradle.api.provider.Property

abstract class SettingsExtension {
    /**
     * The path to the patches project relative to the root project.
     *
     * Used by the settings plugin to include the patches project
     * and apply the patches plugin to the patches project.
     *
     * Defaults to `patches`.
     */
    abstract val patchesProjectPath: Property<String>

    /**
     * The path to the extensions project relative to the root project.
     *
     * Used by the settings plugin to include the extensions project
     * and apply the extensions plugin to the extensions project.
     *
     * Defaults to `extensions`.
     */
    abstract val extensionsProjectPath: Property<String>

    init {
        patchesProjectPath.convention("patches")
        extensionsProjectPath.convention("extensions")
    }
}
