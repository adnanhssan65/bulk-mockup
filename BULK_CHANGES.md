# Bulk Mockup — Technical Notes (changed files, decisions, risks, alternatives)

Base: termux/termux-app @ `8629e632fcb95da272221be327db653fb24befe9` (0.118.0),
fork versionCode 119 / versionName "0.118.1". Pinned versions: see `PINS.txt`.

## The two structural decisions

### 1. applicationId `com.termux` — owner's final decision (2026-11-14)
The fork first implemented Override F (`com.mockup`, side-by-side install,
same-length path rewrite). The owner then **chose `com.termux`** instead —
simpler and far more reliable, at the accepted cost that the app **cannot
coexist with official Termux** on one phone: because the package name is the
same but the signing key differs, official Termux must be uninstalled before
Bulk Mockup installs (its internal app data is removed by that uninstall;
Bulk Mockup ships its own complete environment, and the shared-storage
project folders `/storage/emulated/0/mockup_project_*` are untouched).
Concretely, staying on `com.termux` means the upstream defaults simply stay:
`applicationId "com.termux"`, `TERMUX_PACKAGE_NAME` placeholder and
`TermuxConstants.TERMUX_PACKAGE_NAME` are the official values, and
`android:sharedUserId` remains **removed** (not needed — the API functions
are integrated — and it avoids shared-user signature conflicts).
The `com.mockup` implementation is preserved in git history (commit
`70f53ad`) should side-by-side install ever be wanted again.

### 2. Pre-installed packages are built in CI (Method A) — no rewrite needed
**Method B was rejected on evidence**: `scripts/generate-bootstraps.sh` in
termux-packages reads package lists from a *single* repository base URL with a
hardcoded `/dists/stable/main/` path; `opencv-python` lives in the termux-x11
repo (dist `x11`), so the stock script cannot resolve it without patching
upstream tooling.

**Method A (chosen)**: on a GitHub arm64 runner, the official
`termux/termux-docker:aarch64` image installs exactly the owner's set
(`pkg update`, `x11-repo`, then `python python-numpy opencv-python
python-pillow python-pip dbus nano curl wget zip unzip`, `pip install tqdm`),
verifies `SAB OK` inside the container, and tars `$PREFIX`. The tar is cached
between CI runs (monthly refresh + package-list hash) and embedded in the
APK as-is. Because the applicationId stayed `com.termux`, every hardcoded
path in the official packages is already correct — **no path rewriting, and
`pkg`/`apt` keep working normally** (new packages can be installed later,
exactly like stock Termux).

### 3. Termux:API integrated — ONE APK (Override A)
The owner's scripts use exactly `termux-media-scan` (file and `-r`),
`termux-wake-lock`, `termux-wake-unlock` (verified by grepping his real
scripts). Implementation:
- `BulkApiServer` (new, in `com.termux.app`) runs inside `TermuxService`
  (started in `onCreate`, stopped in `onDestroy`) and listens on a
  **filesystem** unix socket at `/data/data/com.termux/files/bulk_api.sock`
  (app-private directory → only this app's processes can connect).
  Connections are handled sequentially, so scan order is exactly the order
  the scripts request. `SCAN` waits for `MediaScannerConnection`
  `onScanCompleted` (15 s safety cap per file — it can never hang forever);
  `SCANR` walks the directory and scans in (mtime, name) order; `WAKELOCK` /
  `WAKEUNLOCK` hold/release a partial wake lock.
- The commands themselves are small Python wrappers
  (`app/src/main/assets/bulk/bulk_api_client.py`), stamped with the socket
  path and installed into `$PREFIX/bin` at first run by `BulkMockupSetup`.
  If the app service is not running, a wrapper exits 1 within ~2 s with a
  clear message — it never hangs and never prints "Termux:API not installed".
  Locally tested: no-server fail-fast (2.0 s, exit 1) and in-order delivery
  against a mock server (exit 0).
- The `termux-api` package is **not** installed; `am broadcast` (used by the
  owner's `system_core.py`) is an Android framework command and is untouched.

## Files changed / added in the fork

| File | Change |
|---|---|
| `app/build.gradle` | applicationId + package placeholder stay com.termux (owner decision); app name "Bulk Mockup"; versionCode 119 / 0.118.1; commons-compress dep (tar extraction); env-based `bulk` signing config; `noCompress 'gz'`; arm64-v8a only (splits + abiFilters); aarch64-only bootstrap download. targetSdkVersion stays **28**. |
| `termux-shared/.../TermuxConstants.java` | Unchanged value `com.termux` (comment notes the owner's decision). |
| `app/src/main/AndroidManifest.xml` | Removed `sharedUserId`; added `BulkPermissionsActivity`, `BulkUpdateActivity`, and a FileProvider (`${applicationId}.bulkfiles`) for update installs. (Authorities/permissions derive from the placeholder.) |
| `app/src/main/java/com/termux/app/BulkMockupSetup.java` | **NEW.** First-run: free-space check (≥1.5 GB) → stage-2 usr extraction into a **staging dir + atomic rename swap** (interruption-safe, retry dialog) → seed `~/.bashrc`, `~/.termux/colors.properties`, `~/projects.conf` **only if missing** → install bin wrappers → marker file **last** → opens the Permissions screen. Updates never re-run it. |
| `app/src/main/java/com/termux/app/BulkApiServer.java` | **NEW.** See decision 3. |
| `app/src/main/java/com/termux/app/ProjectsConf.java` | **NEW.** Pure-Java projects.conf parser (unit-testable): trims, skips blanks/comments/malformed lines, never crashes. Default content = Project 1–10 with w/p/d placeholders. |
| `app/src/main/java/com/termux/app/ProjectsLauncher.java` | **NEW.** Drawer Projects dialog using ProjectsConf; W/P/D type the command + Enter into the current session (creates one if none); **busy guard** — if the shell has a running child process, it does not type and shows "Pehla kaam chal raha hai"; 800 ms double-tap debounce; "Edit list" editor with Save; the conf file is re-read on every open and never overwritten by the app except via explicit Save in the editor. |
| `app/src/main/java/com/termux/app/BulkPermissionsActivity.java` | **NEW.** Permissions screen: Storage (real write/read/delete test in /storage/emulated/0 + `setupStorageSymlinks` on grant), Battery (isIgnoringBatteryOptimizations), Notifications (API 33+), Install-unknown-apps, brand Background/Auto-start (Xiaomi/Oppo/Realme/Vivo/Huawei/Samsung intents with app-details fallback) — each row ✔/✖ + fix button, statuses rebuilt on every resume, plus an **Allow all** walk-through. First-run opens this same screen. |
| `app/src/main/java/com/termux/app/BulkUpdateActivity.java` | **NEW.** The only network feature, user-tapped only: GET latest GitHub Release of `GITHUB_REPO` (one constant — must be set to the owner's repo), parse `v<versionCode>` tag, compare with installed, show notes, download `BulkMockup.apk`, install via FileProvider. Simple messages for offline/no-permission/download-failure. |
| `app/src/main/java/com/termux/app/TermuxActivity.java` | Wrapped the bootstrap completion callback with `BulkMockupSetup.runIfNeeded(...)`; added drawer-button wiring (`setProjectsButtonView` extended: Projects, Permissions, Check for updates, About dialog with app name + version). |
| `app/src/main/java/com/termux/app/TermuxService.java` | Two lines: `BulkApiServer.start(this)` in onCreate, `BulkApiServer.stop()` in onDestroy. |
| `app/src/main/res/layout/activity_termux.xml` | Four large buttons in the left drawer, in order: PROJECTS, PERMISSIONS, CHECK FOR UPDATES, ABOUT (above the sessions list; the rest of the drawer is stock). |
| `app/src/main/res/xml/shortcuts.xml` | Unchanged (`com.termux`). |
| `app/src/main/res/xml/bulk_file_paths.xml` | **NEW.** FileProvider paths for the update APK. |
| `app/src/main/res/drawable/ic_foreground.xml`, `res/mipmap-*/ic_launcher*.png` | Bulk Mockup icon (green terminal glyph). |
| `app/src/main/assets/bulk/` | **NEW.** `bashrc.txt` (placeholder w/p/d aliases using the `wN.py` pattern, projects 1–10), `projects.conf` (defaults), `colors.properties` (green-on-black), `bulk_api_client.py` (wrapper template). Placeholders are deliberate (spec §4.3); the owner edits them once per phone — or edits these repo files so future builds ship his real shortcuts. |
| `app/src/test/java/com/termux/app/ProjectsConfTest.java` | **NEW.** JUnit tests for the parser (normal lines, blanks/comments, extra spaces, missing columns, empty parts, garbage, defaults). |
| `ci/project1_test.py`, `ci/fixtures/mockup_project_1/` | **NEW.** Override H suite: the owner's real project-1 folder (fixtures) + generated inputs; process HD/Normal, batch, every delete-menu option incl. backup/restore, system_core suite, 6000×8000 perf, 20-image stress — run in the container against the embedded usr tree. |
| `ci/packages-key.txt`, `PINS.txt` | **NEW.** Cache key material; pinned versions record. |
| `.github/workflows/bulk-build.yml` | **NEW.** Two jobs (arm64 packages+tests → x86 APK build+manifest checks+release `v<versionCode>`). |
| `.gitignore` | Ignores the generated usr asset. |
| `keys/` (outside repo) | Release keystore generated locally; **never committed** — goes to GitHub Secrets + owner backup. |

No changes to terminal-emulator, terminal-view, the owner's scripts, or any
project folder logic.

## Risks and how each is handled (G2)

1. **Package/prefix breakage** → no rewrite exists to break anything: the
   in-CI container test mounts the embedded usr at its real path and runs
   SAB OK, real cv2 ops (imread/resize/warpPerspective), PIL JPEG save
   **with ICC**, an import sweep of site-packages, and the full Project-1
   suite against the owner's real scripts.
2. **Coexistence with official Termux** → impossible by the owner's choice
   (same package name, different signature): official Termux must be
   uninstalled first; this is stated in the README install steps and in the
   release notes. CI asserts the built manifest has package `com.termux`
   and no sharedUserId.
3. **Android versions / targetSdk 28** → targetSdk kept at 28 (exec-from-data
   exemption). Emulator matrix NOT run (same arm64 reason); the "built for an
   older Android" prompt is documented in the README; owner's phones span
   several Android versions and are the real test.
4. **First-run extraction** → staging dir + atomic rename swap, free-space
   pre-check, marker written last, error dialog with Retry. Kill-during-extract
   is safe by construction (old usr untouched until swap); not yet exercised
   on a device — phone checklist territory.
5. **Storage on Android 11+** → status is a real file test, not a flag; the
   app requests the legacy permission like stock Termux (targetSdk 28).
   Fallback if a brand blocks it: MANAGE_EXTERNAL_STORAGE flow (not built;
   would be added only if a real phone shows the problem).
6. **media-scan ordering/hangs** → sequential server, per-file completion
   wait with 15 s cap, wrapper fail-fast 2 s. MediaStore *database* ordering
   can only be verified on a device (checklist item 3/9).
7. **Launcher edge cases** → busy guard via `/proc/<shellpid>/task/*/children`,
   debounce, parser unit tests (JUnit in CI + a local harness run: 8/8 pass).
8. **Update failures** → distinct simple messages for offline, missing
   install permission (routes to Permissions screen), interrupted download
   (partial file rejected by size check, retry). Wrong-signature installs are
   rejected by Android itself (all builds share the one keystore).
9. **APK size** → arm64 only; size printed in CI and in the release notes.
   If it ever exceeds practical limits, the alternative is a small APK that
   downloads the package pack on first run (needs internet once).
10. **Key loss** → README step 2 is an explicit two-place backup warning.
11. **Upstream drift** → base commit + image tag pinned in `PINS.txt`;
    `dpkg -l` archived per build.
12. **Background kills** → foreground service + notification are stock
    Termux; wake lock via the built-in command; Permissions screen covers
    battery/auto-start. Cannot be fully tested — checklist item 5.
13. **Phone brands** → NOT testable in CI; checklist item 7.

## Alternatives (G3) — kept ready, not chosen

- **Side-by-side install ever needed → the `com.mockup` route** (already
  implemented once, preserved in git history at `70f53ad`): rename the app
  and either byte-rewrite the package paths (same-length names) or rebuild
  packages from source with `TERMUX_APP__PACKAGE_NAME=com.mockup`. Cost:
  rewrite risk or hours of package-building CI, and `pkg` stops working
  with the official repos. Not needed while replacing official Termux is
  acceptable.
- **API integration too hard → `am broadcast` wrappers**: simpler (no
  server), but broadcasts are fire-and-forget — no completion wait, weaker
  ordering guarantees. Last resort: a second APK (rejected by the owner —
  spec demands exactly one APK).
- **Fork approach too fragile → a normal Chaquopy app**: Python + numpy +
  OpenCV + Pillow inside a regular Android app with a terminal-style
  screen; one APK, modern Android rules, no package-name tricks. But the
  owner's `termux-*` script calls need replacements, there is no general
  shell (`nano`, arbitrary commands gone), and project workflows change.
- **APK too big → first-run download of the package pack**: small APK, but
  the "works offline on a fresh phone" guarantee is lost for the first run.
