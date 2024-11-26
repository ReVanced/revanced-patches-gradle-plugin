package app.revanced.patches.gradle

open class ExtensionExtension {
    /**
     * The name of the extension.
     *
     * The name is the full resource path of the extension in the final patches file.
     * Example: `extensions/extension.rve`.
     */
    var name: String? = null
}
