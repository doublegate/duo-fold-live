## 3.5.2-a16.8 — Every display-override release waits for the hinge to report closed (fork, Phase A)

Plan Phase A (`docs/fold7/remediation-plan-2026-10-04.md`, items A1-A8). Releasing a concurrent display override
(state 5 cover / 4 inner) while the base device state is still HALF_OPENED goes 5->2->0 or 4->2->0, and Samsung
treats 2->0 as `sleepDevice=true` (the cover blanks). a16.7 gated the main closing path; this closes the rest.
Verified by unit tests only (on-device verification follows the full plan).

- **All models:** Fold-Only / Unfold-Only mode changes no longer cancel a held cover override outright. Fold-Only
  turns the effect off at the closed endpoint and on any 2 deg reversal, which released at base HALF_OPENED on
  every Fold-Only close; the release is now deferred by `CloseReleaseGate` like any other (A1).
- **All models:** the gate's "nothing to blank" escape uses the display power state only. It used the "service
  running, enabled, screen on and unlocked" flag, so a keyguard lock, service restart or disabling Duo mid-close
  released with the screen on. The angle poll now sends `PowerManager.isInteractive()` separately (A2).
- **All models:** reader teardown (2.5 s lease expiry, Shizuku destroy, restart) waits up to 3 s for CLOSED before
  cancelling a held cover override; the primary angle-anchor `addView` (re-run exactly when display 0 changes
  panels) no longer fails the whole reader when it throws (A3).
- **All models:** a system cancel of the cover request resets the hold policy, so it is requested again on the next
  sample instead of never for the rest of that close (A4).
- **All models (direct mode):** an inner (4) hold that reaches closed without a sample at or below 94 deg hands over
  to the cover hold, whose release is gated, instead of releasing 4 at base HALF_OPENED (A5).
- **All models (dual mode):** the outer (5) session's endpoint, stale-angle and locked releases are gated (A6).
- **All models:** the continuity probe's FINISH release is gated (A7).
- **All models:** the closing-mirror black mask deactivates 1.8 s after the secondary panel is gone; it stayed
  active for as long as the phone was closed, keeping the fade engine ticking per vsync and starting the next
  opening black-masked (A8).
- `BaseDeviceState`: the base-state query resolves its reflection once and calls system_server at most every
  16 ms, after the cheap escapes (reopening, screen off, cap); it ran on every 4 ms poll during a defer (C3).
- Docs: `docs/fold7/tuning-reference.md` collects every Fold 7 value, debug property and build type introduced since
  a16.1 with its measurement, plus a version map (a16.4 and a16.6 were device-only test builds whose changes are in
  the a16.5 and a16.7 commits).
- Tests: CloseReleaseGate (Fold-Only endpoint, locked-screen-on, bounded teardown, dual outer), HandoffPolicy
  re-request after cancel, DirectHandoffPolicy inner hand-over, ClosingMirrorFadePolicy bound; 273 pass.

## 3.5.2-a16.7 — Close without the cover blackout, steadier glass, right-half blur continuity, lighter hot paths (fork, checkpoint)

Covers a16.6 and a16.7. Developed against four 120 s captures of both panels on an SM-F966U1 / One UI 8.5 and a
full code audit; open findings are tracked in `docs/fold7/remediation-plan-2026-10-04.md`. Fold 7 only unless
marked **all models**.

Closing and the black cover screen

- **All models:** `CloseReleaseGate` holds the cover-concurrent override (state 5) until the base device state is
  CLOSED. Releasing it while the hinge still reports HALF_OPENED went 5->2->0, and Samsung treats 2->0 as
  `sleepDevice=true`, which blanked the cover. The hold is bounded at 3 s and lifts at once on reopening (>= 98 deg)
  or with the screen off. The angle <= 0 RELEASE in `DirectHandoffPolicy` now passes the gate too; it was the path
  of the last remaining blackout. Measured: 5 sleep-path closes per run before, 1 in 8 after a16.6, 0 in 10 after
  a16.7. Known remaining ungated paths (Fold-Only mode, keyguard/teardown, dual mode) are plan Phase A.

Reveal timing and glass frames

- The cover re-reports readiness for 1.5 s after its surface appears (a once-only report could be missed, holding
  black to the cap), and a fully closed cover counts as ready. Closing reveal cap ON+600 -> ON+300 ms.
- Fresh-content window 700 ms on the Fold 7 (upstream 350 ms stays elsewhere): the first post-switch capture alone
  takes up to ~400 ms there, so good frames were being rejected.
- A failed capture keeps the last good same-panel frame for up to 1.5 s and retries in 32 ms instead of dropping
  the glass to its empty fallback ("Panel changed during capture" on the 4->3 state step, screen-off, 250 ms
  timeouts). Rendered glass may be up to 1.5 s old on the Fold 7; panel switches still clear frames explicitly.
  Measured: ~21 visible empty frames per run before, 0 after.
- Removed the switch-time precapture and warm start from a16.5 (`GlassFrames.precapture()`, `WarmStart`, the
  switch-time publication; `GlassCapture` again requires an exclusion list). They produced frames from the wrong
  moment and could deadlock the first capture.
- Upstream readiness test invariants are preserved through per-device constants.

Right half: blur continuity

- `RightHalfBlur`: while the inner panel is primary, a compositor blur over its right half continues the opening
  mirror's blur from 56 px at the switch angle down to 0 at fully open (172 deg). Before, the right half went from
  the mirror at 56 px to the sharp native screen across the black. Closing ramps it back up. Clear angle tunable:
  `adb shell setprop debug.duofold.right_blur_clear <deg>`.

Performance and stalls

- **All models:** angle polling 4 ms while the hinge moves or near the switch angles, 33 ms when still (was 4 ms
  always); secondary-panel refresh 8 ms moving / 100 ms still.
- Mirror attach (waits up to 250 ms for its commit) runs outside the angle reader's lock and on its own IPC thread
  on both sides, so the 4 ms poll that drives the fade and cover hold no longer stalls mid-transition.
- The wallpaper angle command (a synchronous window-manager call) moved off the app's main thread.
- The fade engine no longer runs per vsync while nothing fades, and checks the keyguard every 200 ms, not per frame.
- Preview blur ticker idles once settled and releases its layer on its own thread (no radius on a released layer).
- Angle parser compiles its pattern once (about 640 wallpaper lines/s reach it while polling).

Diagnostics

- `fold7test` build type: release-optimized, not debuggable, debug-signed (installs over debug builds), diagnostic
  logs on. `BuildConfig.DIAGNOSTICS` replaces `BuildConfig.DEBUG` for every diagnostic log.
- Duo's main glass surface is always skipped from screenshots: skip-screenshot is also what keeps it out of the
  live cover->inner mirror, and un-skipping it under `debug.duofold.record_visible=1` mirrored the cover's
  perspective glass onto the inner right half during captures. Captures now show the native screen where the main
  glass is drawn (plan item B1).
- New logs: `DuoState` gate defers, `DuoReady` "frame kept".

## 3.5.2-a16.5 — Unified left-strip glass, aligned reveals, faster post-switch frames (fork, checkpoint)

Developed against frame-by-frame recordings of both panels (see the fork's Fold 7 project tooling). Fold 7 only unless noted; every other model keeps upstream behaviour.

- Unified mode (`debug.duofold.unified`, default on for the Fold 7): before the switch the inner panel's left strip is drawn by the perspective glass shader (mirrored cover), and the right pane stays a flat live mirror with the progressive blur. The frozen frosted snapshot and seam strip no longer cover it.
- The left-strip reflection uses the inner perspective profile (early/end stretch, vertical compression, startup easing), so it matches the post-switch leaf; the inner sliders now tune both sides.
- `debug.duofold.reflect_max_hinge` (default 55°) keeps the reflection from folding edge-on, which made it collapse ~0.6 s before the switch; `debug.duofold.max_darken` (default 0.55) caps the glass edge darkening, which blacked the panel out around 104–114° while closing.
- Opening and closing reveal from black only on a real glass frame of the new panel (opening capped at ON+600 ms, closing likewise), so the reveal never lands on the dark capture fallback; the mirrored hold is dropped at that moment instead of cross-fading, removing a double exposure and a black frame at the cut-over.
- The first post-switch capture starts at the switch: the fade logic publishes the switch time through the angle poll, `GlassFrames.precapture()` captures with an empty exclusion list before the rebuilt overlay exists (`GlassCapture` now accepts zero exclusions for live captures), and the overlay's own `requestFreshCapture()` no longer discards it. Measured median black per transition ~0.6 s (was ~0.8 s).
- All models: the angle reader reaps orphaned `logcat` readers left behind when the Shizuku service process is killed (found running for hours, doubling log parsing).
- Diagnostics (debug builds): `debug.duofold.record_visible=1` keeps Duo's layers visible to screen recordings and excludes them from the glass capture explicitly, applied live; logs `DuoFallback`, `DuoMirror`, `DuoState`, `DuoReady`.
- Known: a one-frame cover blink ~210–250 ms after the concurrent-state request is released at full close (Android display reconfiguration); first post-switch capture latency (150–400 ms) bounds the black.

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
