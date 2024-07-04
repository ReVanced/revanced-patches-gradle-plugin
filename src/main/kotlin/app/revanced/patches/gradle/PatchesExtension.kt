package app.revanced.patches.gradle

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

@Suppress("unused")
abstract class PatchesExtension @Inject constructor(objectFactory: ObjectFactory) {
    /**
     * The path to the extensions project relative to the root project.
     *
     * Used by the patches plugin to consume the extension artifacts.
     *
     * Defaults to `:extensions`.
     */
    abstract val extensionsProjectPath: Property<String>

    internal val about = objectFactory.newInstance(About::class.java)

    fun about(block: About.() -> Unit) {
        about.block()
    }

    init {
        extensionsProjectPath.convention(":extensions")
    }

    /**
     * About information for the project.
     *
     * Used by the patches plugin to create the manifest file and set up the publication of the patches project.
     */
    abstract class About @Inject constructor(project: Project) {
        abstract val name: Property<String>
        abstract val description: Property<String>
        abstract val source: Property<String>
        abstract val author: Property<String>
        abstract val contact: Property<String>
        abstract val website: Property<String>
        abstract val license: Property<String>
        internal abstract val version: Property<String>
        internal abstract val timestamp: Property<Long>

        init {
            version.convention(project.version.toString())
            timestamp.convention(System.currentTimeMillis())
        }
    }
}
