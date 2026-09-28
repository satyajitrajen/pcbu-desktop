# Project Memory

Do not record secrets, credentials, access tokens, or sensitive personal data.

## Approved decisions and rationale

- Feature: lock a **logged-in Windows** session from the phone. Post-v1 companion work; not Android Phase 6.
- Protocol exception: add lock only. Pairing 4.0.0 and unlock 3.0.0 stay frozen. New lock proto `1.0.0`, packets `0xB3` / `0xB4`, port **43298** (not 43296, because login unlock uses 43296 after Win+L).
- Windows only. No Linux, macOS, or Bluetooth lock.
- Tap to lock. No biometric. No second confirm.
- Existing pairings work. Same `encryptionKey`. No re-pair. Phone always uses 43298 in v1.
- Reachability: last IPv4 the phone saw (pair / UDP broadcast / inbound TCP). No logged-in beacon.
- Desktop GUI process listens; closing the window hides to the tray and keeps lock alive. Tray **Quit** stops JarvisAuth.exe and disables lock. `LockWorkStation` from that session.
- Stolen unlocked phone can lock the session. Accepted.
- FGS is not used for lock.
- Forced shutdown/restart is not this feature. See `agentic/features/power-control/` (same `0xB3`/`0xB4`, proto `1.1.0`).

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
| 0 Desktop lock listener | in-progress |
| 1 Android lock client | complete |
| 2 Hardware | not-started |

## Important context

- Desktop `PairedDevice.id` is SHA256(machineID + phone UUID + userName), not the raw phone uuid. Lock `0xB3` outer `deviceId` is that pairing id (same string the phone stores as `Pairing.deviceId`). Inner `pcDeviceId` must match it.
- Unlock `needsDeviceID` / `0xB0` does **not** apply on the lock socket.
- After upgrade, `lastIpv4` is empty until one LAN contact. BT-only pairings that never appear on Wi-Fi cannot lock.
- Project `agentic/` docs include a lock exception: packets `0xB3`/`0xB4`, port 43298, Windows GUI listener. Unlock/pairing remain frozen.
- Phase 1 debug APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Desktop listener binary: `pkg/build/desktop/Release/JarvisAuth.exe`. Both required for Phase 2. Feature is not complete until hardware.
- 2026-08-17: lock “Couldn’t reach” on hardware was the **installed** `C:\Program Files\JarvisAuth\JarvisAuth.exe` (no lock listener). In-app Install only copies `win-pcbiounlock.dll`, not the GUI exe. Nothing was bound on 43298. Phone `lastIpv4` was fine (`192.168.1.7`). After replacing Program Files with the 01:03 build, Lock PC locked the session (Phase 2 happy path). Quit-desktop reachability and post-lock unlock were not re-checked in that pass.
- 2026-08-17: Android layout refresh — index + dock, spec sheet on PC detail, Quiet text for Close/Logs/Pair. Tokens unchanged.
- 2026-08-17: Desktop Qt shell restyled to the same locksmith tokens (Night/Brass, index + dock). In-app Install still does not replace `JarvisAuth.exe`; copy `pkg/build/desktop/Release/JarvisAuth.exe` over Program Files.
- 2026-08-17: Desktop close (X) hides to the system tray. Tray menu **Quit** is the only user quit. Second launch activates the existing process.

## Active risks

- Stale DHCP IP.
- Windows firewall on 43298.
- Desktop not running in the session.
- Mis-tap locks the session at the desk.

## Unresolved noncritical questions

- None for this slice. Installer firewall is program-based for `JarvisAuth.exe` (no extra 43298 rule). “Locked.” auto-clears after 1200ms.

## Changes affecting future work

- A later pairing field for custom `lockServerPort` would be needed if someone changes 43298 on the PC.
- Bluetooth lock would be a new feature: PC as RFCOMM server, opposite of today’s unlock direction.
- Linux lock would be a new feature (`loginctl` / session D-Bus), not a flag on this slice.
