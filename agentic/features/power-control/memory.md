# Project Memory

Do not record secrets, credentials, access tokens, or sensitive personal data.

## Approved decisions and rationale

- Feature: forced Windows **shutdown** and **restart** from the phone. Not sleep/hibernate/WoL. Not Linux/macOS.
- Same lock channel: port **43298**, packets **`0xB3`/`0xB4`**, AES as lock. No new IDs, no new port.
- Lock PC stays proto **`1.0.0`** with no `action` so an old exe still locks.
- Power uses proto **`1.1.0`** and encrypted inner `action` `shutdown` | `restart`. Old exe exact-matches `1.0.0` → `PROTOCOL_ERROR`, does not lock and does not power off.
- New desktop **accepts both** `1.0.0` (lock) and `1.1.0` (action). Must not require only `1.1.0`.
- One tap. No biometric. No confirm. Stolen unlocked phone + Tailscale can do this. Accepted.
- Forced: `ExitWindowsEx` + `EWX_FORCE` after `SE_SHUTDOWN_NAME`. Unsaved work is discarded.
- Write `0xB4` success **then** power. FGS not used.
- UI: Unlock brass, Lock ghost full-width, then quieter **Shut down | Restart**, then Remove.
- Overlay: same host order as lock (`lastIpv4` then `lastTailscaleIpv4`). Pairing still LAN-only.
- After docs in this folder are all approved: strike lock-pc non-goal “remote shutdown”; add the power row to internet-reach / project design dock notes. **Done 2026-08-19.**

## Current progress

| Document | Status |
|---|---|
| Discovery brief | approved |
| `prd.md` | approved |
| `design.md` | approved |
| `architecture.md` | approved |
| `rules.md` | approved |
| `phases.md` | approved |
| `memory.md` | approved |

| Phase | Status |
|---|---|
| 0 Desktop lock server 1.1.0 | complete |
| 1 Android power row | complete |
| 2 Hardware | not-started |

## Important context

- Install trap: in-app Install does not replace `C:\Program Files\JarvisAuth\JarvisAuth.exe`. Power needs that copy after a desktop rebuild, then tray Quit + relaunch.
- `action` is inner encrypted JSON only. Outer `0xB3` stays `protoVersion` / `deviceId` / `encData` / optional `tsIP`.
- Phone never sends `0xB0` on 43298.
- `GetLockProtocolVersion()` remaining `1.0.0` is correct for Lock PC; the server must not exact-match that string for all traffic.
- 2026-08-19: Phase 0 compiled. `LockServer` accepts `1.0.0` lock and `1.1.0` + `action`. ACK then `ExitWindowsEx` FORCE. Release exe: `pkg/build/desktop/Release/JarvisAuth.exe` (03:11). Copy over Program Files before hardware. Force-off is unproven until Phase 2.
- 2026-08-19: Phase 1 Android: Lock PC stays `1.0.0`; Shut down/Restart send `1.1.0` + inner `action`. ktlint + unit tests + assembleDebug passed. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Active risks

- Stolen unlocked phone + overlay = remote force-off.
- ACK lost if the TCP dies at shutdown (phone may show Unreachable after the PC already accepted).
- Old Program Files exe still running while a new APK is installed.
- Missing `SE_SHUTDOWN_NAME` → `APP_ERROR`.

## Unresolved noncritical questions

- “Shut down” (two words, Windows-style) vs a single “Shutdown” token. Frozen as **Shut down**.
- Power row uses 48dp for a11y even though it is “quieter” than Lock (ghost, not brass).
- `action=` in info logs (not a secret). Frozen as allowed.

## Changes affecting future work

- Sleep/hibernate/WoL would be a new feature (and WoL needs a machine that is already off).
- Custom `lockServerPort` still needs a later pairing field (same as lock-pc).
- Linux power would be a new feature, not a flag here.
