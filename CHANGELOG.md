## 3.5.2-a16.3 — Progressive preview blur and blurred handoff hold (fork, checkpoint)

- Opening, before the panel switch: the live cover preview on the inner panel (a full-resolution compositor mirror, previously always sharp) gets a SurfaceFlinger background-blur effect layer whose radius follows the hinge: smoothstep from 0 px at 0° to 56 px at the Fold 7 switch angle, matching the cover glass's `smoothstep(hinge/90°)` blur curve. Radius updates glide at 8 ms intervals (40 ms time constant) between angle samples, which arrive only every 30–130 ms in 2–3° steps.
- After the switch: the frozen two-column "hold" layout gets the same blur over its right frame, so blur continues through the black-out instead of jumping to a sharp frozen frame.
- Debug-tunable without rebuilding: `debug.duofold.preview_blur_start|max|smooth_ms` system properties (shell-writable).
- Debug builds log blur (`DuoBlur`), hold timing (`DuoHold`) and the post-switch capture/readiness pipeline (`DuoReady`). Measured: the hold lasts 560–810 ms after an opening switch, dominated by the first capture on the new panel and glass readiness.
- Known limit, motivating the next step: before the switch the inner panel is drawn by compositor layers (mirror, snapshot, blur), after it by the glass shader, so perspective "book" corners, blur shape and timing still change at the switch.

## 3.5.2-a16.2 — Fold 7 handoff black fade tuned to measured timing (fork)

Measured on SM-F966U1 / One UI 8.5 with a debug fade trace (8–10 transitions per run), solid black per fold went from 280–490 ms closing / 483–622 ms opening to 155–179 ms / 140–212 ms. Most of what remains is Samsung's own panel OFF→ON blank (about 18–145 ms), which Android sequences on purpose.

- Fold 7 only: full black at 94° closing and 101° opening (Samsung's measured switch angles) instead of 98° opening; the re-arm midpoint follows the configured angles.
- Fold 7 only: reveal 40 ms after the destination panel turns ON instead of waiting up to 900 ms for a confirmed destination draw. Duo draws only on new angles, and the wallpaper sends none for 0.3–0.5 s after a switch.
- Fold 7 only: 100 ms reveal at 0% gradualness (was 180 ms), and per-frame (16 ms) re-checks while a switch is in progress (was 80 ms).
- Every other model keeps the original values; the defaults are covered by an equivalence test.
- Debug builds log each change in fade decision under the `DuoHandoff` tag.

## 3.5.2-a16.1 — Fold 7 Android 16 port (fork, untested on device)

- Accept Android 16 (SDK 36) on the SM-F966 / Fold 7 family only. Android 17 stays eligible for every model, as before; SDK 35 and 38 remain rejected.
- The Android 16 framework on SM-F966U1 (One UI 8.5) contains every hidden class the app reflects into, and its `DeviceState` posture property values match the Android 17 values the rotation hold uses. `IWindowManager.setShouldShowSystemDecors` is absent there and was already optional.
- Upstream deliberately left Fold 7 on Android 16 outside the wallpaper profile (1.5.4); whether FoldInteractive answers angle commands on One UI 8.5 is not yet verified.

## 1.6.0 — Tested performance release

Promotes 1.6.0-alpha.4 behavior unchanged. Only stable version metadata and release documentation differ.

- 1 ms minimum wait between completed interactive angle polls, without overlapping requests.
- Direct vsync glass rendering, half-resolution by default, optional full resolution.
- Motion smoothness slider: 12–120 ms, default 12 ms.
- Existing handoff angles, screenshot mode, wallpapers, and settings preserved.
- User testing on SM-F971U reported smooth behavior and last-active samples of 55.7 cover / 108.1 inner submitted FPS. These are not guaranteed panel FPS or controlled benchmarks; angle age is not end-to-end latency.
- Screenshot removal and unlock wallpaper flash remain separate future work.

## 1.5.4 — Regional Fold 8 setup

- Enable the SM-F971 regional family on Android 17, including unlocked U1 and Canadian W, in wallpaper setup and both display controllers.
- Keep Samsung component/API checks, named concurrent-display-state discovery, and wallpaper configuration readback. Regional firmware compatibility remains experimental.
- Report model/OS eligibility separately from Shizuku authorization and verified wallpaper configuration.
- Refresh helper process versions after upgrade. No animation, capture, timing, or settings migration changes.
- SM-F966 / Fold 7 on Android 16 remains outside this wallpaper profile.

# Changelog

## v1.5.1 — privacy-clean public release (versionCode 24)

- Neutral application ID, code namespaces, component authorities, diagnostics, and signing certificate.
- Duo Fold Live branding throughout app interfaces and internal app identifiers.
- Installs separately from older private builds; old app data is not overwritten or migrated.
- Retains guided first-install setup, both animations, custom photo wallpaper, and mandatory Samsung hinge wallpaper configuration.
- Upstream license and attribution notices remain intact.

The rendering/handoff pipeline is unchanged. The custom-wallpaper unlock flash remains unresolved. Setup and the renamed helper components require handset verification.
