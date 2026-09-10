# BlockForge Installer

Android Kotlin + Jetpack Compose installer for Minecraft Java launchers such as **Amethyst, MojoLauncher, and PojavLauncher**.

BlockForge uses Android's Storage Access Framework (SAF) once to access the launcher folder. The selected folder is expected to contain:

```text
Launcher folder/
├── .minecraft/
│   └── launcher_profiles.json
└── custom_instances/
```

BlockForge reads `.minecraft/launcher_profiles.json`, displays the launcher profiles as Minecraft instances, and uses each profile's `gameDir` to determine where content should be installed.

## User flow

1. First launch, or when the saved SAF permission is no longer valid:
   - BlockForge opens Android's folder picker.
   - Select the Amethyst/Mojo/PojavLauncher folder containing `.minecraft` and `custom_instances`.
2. BlockForge reads:
   - `.minecraft/launcher_profiles.json`
3. The home screen displays the launcher profiles/instances.
4. Select an instance.
5. Select what to install:
   - Mods
   - Shaders
   - Resource Packs
   - Worlds
6. Select a source:
   - Modrinth
   - CurseForge
7. Search for content, choose a file/version, and install.
8. BlockForge resolves the selected profile's `gameDir` and installs automatically:

```text
Mods           → <gameDir>/mods/
Shaders        → <gameDir>/shaderpacks/
Resource Packs → <gameDir>/resourcepacks/
Worlds         → <gameDir>/saves/
```

World ZIP files are extracted into the `saves` directory. Mods, shaders, and resource packs are copied as their downloaded files.

## CurseForge

CurseForge requires an API key. Enter your own key in Settings. Modrinth does not require a key.

## Build

Open the project in Android Studio and let Gradle sync. The project targets Android API 34 and has a minimum SDK of 26.

The repository does not include a prebuilt APK.
