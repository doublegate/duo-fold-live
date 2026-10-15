# Current release baseline — 3.5.4

User approved 3.5.4-alpha.7 cover-close behavior for stable promotion on 2026-10-12 after reporting that the full-close blackout totally disappeared on SM-F971U / Android 17. Stable 3.5.4 adds requested Smooth (120 ms default) / Fast (30 ms) presets and removes the unused closing power-pulse experiment. The tested closure behavior and geometry are retained.

- Tested alpha: [ci-161-1](https://github.com/joeconsorti/duo-fold-live/releases/tag/ci-161-1), source `4320933c746e34f933b4e2fe86c283a7de935640`.
- Stable: [v3.5.4](https://github.com/joeconsorti/duo-fold-live/releases/tag/v3.5.4).
- Preserve all historical releases and signing identity. Optical behavior still requires handset testing; the separate mid-fold switch and wallpaper unlock flash are not claimed fixed.

## Historical wallpaper rollback record

# Wallpaper rollback baseline — preserve alpha.13

**Current user-approved best version: 3.5.1, based on the user-tested 3.5.1-alpha.9 and approved for main/latest on 2026-09-26.** The user still observes occasional immediate/delayed unlock flashing. Alpha.13 remains the preserved original partly working rollback baseline; do not delete or rewrite it. Automated checks alone do not establish visual correctness.

- Release/tag: [ci-27-1](https://github.com/joeconsorti/duo-fold-live/releases/tag/ci-27-1)
- Original signed APK: [Duo-Fold-Live-ci-27-1.apk](https://github.com/joeconsorti/duo-fold-live/releases/download/ci-27-1/Duo-Fold-Live-ci-27-1.apk)
- Exact source commit: `83e40e827238da11b2eee6a3d95ce038f6b57bea`
- Source branch: `alpha/3.0.4-alpha.13`
- User confirmation: “NO FLASH ANYMORE! WE FIXED IT! (with one issue).”
- Known limitation: the flash remains when the device stays awake at the lock screen for several seconds before unlocking. Quick wake/unlock works according to the user.

Keep the original release, assets, tag and source commit intact. If a later experiment regresses, restore this source in a new build with a higher version code so users can update without uninstalling or losing settings. An original APK downgrade may be refused by Android.

## Rejected experiment and recovery

- Alpha.14 (`ci-28-1`, source `7dd629ebce55afcbeb043299338488937662c875`) was rejected by the user: worse unlock flashing and a new large flicker after applying wallpaper. Do not use it as a baseline.
- Alpha.15 restores alpha.13 runtime source exactly, with a higher version code for installation over alpha.14. It is a recovery build, not a fix for the remaining delayed-unlock issue. Alpha.13 remains the only user-confirmed partly working baseline.
