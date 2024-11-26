package app.revanced.patches.gradle

import com.android.tools.build.apkzlib.zip.ZFile
import com.android.tools.r8.CompilationMode
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.android.tools.r8.utils.ArchiveResourceProvider
import kotlinx.validation.BinaryCompatibilityValidatorPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownProjectException
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import java.io.File

@Suppress("unused")
abstract class PatchesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("patches", PatchesExtension::class.java)

        project.configureDependencies()
        project.configureKotlin()
        project.configureJava()
        project.configureBinaryCompatibilityValidator()
        project.configureConsumeExtensions(extension)
        project.configureJarTask(extension)
        project.configurePublishing(extension)
        project.configureSigning()
    }

    /**
     * Adds the dependencies ReVanced Patcher and SMALI to the project.
     * The versions are fetched from the version catalog by the respective project.
     */
    private fun Project.configureDependencies() {
        afterEvaluate {
            val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

            operator fun String.invoke(versionAlias: String) = dependencies.add(
                "implementation",
                "$this:" + catalog.findVersion(versionAlias).orElseThrow {
                    IllegalArgumentException("Version with alias $versionAlias not found in version catalog")
                },
            )

            "app.revanced:revanced-patcher"("revanced-patcher")
            "com.android.tools.smali:smali"("smali")
        }
    }

    /**
     * Configures the Kotlin plugin with JVM 11 as the target because JVM 11 is the target on Android.
     */
    private fun Project.configureKotlin() {
        pluginManager.apply(KotlinPluginWrapper::class.java)

        extensions.configure<KotlinJvmProjectExtension>("kotlin") {
            it.compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    /**
     * Configures the Java plugin with Java 11 as the target because Java 11 is the target on Android.
     * Additionally, adds sources and javadoc JARs, as patches have a public API.
     */
    private fun Project.configureJava() {
        extensions.configure<JavaPluginExtension>("java") {
            it.targetCompatibility = JavaVersion.VERSION_11

            it.withSourcesJar()
            it.withJavadocJar()
        }
    }

    /**
     * Applies the binary compatibility validator plugin to the project, because patches have a public API.
     */
    private fun Project.configureBinaryCompatibilityValidator() {
        pluginManager.apply(BinaryCompatibilityValidatorPlugin::class.java)
    }

    /**
     * Configures the signing plugin to sign the patches publication.
     */
    private fun Project.configureSigning() {
        pluginManager.apply("signing")

        extensions.configure<SigningExtension>("signing") {
            it.useGpgCmd()
            extensions.getByType(PublishingExtension::class.java).publications
                .named("ReVancedPatches").configure(it::sign)
        }
    }

    /**
     * Adds a task to build the DEX file of the patches and adds it to the patches file to use on Android,
     * adds the publishing plugin to the project to publish the patches API and
     * configures the publication with the "about" information from the extension.
     */
    private fun Project.configurePublishing(patchesExtension: PatchesExtension) {
        val buildAndroid = tasks.register("buildAndroid") { task ->
            task.description = "Builds the project for Android by compiling to DEX and adding it to the patches file."
            task.group = "build"

            task.dependsOn(tasks["jar"])

            task.doLast {
                val workingDirectory = layout.buildDirectory.dir("revanced").get().asFile.also(File::mkdirs)

                val patchesFile = tasks["jar"].outputs.files.first()
                val classesZipFile = workingDirectory.resolve("classes.zip")

                D8Command.builder()
                    .addProgramResourceProvider(ArchiveResourceProvider.fromArchive(patchesFile.toPath(), true))
                    .setMode(CompilationMode.RELEASE)
                    .setOutput(classesZipFile.toPath(), OutputMode.DexIndexed)
                    .build()
                    .let(D8::run)

                ZFile.openReadWrite(patchesFile).use { zFile ->
                    zFile.mergeFrom(ZFile.openReadOnly(classesZipFile)) { false }
                }
            }
        }

        pluginManager.apply("maven-publish")

        extensions.configure(PublishingExtension::class.java) { extension ->
            // Necessary for the signing plugin for a publication to be created
            // and the signing plugin to sign the publication, when no repositories are defined.
            extension.repositories.mavenLocal {
                it.name = "DummyMavenLocal"
            }

            extension.publications { container ->
                container.create("ReVancedPatches", MavenPublication::class.java) {
                    it.from(components["java"])

                    val about = patchesExtension.about
                    it.pom { pom ->
                        pom.name.set(about.name)
                        pom.description.set(about.description)
                        pom.url.set(about.website)

                        pom.licenses { licenses ->
                            licenses.license { license ->
                                license.name.set(about.license)
                            }
                        }
                        pom.developers { developers ->
                            developers.developer { developer ->
                                developer.name.set(about.author)
                                developer.email.set(about.contact)
                            }
                        }
                        pom.scm { scm ->
                            scm.url.set(about.source)
                        }
                    }
                }
            }
        }

        // Used by gradle-semantic-release-plugin.
        // Tracking: https://github.com/KengoTODA/gradle-semantic-release-plugin/issues/435
        tasks["publish"].apply {
            dependsOn(buildAndroid)
        }
    }

    /**
     * Configures the project to consume the extension artifacts and add them to the resources of the patches project.
     */
    private fun Project.configureConsumeExtensions(patchesExtension: PatchesExtension) {
        val extensionsProject = try {
            project(patchesExtension.extensionsProjectPath ?: return)
        } catch (e: UnknownProjectException) {
            return
        }

        val extensionProjects = extensionsProject.subprojects.filter { extensionProject ->
            extensionProject.plugins.hasPlugin(ExtensionPlugin::class.java)
        }

        val extensionsDependencyScopeConfiguration =
            configurations.dependencyScope("extensionsDependencyScope").get()
        val extensionsConfiguration = configurations.resolvable("extensionConfiguration").apply {
            configure { it.extendsFrom(extensionsDependencyScopeConfiguration) }
        }

        project.dependencies.apply {
            extensionProjects.forEach { extensionProject ->
                add(
                    extensionsDependencyScopeConfiguration.name,
                    project(
                        mapOf(
                            "path" to extensionProject.path,
                            "configuration" to "extensionConfiguration",
                        ),
                    ),
                )
            }
        }

        extensions.configure<SourceSetContainer>("sourceSets") { sources ->
            sources.named("main") { main ->
                main.resources.srcDir(extensionsConfiguration)
            }
        }
    }
}

/**
 * Configure the manifest file with the "about" information from the extension.
 */
private fun Project.configureJarTask(patchesExtension: PatchesExtension) {
    tasks.withType(Jar::class.java).configureEach {
        it.archiveExtension.set("rvp")
        it.manifest.apply {
            attributes["Name"] = patchesExtension.about.name
            attributes["Description"] = patchesExtension.about.description
            attributes["Version"] = project.version.toString()
            attributes["Timestamp"] = System.currentTimeMillis().toString()
            attributes["Source"] = patchesExtension.about.source
            attributes["Author"] = patchesExtension.about.author
            attributes["Contact"] = patchesExtension.about.contact
            attributes["Website"] = patchesExtension.about.website
            attributes["License"] = patchesExtension.about.license
        }
    }
}
