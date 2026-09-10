# BlockForge Installer

An Android app (Kotlin + Jetpack Compose) for browsing **Mods, Resource Packs,
Worlds, and Shaders** from **Modrinth** and **CurseForge**, and installing the
downloaded file straight into a folder you choose — e.g. your **Amethyst
launcher** game-data folder — using Android's Storage Access Framework (SAF).

Black background, amethyst accent, optional **Mojangles** font.

## Build it

1. Open this folder in **Android Studio** (Koala/Ladybug or newer).
2. Let Gradle sync — it will download the Gradle 8.7 wrapper and dependencies
   over your machine's internet connection (this sandbox has no network
   access, so that step couldn't be done here).
3. Run on a device/emulator running **Android 8.0 (API 26)** or newer.

No prebuilt APK is included — Android apps must be compiled with the Android
SDK/toolchain, which this environment doesn't have. This zip is the complete,
ready-to-build source.

## Before you build: two things need to be supplied by you

### 1. CurseForge API key (required only for the CurseForge source)
CurseForge's API terms require each app to use its **own developer key** —
keys can't be hardcoded into an app that's redistributed. Get a free key at
`console.curseforge.com`, then paste it into the app's **Settings** screen
after install. Modrinth needs no key and works immediately.

### 2. Mojangles font (optional)
Mojang's in-game UI font ("Mojangles") is proprietary and isn't bundled here
for licensing reasons. If you have a legitimately obtained copy of the file:

```
app/src/main/assets/fonts/Mojangles.ttf
```

Drop it there and rebuild. If it's missing, the app automatically falls back
to a clean monospace font — it will never fail to build or crash because the
font is absent.

## How installing works

1. Search a project (mod/pack/world/shader) on Modrinth or CurseForge.
2. Pick which file/version to install.
3. Tap **Install to selected folder** — Android's folder picker opens so you
   can navigate into **Amethyst's** app storage and select the exact folder
   for that content type (its `mods`, `resource_packs`, `worlds`, or
   `shader_packs` folder). The app remembers your choice per category so
   future installs of the same content type are one tap.
4. The file downloads and is either copied in as-is, or (optionally, useful
   for resource packs/shaders) extracted into a folder of the same name.

SAF is required here because, since Android 11 (scoped storage), apps can no
longer freely read/write another app's private files without the user
explicitly granting access to a specific folder — this is what the folder
picker does.

## Known caveats (please read before relying on this)

- **Modrinth has no dedicated "World" project type.** The Worlds tab against
  Modrinth currently queries "modpack" projects as the closest match — it is
  *not* a perfect mapping. CurseForge does have a real Worlds category
  (`classId=17`), so for actual Bedrock/Java world downloads, CurseForge is
  the more accurate source.
- **CurseForge category (`classId`) numbers** can be renumbered by CurseForge
  over time. If a CurseForge tab ever returns empty results, check
  `GET /v1/games/432/categories` with your API key to confirm the current IDs
  and update `CurseForgeClassIds` in `CurseForgeApi.kt`.
- **Amethyst launcher's exact folder layout** wasn't something this build
  could verify (no network/device access here). The picker is generic — it
  lets you navigate to and select *any* folder Amethyst reads from, but you
  should confirm the exact subfolder names it expects on your device the
  first time you use each category.
- This project has not been compiled or run — please build it and file-check
  it in Android Studio before considering it final; I've written it carefully
  but can't personally guarantee a zero-error build without a real compiler
  in the loop.

## Project structure

```
app/src/main/java/com/blockforge/installer/
  MainActivity.kt              — Compose entry point / screen switching
  model/Models.kt              — shared data classes
  network/                     — Retrofit clients for Modrinth + CurseForge,
                                  plus a source-agnostic ContentRepository
  saf/SafInstaller.kt          — SAF download + install/extract logic
  util/PrefsRepository.kt      — remembered folders + CurseForge key (DataStore)
  ui/                          — theme (black/amethyst + Mojangles loader),
                                  BrowseViewModel, and the three screens
```
