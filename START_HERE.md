# Prompt for Codex Astra

Use this Fluid Home project as the starting implementation for my Samsung Galaxy S24 Ultra
launcher. Work in the repository I provide. Read README.md, AGENTS.md, docs/DESIGN.md, and
docs/VALIDATION.md first.

Preserve and improve the real native launcher: three home pages, layered 3D materials,
installed-app discovery and launching, a perspective app drawer, persistent placement,
folders, and Android widget hosting. Prioritize my S24 Ultra and visual quality. I want the
depth and responsiveness that made HTC Sense 3.0 exciting, as an original modern design.

First inspect this source and build it. Fix failures and perform the device checks your
environment actually supports. Keep widgets interactive and the launcher usable if an app
or widget disappears. Do not replace it with a WebView, static mockup, flat stock launcher,
or empty scaffold.

Place all project files at the repository root, including .github/workflows/android.yml.
Keep Java 17 / Gradle 8.11.1 / AGP 8.9.2 / SDK 35 unless a specific verified issue requires a
coordinated change. CI explicitly installs Gradle. Do not change commands to ./gradlew unless
you supply a valid wrapper. Keep binaries, APKs, fake JARs, and encoded substitutes out of a
text-only code patch.

Run :app:testDebugUnitTest, :app:lintDebug, and :app:assembleDebug. Do not suppress checks to
make them green. Distinguish tool/network blockers from source failures. Never claim the app
ran on my phone unless it did. Keep APK build identity and the commit identifier so I can
find the correct download.

Improve the documented limits in small validated steps, starting with on-phone behavior and
visual polish. Notification badges, deep shortcuts, icon packs, wallpaper import, freeform
widget placement, private-space management, physically refractive glass, and system Recents
integration are not implemented. Do not claim otherwise. Ask about major new product choices
only after the existing result is concrete and reviewable.

Finish with short instructions for my phone: which PR/commit has the work, which Actions run
to open, and which artifact contains the APK. Explain any needed merge as one specific action.
