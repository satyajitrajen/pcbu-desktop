# Delivery Phases

## Phase status values

Use only: `not-started`, `in-progress`, `blocked`, `complete`.

## Phase 0 — Overlay addresses and `tsIP`

- **Objective and scope:** Classify `100.64.0.0/10` vs LAN. Additive `tsIP` on the JSON listed in architecture. Persist `Pairing.lastTailscaleIpv4` and `PairedDevice.tsIP` without clobbering LAN fields. `0xB0` remains a raw id. No dual-path or Unlock PC UI yet.
- **Dependencies:** Approved feature PRD, design, architecture, rules.
- **Deliverables:** `OverlayIpv4` (Android) + desktop overlay check in `NetworkHelper`; optional `tsIP` parse/emit; storage additive fields; JVM/desktop tests for ignore-unknown and no-clobber.
- **Verification steps:** Tests pass if overlay is not written into `lastIpv4`/`ipAddress`. Old JSON without `tsIP` still parses. `0xB0` tests still treat payload as a string id.
- **Exit criteria:** FR-R2, FR-R4, FR-R24 in code for parse/store only.
- **Current status:** `complete`

## Phase 1 — Desktop wait: server for all methods + overlay client

- **Objective and scope:** During unlock wait, always bind `TCPUnlockServer` (UDP/TCP/BT). If `tsIP` is set, also `TCPUnlockClient` to the phone overlay IP. First SUCCESS cancels siblings. UDP broadcast includes `tsIP` when the PC has an overlay address. Learn phone `tsIP` from pair init / `0xB2` / `0xB3`.
- **Dependencies:** Phase 0.
- **Deliverables:** `UnlockHandler` dual-path; broadcast `tsIP`; persist `PairedDevice.tsIP`. Linux/macOS: no lock listener change.
- **Verification steps:** With a fake/local client: TCP pairing wait accepts inbound `0xB0` on 43296. With `tsIP` set, a TCP client is started. First success stops the other connection. 43298 still lock-only.
- **Exit criteria:** FR-R7–R10, FR-R16, FR-R23 (desktop side).
- **Current status:** `complete`

## Phase 2 — Android overlay learn + lock over Tailscale

- **Objective and scope:** Read/send phone overlay IP. Store PC `lastTailscaleIpv4` from UDP/`0xB1`/overlay peer. `LockPc` tries LAN then overlay. PC detail Tailscale spec row. Listeners on `0.0.0.0`. Pair init may send `tsIP`.
- **Dependencies:** Phase 0 (Phase 1 desktop needed for real `0xB1` `tsIP` / broadcast; JVM can fake packets).
- **Deliverables:** Store + spec row + LockPc host order; logs `path=lan|tailscale`; ktlint + unit tests.
- **Verification steps:** `test` + `ktlintCheck`. Missing overlay does not wipe `lastIpv4`. Lock with only overlay IP dials `100.x:43298`.
- **Exit criteria:** FR-R1–R6, FR-R11, FR-R19–R23 (Android lock/learn).
- **Current status:** `complete`

## Phase 3 — Unlock PC

- **Objective and scope:** PC detail dock per design (Unlock brass, Lock ghost). `UnlockPc`: resume in-flight session or dial 43296 LAN then overlay, `0xB0` first, plate + biometric, `0xB2`. Not-asking copy. FGS not started from this button.
- **Dependencies:** Phase 2. Hardware of Unlock PC needs Phase 1 desktop.
- **Deliverables:** `UnlockPc`, detail UI, plate reuse, JVM tests (in-flight, not-asking, no `passwordKey` on fail).
- **Verification steps:** `test` + `ktlintCheck` + `assembleDebug`. Cancel / not-asking never emit `passwordKey`.
- **Exit criteria:** FR-R12–R18, AC-R4 in code.
- **Current status:** `complete`

## Phase 4 — Hardware

- **Objective and scope:** Real phone, real Windows, Tailscale.com on both. Existing pairing, no re-QR. One home contact with Tailscale up, then cellular lock/unlock and Unlock PC.
- **Dependencies:** Phases 0–3. Copy `JarvisAuth.exe` over Program Files. Tailscale Windows service running at login.
- **Deliverables:** Sign-off in feature `memory.md`. No new surface.
- **Verification steps:** Feature PRD success metrics 1–7. Home unlock with Tailscale quit. Unlock PC while at the desktop → not-asking. Dual-path does not double-unlock.
- **Exit criteria:** AC-R1–R8 true on hardware.
- **Current status:** `in-progress`

## Phase structure

Every phase above includes: objective and scope, dependencies, deliverables, verification steps, exit criteria, current status.

Do not start Phase N+1 until Phase N is `complete`, except Phase 2 JVM tests may fake `tsIP` packets before Phase 1 is hardware-proven. Phase 4 is required before the feature is `complete`.
