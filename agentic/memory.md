# Project Memory

Do not record secrets, credentials, access tokens, or sensitive personal data.

## Approved decisions and rationale

- Product name: **JarvisAuth**. Desktop was rebranded from PC Bio Unlock; GitHub/MeisApps updater and marketing links were removed. GPL-3 remains.
- Android companion is a new first-party client so pairing does not depend on the Play Store PC Bio Unlock app.
- Stack: Kotlin + Jetpack Compose + BiometricPrompt. Flutter and React Native were rejected (background sockets + RFCOMM).
- v1 scope: QR/code pairing and unlock over UDP, TCP, and Bluetooth. Out: Wear OS, cloud, silent auto-unlock, Play Store, iOS.
- Application ID: `com.jarvisauth.app`.
- Code lives in this repo under `android/`.
- minSdk 26, targetSdk 35. Sideload APK. EncryptedFile for pairings. Foreground service for automatic methods. Device-credential fallback allowed.
- Protocol frozen to desktop for pairing/unlock: pairing 4.0.0, unlock 3.0.0, header `0xDB065AC7AFDFA4CC`, AES-256-GCM with **16-byte IV**, PBKDF2-HMAC-SHA256 65535, BT UUID `62182bf7-97c8-45f9-aa2c-53c5f2008bdf`. Do not rename `pcbuIP`/`pcbuPort`. Approved exceptions: Windows lock `0xB3`/`0xB4` on port 43298 (`agentic/features/lock-pc/`); lock proto `1.1.0` + inner `action` for forced shutdown/restart (`agentic/features/power-control/`); Tailscale overlay optional `tsIP` (`agentic/features/internet-reach/`). `0xB0` stays a raw pairing-id string.
- Design: locksmith graphite/brass, Sora + IBM Plex Mono (Android) / Segoe UI + Consolas (desktop), 2dp armed rail, full-screen unlock plate. Layout is index + dock. Desktop Qt is the same tokens, not Material red/teal. Desktop close hides to the tray; Quit is on the tray menu.
- Architecture: raw sockets, ZXing (no ML Kit/GMS), manual DI, no Hilt/Room/Firebase. FGS owns listeners. UI must not import transport.
- Web frontend standards (React, Tailwind, TanStack) do not apply to `android/`.
- Delivery: Phases 0–6 in `phases.md`, one phase complete before the next starts.

## Current progress

| Document | Status |
|---|---|
| Discovery brief | approved |
| `agentic/prd.md` | approved |
| `agentic/design.md` | approved |
| `agentic/architecture.md` | approved |
| `agentic/rules.md` | approved |
| `agentic/phases.md` | approved |
| `agentic/memory.md` | approved |

| Phase | Status |
|---|---|
| 0 Android skeleton | complete |
| 1 Protocol and crypto | complete |
| 2 Pairing | complete |
| 3 Listener and UDP unlock | complete |
| 4 TCP unlock | complete |
| 5 Bluetooth unlock | complete |
| 6 Multi-PC, manual UDP, polish | in-progress |

Phase 0–5 are complete in code (UDP/TCP confirmed on hardware; Bluetooth implemented, hardware-dependent). Phase 6 code is in; hardware still needed before Phase 6 is `complete`. Feature lock-pc: Phase 0 desktop listener is in code (hardware happy path done; quit-desktop / post-lock unlock not fully re-checked). Feature internet-reach: Phases 0–3 are `complete` in code; hardware (Phase 4) is required before the feature is `complete`. Feature power-control: docs approved; Phases 0–1 complete in code; hardware (Phase 2) required before the feature is `complete`.

## Important context

- Desktop still uses CMake targets like `pcbu_desktop`; user-facing binary/name is JarvisAuth. Login DLLs (`win-pcbiounlock.dll`, `pam_pcbiounlock.so`) keep old filenames so OS hooks do not break.
- Desktop settings path is `C:\ProgramData\JarvisAuth` / `/etc/jarvis-auth`. Old PC Bio Unlock pairings do not migrate.
- Desktop UDP unlock broadcast is **plaintext JSON**. Pairing TCP and unlock TCP payloads are AES. Copy that split; do not “fix” it on one side.
- Desktop UDP pairing also opens a TCP unlock server (fall-through in `UnlockHandler`). Phone UDP path connects to `pcbuIP:pcbuPort`.
- Phone never stores the PC password; it stores `passwordKey`. PC stores AES-wrapped password.
- Pairing QR contains `encKey` in plaintext by design. Current QR is `{serverId, port, method, encKey}` where `port` is the **UDP discovery** port (default 43297). The phone decrypts the AES UDP beacon, then TCP-connects to `beacon.ip:beacon.port` (pairing TCP, default 43295). Legacy QR `{ip, port, method, encKey}` skips discovery. Typed code is unpadded Base32 of the same JSON, grouped with dashes every 5 characters.
- Packet timestamp skew > 120s is a hard fail.
- Desktop auto-updater to meis-apps.com was disabled; Android must not call that host.

## Active risks

- Crypto/framing mismatch with desktop (16-byte GCM IV is easy to get wrong).
- OEM battery killers vs FGS listener.
- Bluetooth RFCOMM unreliable on many phones.
- Full-screen unlock intent suppressed on some OEMs.
- Stolen unlocked phone + PC blob ≈ password (not true 2FA).

## Unresolved noncritical questions

- Custom launcher icon vs monochrome key glyph for v1.
- Whether Play Store happens after v1 (not in this release).
- Exact debug overlay beyond the in-app log ring.

## Changes affecting future work

- Any desktop protocol change requires a paired Android change and a PRD/architecture update before code.
- Reinstalling the Android app regenerates uuid/ports → user must re-pair.
- Raising minSdk or adding Hilt/Room/Firebase requires a rules exception, not a silent dependency add.
