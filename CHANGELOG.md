## Unreleased — Build and tooling (fork)

- `gradle.properties`: Kotlin incremental compilation and the Kotlin compiler daemon instead of full in-process
  recompiles, Gradle parallel execution and build cache, 6 GB daemon heap, worker count left at the CPU count.
  Measured on a 10-core desktop: one-file change + `assembleFold7test` 33 s -> 15.5 s. File-system watching stays
  at Gradle's default (unsupported on network shares). Upstream's values differ; keep this file out of upstream PRs.
- `tools/check_inner_stretch.py`: the two shaders are compiled and checked concurrently (3.2 s -> 1.8 s) with the
  report in a fixed order; a failing invariant still exits non-zero (verified with a deliberately broken assert).

## 3.5.2-a16.16 — Review fixes from the upstream pull requests (fork)

Fixes for the automated review of upstream PRs #17 and #18 (joeconsorti/duo-fold-live). Verified by unit tests and the
lint/build gates; not yet re-checked on the device.

- `AngleParser`: the per-action token cache is one immutable snapshot published through a volatile field. Two
  overlapping reader threads after a restart could pair one action's name with another action's tokens and reject
  every sample until the next restart.
- `CoverHandoff.changeState` resets the release deferral, so a new direct-mode hold never inherits an expired 3 s
  window from an earlier close and releases before base CLOSED.
- `ConcurrentController`: the reopening escape requires a fresh sample (`deferOuter(angle,fresh)`,
  `releaseGated(angle,fresh)`), as `CoverHandoff` already did. A stale 100 deg reading no longer releases state 5 at
  base HALF_OPENED.
- `AngleReader`: on a dual-mode toggle the incoming controller starts only after the outgoing override has actually
  released; starting it while the gated release was still deferring was refused as an existing override.
- `DuoGlass.sourceSize` clears the reused size before the display lookup; an absent source display left stale
  dimensions that let an old frame pass `usable()`.
- **All models but the Fold 7 unified path now keep their original visuals:** the preview's progressive blur is
  created only for the unified half-pane preview, the hold blur is shown only for the unified half-pane hold, the
  post-switch right-half blur also requires the unified renderer (`debug.duofold.unified=0` compares cleanly), and the
  hold uses the half-pane layout only when both panels are upright (as `SecondaryShade` does).
- `HandoffFadePolicy`: the 600 ms opening cap applies only when inner glass is required; the no-glass path (Duo
  Classic, debug) keeps the 40 ms destination-draw cap.
- `PreviewTransition`: hold readiness uses the per-model fresh window (700 ms on the Fold 7) so the hold drops on the
  same frame that starts the reveal; pre-switch preparation keeps 500 ms.
- `InnerLiveMirror.attach` re-checks its ticket after unlocking: a revoke that found the lock held could otherwise
  leave the revoked mirror attached until the next poll.
- Tests: `HandoffFadePolicyTest.openingCapAppliesOnlyWhenInnerGlassIsRequired` (fails without the fix),
  `PreviewExpansionPolicyTest.readinessWindowMatchesTheRevealWindow`.

## 3.5.2-a16.15 — Upstream 3.5.3 alpha improvements: helper recovery, preview release gate, closed-hinge hysteresis (fork)

Ported from the upstream 3.5.3 alpha line (joeconsorti/duo-fold-live) after surveying every branch off `main`.
**All models.** Verified by unit tests only.

- Helper recovery after Shizuku restarts (upstream alpha.19): each Shizuku UserService bind is one attempt
  (`HelperAttempt`, `HelperBinding`) with a 35 s callback timeout (the server allows 30 s), late and duplicate
  callbacks ignored, and the local callback detached before the remote cleanup. Shizuku binder death or arrival now
  resets every helper (angle reader, wallpaper restore, keep-awake, inner decor) instead of only clearing the effect;
  `FoldBackgroundService.reconnectHelpers` and a "Reconnect helpers" button force it, and the status report shows
  per-helper connection stages. Previously a Shizuku restart could leave Duo disconnected until the app was reopened.
- Non-blocking preview release gate (upstream alpha.15): the pre-release hold no longer waits up to 24 ms on the
  angle poll. `PreviewReleaseGate` is request-local (committed, timed out after 80 ms, or skipped); the cover hold is
  not released until it allows, and a reversal, staleness, disable or lock cancels the wait. Cleanup no longer counts
  as a commit, and a late callback cannot acknowledge a newer request.
- Closed-hinge hysteresis (upstream alpha.20, default on, "Closed-hinge jitter protection" setting): the effective
  angle opens only at closed + 2 deg and closes at the closed threshold, applied once before rendering, preview,
  rotation, fade and display requests; a still, closed phone reading 3 deg no longer starts a cover preview. Sent as a
  trailing field after the screen-on flag in the angle poll (older clients omit it).
- Tests: `PreviewReleaseGateTest`, `ClosedHingeGateTest`, `HelperAttemptTest` (from upstream).

## 3.5.2-a16.14 — Glass content capture slows down at rest (fork, post-plan follow-up)

Measured on the device (SM-F966U1) with a per-process CPU sampler, 60 s per posture, phone still, screen on.

- **All models:** the glass content capture loop runs at the configured rate (`content_fps`, default 120) only while
  the hinge moves or right after a panel switch; once the hinge has been still for 1 s it captures at 10 fps (never
  above the setting). The shader still redraws on every angle change; only the content under the glass refreshes
  less often. Resting part-open (Flex, ~120 deg, glass partly visible) kept capturing at the full rate.

| Posture at rest (app + angle service, % of one core) | a16.7 | a16.14 |
| --- | --- | --- |
| Fully open | 4.3 + 2.3 | 3.5 + 2.8 |
| Closed (cover on) | 5.6 + 7.7 | 4.0 + 3.4 |
| Flex (~120 deg) | 20.6 + 22.1 | 10.4 + 9.8 |

- Tried and reverted (no gain on the device): a 500 ms timeout for post-switch captures. The first capture after a
  switch completes only after Android's own display-switch transition paints the destination (~300 ms after the
  switch); waiting longer did not make it sooner, and opening black rose. The remaining black is bounded by that
  transition.
- Tried and withdrawn pending a verified fix: keeping a warm cover frame while closed and pre-creating the inner left
  strip, aimed at a brief black left half at 3-6 deg when opening; it still occurred in 2 of 7 samples.
- Tests: `AdaptiveCaptureRateTest`.

## 3.5.2-a16.13 — Closing cap from capture data, lighter poll replies; remediation plan complete (fork, Phase F)

Plan Phase F (F2-F6; F1 and F7 landed in a16.9). This completes `docs/fold7/remediation-plan-2026-10-04.md`.
Verified by unit tests only; on-device verification of all phases is next.

- Fold 7 closing reveal cap ON+300 -> ON+380 ms (F2). In the 2026-10-04 capture 7 of 10 closes confirmed cover glass
  at ON+190-281 ms; the 3 that hit the cap had their first capture at ON+230-265 ms and commit lags of up to 114 ms,
  so their frames landed just past 300 ms and they revealed on the fallback instead. Worst case +80 ms of black.
- **All models:** the angle-poll reply carries its ~10 diagnostic status strings at most every 250 ms instead of on
  every 4 ms poll; angles, counts, flags and reader state still go out every poll, and the client keeps the previous
  strings in between (F5).
- Readiness evidence from the old panel can never reveal the new one: a pinned test (shown to fail without the panel
  check) documents that the inner/cover identity on every draw already guarantees this (F4).
- Decision (F3): no zero-copy (HardwareBuffer) capture path. Successful captures complete 58-146 ms after the request,
  so the black is bound by post-switch compositor/surface start-up and occasional 250 ms timeouts, not by the copy;
  the live frame also feeds software consumers (`frost()`, the hold's `getPixels`). Recorded in the plan.
- Owner workspace tooling: `transition_sheets.py` writes `summary.txt` from the logs (per-switch ON/reveal/clear and
  reason, sleep-path closes, gate defers, fallback events), since screen recordings drop frames at every switch (F6).
- Tests: `DiagnosticsCadenceTest`, closing-cap default, the F4 old-panel test.

## 3.5.2-a16.12 — One source for the handoff angles, endpoint freshness, test coverage (fork, Phase E)

Plan Phase E (E3-E5, E7-E9; E1, E2 and E6 landed with the plan). Verified by unit tests only.

- **All models:** the cover-hold hysteresis (94 deg request, 98 deg release) has one source,
  `HandoffPolicy.HOLD_ANGLE` / `RELEASE_ANGLE`, used by `DirectHandoffPolicy`, the release gate's "reopening" check in
  `CoverHandoff` and `ConcurrentController`, the pre-release hold and the continuity probe. The gate's escape had to
  equal the policy's release angle and only matched by coincidence (E4).
- **All models:** `ReleaseDeferral` is the one bounded "wait for CLOSED" timer behind every override release; the
  cover hold and the dual-mode session each had their own copy (E8).
- The inner fully-open endpoint accepts frames up to the same freshness window as the reveal readiness (700 ms on the
  Fold 7); it used the 350 ms default, so 350-700 ms frames never counted (E5).
- Decision (E3): the release gate, polling cadence and race fixes stay enabled on every model and are labelled
  "all models" here; the gate protects against the same Samsung device-state sleep transition on other foldables.
- Tests: `AngleParserTest` (both formats, invisible samples, action changes, range), `ReleaseDeferralTest`, pinned
  hysteresis constants, the PreviewExpansion no-readiness 120 ms fade, and every per-model closing-cap default (E8).
- Owner workspace tooling: contact sheets show gate defers and fallbacks and accept both angle formats (E7); the
  workspace README documents the capture scripts as device-modifying (E9).

## 3.5.2-a16.11 — Service-process races and stalls (fork, Phase D)

Plan Phase D (D1-D4, D6, D7; D5 and D8 landed in a16.10). **All models.** Verified by unit tests only.

- Live mirror attach (D1): an attach that read "preview allowed" just before a revoke or a reader stop could commit a
  cover mirror that then stayed on the inner right half until the next start. Attaches now take a generation
  ticket (`MirrorAttachGate`) before reading the permission and close what they built if it was revoked meanwhile.
  `InnerLiveMirror` uses a lock instead of its monitor, so the angle poll's revoke uses `tryLock` and never waits out a
  250 ms attach; reader stop invalidates any attach in flight.
- Blur ticker (D3): the reset and restart run as one runnable on the ticker thread, so two frame loops can no longer
  run at once (halving the glide time and doubling transactions) and the attach thread no longer writes
  ticker-owned fields (which could leave the preview sharp while still).
- Fold-setting commands (D2): the glass-capture helper's `settings` subprocess is bounded at 2 s and killed on
  timeout, and the helper's close no longer queues behind an in-flight capture. A hung `settings` process used to
  hold the helper's monitor forever, blocking reader teardown and with it every later angle poll.
- Record-visible capture exclusions (D4): the capture takes its own copies of the registered layers under the lock
  and releases them afterwards, so an owner releasing its layer mid-capture cannot leave a released handle in the
  exclusion list (diagnostics mode only).
- Rotation hold (D6): after a reader restart the new hold is created only once the previous one has restored
  rotation and dropped its file lock; it used to fail with "Another orientation hold is active" and leave about 2 s
  without a hold, so a fold right after a restart could rotate the screen.
- Shizuku destroy (D7): the up-to-8 s wait for the rotation restore happens outside the reader's monitor, so a
  poll in flight is not held for that long before the process exits.
- Tests: `MirrorAttachGateTest`.

## 3.5.2-a16.10 — Idle costs nothing: hot paths off the poll and frame loops (fork, Phase C)

Plan Phase C (C1-C6; C3 landed in a16.8) plus D5 and D8. **All models** unless noted. Verified by unit tests only.

- Resting in the 80-115 deg switch band (Flex posture) no longer keeps 4 ms angle polling, 8 ms secondary refreshes
  and per-vsync fade ticks forever: the band is treated as "moving" only within 2 s of the last angle change (C1).
- The secondary-panel refresh looks up display 1 directly instead of listing every display, and the overlay reads
  the keyguard state through one 200 ms cache (cleared on every screen/unlock broadcast) instead of a Binder call per
  animation frame and per refresh (C1, C2).
- `DuoLiveShade` stops requesting a frame every vsync once the smoothed angle has settled; it waits for the next
  angle or panel change (re-checking freshness every 250 ms). It ran a 120 Hz frame loop for as long as the screen
  was on (C2).
- The post-switch hold reads display info through the cached `DisplayManagerGlobal`, checks the keyguard every
  200 ms instead of every 8 ms tick, and the angle poll no longer waits up to 24 ms on the hold's handler once the
  hold has started (C4). A prepare or hold request that races the helper's shutdown now frees its parcelled bitmaps
  and skips the wait (D5).
- Frozen glass frames build their mip pyramid on the capture executor, not with seven `createScaledBitmap` calls on
  the main thread at the first frozen draw (C5). Per-frame SharedPreferences reads were measured as in-memory map
  lookups (~1.7k/s at 120 Hz) and deliberately left uncached.
- The diagnostic continuity probe's task-manager checks run at most every 50 ms instead of on every 4 ms poll (C6).
- The fade engine wakes on every real angle change; reading its idle flag from the Binder thread could miss a wake
  for up to 50 ms (D8).
- Tests: `PollCadenceTest` band hold.

## 3.5.2-a16.9 — One geometry and one blur curve on both sides of each switch (fork, Phase B)

Plan Phase B (items B1-B10) plus F1. Fold 7 unified renderer only unless marked **all models**. Verified by unit
tests only (on-device verification follows the full plan).

Geometry: perspective and sizing

- The inner panel's flat right pane is exactly the right half before the switch, during the post-switch hold and
  after the switch. The live cover mirror now fills the right half (`LiveMirrorLayout.rightHalf`: scale 984/1080,
  ~56 px cropped top and bottom; it was fitted to 1032..1968), the left perspective strip covers exactly 0..984, and
  the hold's right frame and reflected left copy use the same layout. Before, the pane edge moved 48 px at the
  switch (B3).
- The left-strip reflection is drawn with the same uniform scale as the mirror, so it is that frame's exact mirror
  image; it was stretched 0.956 x 0.867 (features ~10 % wider than in the mirror) (B4).
- The strip's pose no longer freezes at the 55 deg anti-collapse cap and then jumps at the reveal. `StripPose` eases
  the leaf from the cap to the pose whose shader progress equals the inner glass's at the switch angle (101 deg),
  and ramps the strip blur factor to the inner model's x1.25, so leaf progress, motion, blur radius and edge
  darkening are continuous across the switch while the leaf never reaches edge-on. (The alternative, drawing the
  strip with the inner leaf model, needs a texture remap that cannot be validated off-device; recorded in the plan.)
  (B2)
- The strip and the half-pane layout are used only with both panels in their natural orientation; in landscape the
  strip's physical-aspect width did not match the logically fitted mirror (B9).

Blur

- After the switch the right half loses blur on the same curve as the left glass (`smoothstep((172-a)/(172-90))`),
  rescaled to the mirror's maximum at the switch; it was blurrier through mid-opening (0.65 vs 0.52 at 130 deg) (B5).
- `BlurTuning`: one 1 s-cached, sanitized source for `debug.duofold.preview_blur_max|start|smooth_ms` and
  `right_blur_clear`, used by the mirror, the hold and the right half, and the right half now uses the configured
  glide instead of a fixed 40 ms. A `setprop` mid-session gave different maxima on the two sides of the switch (B10).

Frames and captures

- The no-frame fallback gradient respects the unified darkening cap (0.55); it reached solid black, flashing the left
  half near-black whenever a frame was missing (B6). Below 6 % glass amount it draws nothing (it was drawn at the
  endpoints at 0.4-5 %, invisibly) (F1).
- **All models:** only the primary live glass resets the shared capture; frozen snapshots, the reflected strip and
  secondary-display surfaces wiped the primary frame at the switch in dual/native modes (B7).
- **All models:** each glass frame records the display it was captured from, and a surface accepts only frames from
  its source display (the reflected strip: display 0; others: their own). In continuity-native mode display-1 frames
  were checked against display 0 and never rendered (B8).
- **All models:** Duo's main glass surfaces are held by the service (`MainGlassSurfaces`) and stay skipped from
  screenshots only while the live cover->inner mirror is attached (skip-screenshot doubles as the mirror exclusion).
  With `debug.duofold.record_visible=1` the glass is recorded again whenever the mirror is not live, and is then
  excluded from Duo's own capture explicitly; captures no longer change what the user sees (B1).

Tests: StripGeometryTest (right-half fill, reflection scale, pose identity/continuity/monotonic, blur factor),
RightHalfBlurTest (left-curve match), GlassFallbackPolicyTest, BlurTuningTest, MainGlassSurfacesTest.

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
