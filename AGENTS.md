# Fluid Home engineering contract

- Target Chris's Galaxy S24 Ultra. Preserve practical inset, font-scale, and display-zoom
  handling; do not build a low-end-device fallback architecture.
- Keep this a real HOME app. Preserve launching, durable layout, widget input, native focus,
  and a visible route back to One UI Home.
- Keep the design direction in docs/DESIGN.md. Additional explicit user instructions win.
- CI uses explicitly installed Gradle. Local entry point is `bash scripts/gradle.sh`.
  Never claim a missing wrapper JAR exists. No binary files disguised as text.
- App keys include both activity and user serial. A locked profile must not erase placements.
- Widget IDs and pending configuration survive process death; canceled allocations are released.
- Stop GPU/sensor work when paused. Avoid per-frame state writes and bitmap/texture allocation.
- Honor reduced motion and disabled system animations. A requested refresh rate is not a benchmark.
- Keep docs/VALIDATION.md truthful. A successful build is not an installation or device test.
- Keep fixes scoped and tests meaningful. Do not disable failing tests or blanket-ignore lint.

Required build check:

```sh
bash scripts/gradle.sh --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```
