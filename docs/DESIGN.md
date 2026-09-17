# Fluid Home design

## Direction

An original, tactile launcher inspired by the depth and responsiveness of HTC Sense 3.0.
Use a near-black AMOLED canvas, dark translucent slabs, polished edges, a sculpted metallic
background, and restrained teal lighting. Legibility and direct interaction remain central.

The available prior requirements establish S24 Ultra focus, three swipeable pages, a 3D
drawer, sophisticated materials, and a real HOME application. The exact palette, page purposes,
sculpture, slot counts, and name are implementation choices, not a claimed verbatim recovery
of the full earlier specification.

## Screen layout

| Surface | Design | Interaction |
|---|---|---|
| Space / left | Page title, native widget rack, 12 app slots | Add, bind, configure, resize, remove widgets |
| Home / center | Large thin clock, date, sculpted background, search/battery, 12 slots | Open apps, search, move and group icons |
| Tools / right | Launcher and system actions, 12 slots | Set Home, settings, device/build information |
| Dock | Four apps or folders around the drawer control | Same dock across all home pages |
| App drawer | Search above 20-icon slabs, page navigation below | Perspective shelf rotation, launch, pin, app tools |
| Folder | Native app-list dialog with editing | Rename, launch, remove members |

Pages scroll vertically when required by widgets or larger text. Horizontal paging locks
to a horizontal gesture. A gesture beginning inside a widget remains with the widget.
Page tabs and accessibility scroll actions provide alternatives.

## Materials and dimensions

| Token | Value |
|---|---|
| Background | `#080D15` |
| Main text | `#F0F5FB` |
| Secondary text | `#ACBBC9` |
| Sea glass accent | `#8FE6D5` |
| Glacier / ember / orchid | `#91C6FF` / `#EFB480` / `#C1ADFA` |
| Surface radius | 22–28 dp; icon face 17 dp |
| Surface edge | 1 dp illuminated border, 3 dp lower lip |
| Icon face | 58 dp on pages, 50 dp in dock |
| Main controls | At least 48 dp high |
| Clock | 78 sp, thin system face, 12/24-hour-aware |
| Page title | 32 sp, light system face |

The actual app icons come from installed packages. Status bar, cutout, navigation bar and
keyboard insets are measured; no fixed S24 pixel resolution or reported RAM size is assumed.

## Rendering and motion

Three closed ribbon meshes use perspective projection, a depth buffer, key-light specular
highlights, diffuse shading, and a Fresnel rim. Each mesh uses 128 major and 20 minor segments,
for 15,360 triangles across the three objects. Normals approximate the rippled surface. Mesh
and GPU buffers are created on GL context creation, not each frame.

Native Android controls sit above the scene. Their page-relative position drives rotation,
horizontal and vertical translation, depth ordering, scale and opacity. Home pages rotate
24 degrees per page; drawer shelves use 52 degrees. Android transforms hit coordinates with
the views, so app icons and widget controls stay native.

The spring uses stiffness 280 and damping 29 with bounded integration steps. Native animations
follow display Choreographer timing. Ambient GL animation requests up to 60 frames/second;
the activity requests the highest advertised refresh rate up to 120 Hz without changing
display resolution. Android and Samsung still choose the actual refresh behavior.

Tilt uses the game-rotation-vector sensor, a resume-time baseline, wrapped angles, low-pass
filtering and clamping. It subtly shifts the scene and pages. Reduced motion, disabled system
animations, and TalkBack touch exploration remove page perspective motion and ambient motion.
The sensor, GL scene and widget host follow activity lifecycle.

The result is real 3D background geometry with translucent shaded native surfaces. It does
not simulate true glass refraction, arbitrary-content reflections, ray tracing, or 3D app-icon meshes.

## State and native integration

App identity is Android user serial plus launchable component. Icons are loaded on a worker
thread. Package callbacks refresh the catalog, and stale results are discarded. Unavailable
apps remain in the saved layout so a locked profile cannot erase placements.

Placement rules are independent of Android. Swap preserves both items; folder merge checks
capacity before changing either slot. The JSON schema is decoded fully before adoption.
Invalid or future data is retained in a recovery preference. Device backup is disabled until
proper widget-ID and profile remapping is implemented.

Widgets use AppWidgetHostView. Binding requests system permission, runs required configuration,
persists pending IDs before leaving the activity, and releases canceled IDs. Interrupted setup
can be continued or discarded. Provider size options are updated after layout. Unsupported
minimum widths are rejected, and orphan cleanup is skipped when saved layout recovery is needed.

## Limits Astra should address

1. **Physical device QA first:** system navigation, insets, app launch/return, font/display zoom,
   shader appearance, tilt direction, widget permission/configuration, and measured frame timing.
2. **Drawer performance:** native views are built eagerly. Measure large catalogs and search;
   introduce reusable pages or debounced filtering if needed.
3. **Widgets:** a rack on Space, with height choices, not freeform placement on every page.
   Reconfiguration after setup and drag resize handles are not exposed.
4. **Folders and menus:** native dialogs, not yet sculpted material overlays.
5. **No notification dots, deep/pinned shortcuts, icon-pack loading or wallpaper import.**
6. **No first-party weather/music service:** use installed providers' widgets; no fake live data.
7. **No private-space/Secure Folder management:** only profiles currently visible through
   LauncherApps. No hidden-profile unlock controls.
8. **No privileged system Recents, notification shade, root, device administrator, or lock-screen integration.**
9. **Debug signing:** use a durable private key before expecting seamless long-term updates.

## References

- [AGP compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [LauncherApps](https://developer.android.com/reference/android/content/pm/LauncherApps)
- [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager)
- [Widget host responsibilities](https://developer.android.com/develop/ui/views/appwidgets/host)

These establish platform contracts, not physical-device validation of this code.
