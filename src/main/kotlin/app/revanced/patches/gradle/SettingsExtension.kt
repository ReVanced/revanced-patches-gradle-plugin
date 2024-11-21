package app.revanced.patches.gradle

open class SettingsExtension {
    /**
     * The path to the patches project.
     */
    var patchesProjectPath = "patches"

    // Need to rename, otherwise it will conflict with the `getExtensions` property from ExtensionAware.
    @get:JvmName("getExtensionsExtension")
    val extensions = ExtensionsExtension()

    fun extensions(block: ExtensionsExtension.() -> Unit) {
        ExtensionsExtension().apply(block)
    }

    class ExtensionsExtension {
        /**
         * The path to the project containing the extension projects.
         */
        var projectPath: String? = "extensions"
    }
}
