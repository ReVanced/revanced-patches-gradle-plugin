package app.revanced.patches.gradle

import org.gradle.api.provider.Property

abstract class ExtensionExtension {
    /**
     * The name of the extension.
     *
     * The name is the full resource path of the extension in the final patches file.
     * Example: `extensions/extension.rve`.
     */
    abstract val name: Property<String>
}
