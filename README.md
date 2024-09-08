<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="assets/revanced-headline/revanced-headline-vertical-dark.svg"
    >
    <img 
      width="256px"
      src="assets/revanced-headline/revanced-headline-vertical-light.svg"
    >
  </picture>
  <br>
  <a href="https://revanced.app/">
     <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="assets/revanced-logo/revanced-logo.svg" />
         <img height="24px" src="assets/revanced-logo/revanced-logo.svg" />
     </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://github.com/ReVanced">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://i.ibb.co/dMMmCrW/Git-Hub-Mark.png" />
           <img height="24px" src="https://i.ibb.co/9wV3HGF/Git-Hub-Mark-Light.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="http://revanced.app/discord">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://reddit.com/r/revancedapp">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://t.me/app_revanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://x.com/revancedapp">
      <picture>
         <source media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/93124920/270180600-7c1b38bf-889b-4d68-bd5e-b9d86f91421a.png">
         <img height="24px" src="https://user-images.githubusercontent.com/93124920/270108715-d80743fa-b330-4809-b1e6-79fbdc60d09c.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://www.youtube.com/@ReVanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
     </picture>
   </a>
   <br>
   <br>
   Continuing the legacy of Vanced
</p>

# 🐘 ReVanced Patches Gradle plugin

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/ReVanced/revanced-patches-gradle-plugin/release.yml)
![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

A Gradle plugin for ReVanced Patches projects.

## ❓ About

ReVanced Patches Gradle plugin configures a project to develop ReVanced Patches.

For that, the plugin provides:

- The [settings plugin](src/main/kotlin/app/revanced/patches/gradle/SettingsPlugin.kt):
Applied to the `settings.gradle.kts` file, configures the project repositories and subprojects
- The [patches plugin](src/main/kotlin/app/revanced/patches/gradle/PatchesPlugin.kt):
Applied to the patches subproject by the settings plugin
- The [extension plugin](src/main/kotlin/app/revanced/patches/gradle/ExtensionPlugin.kt):
Applied to extension subprojects by the settings plugin

> [!CAUTION]
> This plugin is not stable yet and likely to change due to lacking experience with Gradle plugins.  
> If you have experience with Gradle plugins and can help improve this plugin,
> consider reaching out to us at gradle-plugin@revanced.app or by opening an issue.

## 🚀 How to get started

> [!TIP]
> The [ReVanced Patches template](https://github.com/revanced/revanced-patches-template) repository
> uses this plugin and is a good starting point to create a new ReVanced Patches project.

Add the following to the `settings.gradle.kts` file:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
           name = "GitHubPackages"
           url = uri("https://maven.pkg.github.com/revanced/registry")
           credentials {
              username = providers.gradleProperty("gpr.user")
              password = providers.gradleProperty("gpr.key")
           }
        }
    }
}

plugins {
   id("app.revanced.patches") version "<version>"
}
```

> [!NOTE]
> The plugin is published to the GitHub Package Registry, so you need to authenticate with GitHub.  
> More information
> [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).

Create the patches project and configure the `build.gradle.kts` file:

```kotlin
patches {
    about {
        name = "ReVanced Patches"
        description = "Patches for ReVanced"
        // ...   
    }
}
```

> [!NOTE]
> By default, the plugin expects the patches project to be in the `patches` directory.

Create the extension project and configure the `build.gradle.kts` file:

```kotlin
extension {
   name = "extensions/extension.rve"
}

android {
   namespace = "app.revanced.extension"
}
```

> [!NOTE]
> By default, the plugin expects extension projects to be under the `extensions` directory.

## 📚 Everything else

### 🛠️ Building

To build ReVanced Patches Gradle plugin, follow these steps:

1. Clone the repository and navigate to the project directory.
2. Authenticate with GitHub. More information
   [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-to-github-packages).
3. Run `./gradlew build` to build the plugin.
4. Optionally, run `./gradlew publishToMavenLocal` to publish the plugin to your local Maven repository for development.

## 📜 Licence

ReVanced Patches Gradle plugin is licensed under the GPLv3 license.
Please see the [license file](LICENSE) for more information. [tl;dr](https://www.tldrlegal.com/license/gnu-general-public-license-v3-gpl-3) you may copy, distribute and modify
ReVanced Patches Gradle plugin as long as you track changes/dates in source files.
Any modifications to ReVanced Patches Gradle plugin must also be made available under the GPL,
along with build & install instructions.