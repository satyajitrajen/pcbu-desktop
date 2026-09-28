# Delivery Phases

## Phase status values

Use only: `not-started`, `in-progress`, `blocked`, `complete`.

## Phase 0 — Desktop lock server 1.1.0

- **Objective and scope:** Windows `LockServer` accepts proto `1.0.0` as lock; accepts `1.1.0` with inner `action` `shutdown`/`restart`/`lock`; ACK `0xB4` then `ExitWindowsEx` + `EWX_FORCE` after `SE_SHUTDOWN_NAME`. Linux/macOS unchanged. No Android UI yet.
- **Dependencies:** Approved feature PRD, design, architecture, rules. Existing lock listener and pairing store.
- **Deliverables:** Dual proto accept; unknown `action` → `DATA_ERROR`; other proto → `PROTOCOL_ERROR`; decrypt fail does not power or lock; installer unchanged (same 43298 hole).
- **Verification steps:** Local client: `1.0.0` still `LockWorkStation`; `1.1.0` without action is `DATA_ERROR`; `1.1.0` + shutdown/restart hits the force path (stub or hardware). 43296 still unlock.
- **Exit criteria:** Desktop distinguishes lock vs force-power by proto + `action`. Old exact-match-only behavior is gone.
- **Current status:** `complete`

## Phase 1 — Android power row

- **Objective and scope:** `LockHandshake` / `LockPc` send `1.1.0` + `action` for power; Lock PC stays `1.0.0`. PC detail: Shut down | Restart under Lock. Same hops as lock. Tests.
- **Dependencies:** Phase 0 desktop for hardware; JVM can use a fake lock server.
- **Deliverables:** Design dock order, captions, redacted logs with `action=`, `ktlintCheck` + unit tests.
- **Verification steps:** `test` + `ktlintCheck` + `assembleDebug`. Assert lock JSON omits `action`; power JSON includes it; no `passwordKey`.
- **Exit criteria:** FR-W1–W5, W11 in code. No pairing schema change.
- **Current status:** `complete`

## Phase 2 — Hardware

- **Objective and scope:** Real phone + real Windows. New APK + new Program Files exe. Shut down, Restart, Lock, old-exe fail-close, overlay if internet-reach IPs exist.
- **Dependencies:** Phase 0 and 1. User copies `pkg/build/desktop/Release/JarvisAuth.exe` over Program Files after rebuild.
- **Deliverables:** Manual sign-off in memory. No new surface area.
- **Verification steps:** AC-W1–W5. Unlock 43296 still works. Tray Quit → reachability, PC unchanged.
- **Exit criteria:** Feature PRD success metrics 1–5 true on hardware.
- **Current status:** `not-started`

## Phase structure

Every phase above includes: objective and scope, dependencies, deliverables, verification steps, exit criteria, current status.

Do not start Phase N+1 until Phase N is `complete`, except JVM tests in Phase 1 may use a fake server. Phase 2 is required before the feature is `complete`.
