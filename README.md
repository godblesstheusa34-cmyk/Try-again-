# Fluid Home — Codex Astra handoff

A native Android launcher for Chris's Samsung Galaxy S24 Ultra. This is a working source
foundation for Astra to polish and test on the phone, rather than a claim of finished device QA.

**Give Astra this entire project and tell it to read [START_HERE.md](START_HERE.md).**
The visual design and implementation limits are in [docs/DESIGN.md](docs/DESIGN.md).
Validation results are in [docs/VALIDATION.md](docs/VALIDATION.md).

## Included behavior

- A real Android HOME activity and default-Home selection.
- Three swipeable pages: Space, Home, and Tools, with native perspective transforms.
- A lit OpenGL ES 3 background, smoked-glass surfaces, raised icons, and phone tilt.
- Actual installed apps, a searchable drawer, and rotating shelves of 20 apps.
- Twelve app slots per page and four dock slots, saved across restarts.
- Icon moves, drag/drop, swapping, folders, renaming, and folder membership editing.
- Native widget hosting, system bind permission, provider configuration, resizing, and removal.
- App information and Android's uninstall flow.
- Accent colors, ambient motion, reduced motion, and haptic settings.
- Real time/date, battery reading, reported memory, and current display mode.

The scene and sensors stop while the launcher is in the background. Widgets remain native
controls. Gestures starting inside a widget stay with it; swipe outside it or use the page tabs
to switch home pages. The launcher requests a supported refresh rate up to 120 Hz. That is
not a measured frame-rate guarantee.

## Build on GitHub from a phone

1. Have Astra put these files at the **repository root**, with `app/`, `settings.gradle`,
   and `.github/workflows/android.yml` directly there.
2. Open **Actions → Build Fluid Home APK** and choose the run for the new commit.
3. When it is green, open **Artifacts** and download `fluid-home-debug-...`.
4. Extract that artifact ZIP and install **app-debug.apk**. `build-info.txt` identifies its source.
5. Open Fluid Home and use **Tools → Set Fluid Home as default** when ready.

**Code → Download ZIP** downloads source, not an APK. An Actions artifact ZIP must also be
extracted before opening its APK. For a PR build, check that the run matches the PR's current
commit. After merging, use the run for the merge commit rather than an older green build.

To return to Samsung's launcher, use **Tools → Choose your default Home app → One UI Home**.
The same choice is available through Android Settings → Apps → Choose default apps → Home app.

## Pinned build baseline

| Component | Version |
|---|---|
| Language | Java 17 and Android native Views |
| Android Gradle Plugin | 8.9.2 |
| Gradle | 8.11.1 |
| JDK | 17 |
| Compile / target SDK | 35 / 35 |
| Minimum API | 34 |
| Build Tools | 35.0.0 |
| Application ID | `com.chris.fluidhome` |

The combination follows the [AGP 8.9 compatibility table](https://developer.android.com/build/releases/agp-8-9-0-release-notes).
It is a fixed baseline, not a claim to use the latest plugin. There are no runtime libraries
beyond the Android platform, and no Kotlin/Compose compiler version dependency.

## Build locally

Install JDK 17, Android SDK platform 35, and Build Tools 35.0.0. Set `ANDROID_HOME` to your SDK,
or create an untracked `local.properties` with `sdk.dir=...`. Run from this directory:

```bash
bash scripts/gradle.sh --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On Windows:

```powershell
.\scripts\gradle.ps1 --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. The Unix bootstrap needs Python 3
for its first download. The download has a pinned SHA-256 checksum.

### No missing wrapper binary

The source handoff contains text only. GitHub Actions explicitly provisions Gradle 8.11.1 and
runs `gradle`; local builds use the named bootstrap scripts. There is no fake `gradlew` that
expects a nonexistent JAR. This avoids the prior binary-file handoff problem.

To generate a conventional wrapper for Android Studio:

```bash
bash scripts/gradle.sh wrapper --gradle-version 8.11.1 --distribution-type bin \
  --gradle-distribution-sha256-sum f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
```

Then open the project in Android Studio. Keep the generated JAR local if a text-only patch
cannot carry it. Do not commit APKs, SDKs, Gradle distributions, keystores, or encoded binaries.

### Debug signing

Debug APKs use development signing. The GitHub workflow caches the debug key when available,
but a fresh branch or evicted cache can generate another key. Android will not install a
differently signed APK over the same package. Arrange a stable private key for ongoing use;
uninstalling clears saved layout and widgets. Never commit a private signing key. A locally
built APK and a GitHub-built APK may use different keys.

## Source map

| File | Responsibility |
|---|---|
| `MainActivity.java` | Home shell, actions, sensors, placement UI, default Home |
| `AppCatalog.java` | Background app discovery and package callbacks |
| `Workspace.java` / `HomeStore.java` | Placement rules, folders, durable state |
| `DepthPager.java` / `Spring.java` | Perspective paging, gestures, motion |
| `AppDrawer.java` / `AppTile.java` | Search, app shelves, raised icon controls |
| `WidgetController.java` | Native widget lifecycle and setup |
| `SceneSurface.java` / `assets/scene.*` | Geometry, shader lighting, GL lifecycle |
| `Ui.java` / `GlassDrawable.java` | Typography, dimensions, surfaces |

No runtime network permission, analytics, account, or downloaded artwork is needed. Launched
apps and hosted widget providers can use their own permissions and services.
