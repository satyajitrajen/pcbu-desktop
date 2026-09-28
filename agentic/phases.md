# Delivery Phases

## Phase status values

Use only: `not-started`, `in-progress`, `blocked`, `complete`.

## Phase 0 — Android skeleton

- **Objective and scope:** Create `android/` Gradle project (`com.jarvisauth.app`), Compose Material3 with design tokens (Night/Brass/Sora/Plex Mono), single activity, Home empty state, ktlint/editorconfig, gitignore for keystores. No sockets yet.
- **Dependencies:** Approved PRD, design, architecture, rules.
- **Deliverables:** Debug APK that launches to JarvisAuth empty home with Pair PC (button can be a stub destination). `android/README.md` with assemble/install commands.
- **Verification steps:** `assembleDebug` succeeds. Install on a device/emulator; empty home matches design (armed rail steel, brand type, no Material purple).
- **Exit criteria:** App id is `com.jarvisauth.app`; no MeisApps strings; `allowBackup=false`.
- **Current status:** `complete`

## Phase 1 — Protocol and crypto

- **Objective and scope:** Implement `protocol/` framing and AES-GCM packet codec as a byte clone of desktop `BaseConnection` / `CryptUtils`. JVM golden tests. No UI networking.
- **Dependencies:** Phase 0.
- **Deliverables:** `AesGcmPacket`, `PacketCodec`, pair/unlock JSON models, tests for IV/salt/tag/timestamp/PBKDF2-65535.
- **Verification steps:** Unit tests pass. At least one encrypt/decrypt round-trip and one known-vector test. Tests fail if IV length is 12.
- **Exit criteria:** Rules for crypto tests satisfied. 2KB payload cap enforced.
- **Current status:** `complete`

## Phase 2 — Pairing

- **Objective and scope:** CameraX+ZXing QR and typed-code pair flow. UDP beacon discovery + TCP pair client. Persist pairings in EncryptedFile. Pairing ports/uuid generated once.
- **Dependencies:** Phase 1. Running JarvisAuth desktop in pairing mode on the same LAN.
- **Deliverables:** Pair scanner/code/connecting/error screens. `PairingStore`. Successful pair shows the PC on Home.
- **Verification steps:** Scan current desktop QR; desktop lists the phone; Home shows PC name and method. Protocol mismatch and time-skew errors show PRD copy. Abandon mid-pair leaves no record.
- **Exit criteria:** FR-P1–P7 for UDP/TCP/BT method values stored (BT address is a desktop concern; phone still advertises ports). FLAG_SECURE on pair screens.
- **Current status:** `complete`

## Phase 3 — Listener and UDP unlock

- **Objective and scope:** Foreground service, Start/Stop listening, UDP broadcast receive, TCP connect to `pcbuIP:pcbuPort`, unlock handshake, full-screen unlock plate + BiometricPrompt.
- **Dependencies:** Phase 2. Desktop paired with Automatic UDP.
- **Deliverables:** `UnlockForegroundService`, unlock plate UI, FGS notification, battery-optimization hint. Home rail brass when armed.
- **Verification steps:** Start listening; lock or login on PC; plate appears; Confirm + biometric unlocks. Cancel leaves PC locked. Force-stop app → no unlock. Logs contain no keys.
- **Exit criteria:** PRD success metrics 1, 2, 4, 6 for UDP. AC5–AC7.
- **Current status:** `complete`

## Phase 4 — TCP unlock

- **Objective and scope:** Phone TCP server on persisted `tcpPort`. PC Automatic TCP pairing connects in; same plate/handshake as Phase 3.
- **Dependencies:** Phase 3. Re-pair or pair a device with TCP method.
- **Deliverables:** `TcpUnlockServer` started by FGS when any pairing is TCP.
- **Verification steps:** TCP-paired PC unlocks via biometric. UDP still works. Two PCs: matching `deviceId` only.
- **Exit criteria:** FR-U2 true on hardware. Success metric 3 (TCP).
- **Current status:** `complete`

## Phase 5 — Bluetooth unlock

- **Objective and scope:** RFCOMM server with UUID `62182bf7-97c8-45f9-aa2c-53c5f2008bdf`. Runtime BT permissions. Same handshake/plate.
- **Dependencies:** Phase 4. Desktop Automatic Bluetooth pairing (phone BT discoverable/bonded as desktop requires).
- **Deliverables:** `RfcommUnlockServer`. BT errors surfaced in UI, no infinite retry.
- **Verification steps:** Hardware only. PC BT unlock succeeds once; cancel fails closed. Emulator does not count.
- **Exit criteria:** FR-U3 true. Success metric 3 (Bluetooth).
- **Current status:** `complete`

## Phase 6 — Multi-PC, manual UDP, polish

- **Objective and scope:** Second PC pairing, PC detail + remove, MANUAL_UDP listen, overflow logs, copy/tokens audit, release signing gitignore.
- **Dependencies:** Phase 5 (can overlap with 4–5 for remove/logs if those land earlier, but exit waits for BT).
- **Deliverables:** Detail/remove, in-app log viewer with redaction, manual UDP path, `android/README.md` install notes.
- **Verification steps:** Two PCs unlock independently. Remove → subsequent request `NOT_PAIRED`. Manual UDP works. Design tokens consistent. `assembleRelease` with local keystore.
- **Exit criteria:** All PRD success metrics 1–6. v1 Definition of done in `rules.md`.
- **Current status:** `in-progress`

## Phase structure

Every phase above includes: objective and scope, dependencies, deliverables, verification steps, exit criteria, current status.

Work one phase at a time. Do not start Phase N+1 until Phase N is `complete`, except notes in Phase 6 that remove/logs may be implemented during 4–5 but Phase 6 is not `complete` early.
