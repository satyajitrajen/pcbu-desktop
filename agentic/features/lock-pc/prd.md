# Product Requirements Document

## Product vision

JarvisAuth can lock a logged-in Windows session from the paired phone with one tap on the LAN. The phone stays a key holder: it does not gain a dashboard, and it never sends `passwordKey` to lock.

## Problem statement

Unlock is PC-initiated. Once logged in, walking away requires Win+L or the keyboard. The phone already has a pairing and a LAN path, but no command to lock the session.

## Target users and needs

**Primary user:** Satyajit, with JarvisAuth on his Windows PC and the first-party Android app.

Need: open the paired PC on the phone and lock that session without touching the PC.

Not a target: Linux, macOS, Bluetooth lock, locking the login screen, Play Store users.

## Goals

1. Lock the current Windows user session from the phone over LAN TCP.
2. Reuse existing pairings and `encryptionKey`. No re-pair.
3. Fail closed if the PC is unreachable, the desktop app is not running, or crypto/time/device checks fail.
4. Keep unlock packets and ports unchanged.

## Non-goals

- Linux, macOS, Bluetooth, or cloud lock.
- Biometric (or any second factor) to lock.
- Always-on “I am logged in” UDP beacon.
- Locking at the Windows login/credential UI.
- Silent lock without a user tap.
- Changing unlock 3.0.0 / pairing 4.0.0 field names or `0xB0`–`0xB2`.
- Sleep, hibernate, or Wake-on-LAN. Forced shutdown/restart is `agentic/features/power-control/`.

## Use cases and user stories

**Lock from PC detail**  
As a user on the same Wi-Fi, I open a paired Windows PC and tap Lock PC. The session locks. I see a short success or a one-line failure.

**Existing pairing**  
As a user who already paired, I install the new phone and desktop builds and lock without scanning a QR again.

**Unreachable PC**  
As a user whose PC changed IP or who used tray **Quit** (process gone), I see that the PC could not be reached. Closing the window to the tray does not count as quit. The session is not affected.

**Stolen unlocked phone**  
As an attacker with the unlocked phone, I can lock the session (accepted). I still cannot obtain `passwordKey` from this flow.

## Functional requirements

- FR-L1. PC detail shows **Lock PC** for a pairing that has a stored IPv4. If the IP is missing, show why (need one LAN contact) instead of a silent no-op.
- FR-L2. One tap sends the lock command. No biometric. No second confirm.
- FR-L3. Phone TCP-connects to `{lastIpv4}:43298` (desktop default lock port). Start listening / FGS is not required.
- FR-L4. Phone sends `PACKET_ID_LOCK_REQUEST` (`0xB3`). Desktop replies `PACKET_ID_LOCK_RESPONSE` (`0xB4`). Same framing and AES-256-GCM as unlock. Lock proto version `1.0.0`.
- FR-L5. Desktop matches the phone by stored pairing id, decrypts with that `encryptionKey`, checks timestamp (120s) and that the encrypted `pcDeviceId` equals that pairing id.
- FR-L6. On success the desktop calls `LockWorkStation` in the logged-in session and returns success without `passwordKey`.
- FR-L7. Errors use wire codes `TIME_ERROR`, `DATA_ERROR`, `NOT_PAIRED`, `PROTOCOL_ERROR`, `APP_ERROR` (empty `encData`). Connect/timeout failures stay local on the phone.
- FR-L8. Persist `lastIpv4` when the phone sees that PC: pairing beacon IP, UDP unlock `pcbuIP`, or inbound TCP unlock remote address. Do not invent IPs.
- FR-L9. Bluetooth-only pairings with no stored IPv4 cannot lock. Copy says the PC was not seen on this Wi-Fi.
- FR-L10. Logs may include method, truncated ids, and error names. Never `encryptionKey`, `passwordKey`, `encData`, or raw payloads.

## Non-functional requirements

- NFR-L1. Byte-compatible with desktop `CryptUtils` / `BaseConnection` (16-byte IV, PBKDF2 65535, header `0xDB065AC7AFDFA4CC`).
- NFR-L2. Lock listener binds only while JarvisAuth.exe is running in an interactive Windows session (window may be hidden in the tray). It must not use unlock port 43296.
- NFR-L3. Default lock port 43298, configurable on desktop like other ports. Phone uses 43298 unless a later pairing field exists (v1 of this feature: hardcoded default so old pairings work).
- NFR-L4. No JarvisAuth HTTP/cloud. GPL-3. Same Android/desktop stacks as the companion. Overlay lock via Tailscale `100.x` is internet-reach, not this feature’s LAN listener.
- NFR-L5. Windows firewall: installer or first-run must allow inbound TCP 43298 for the desktop binary, or lock will fail on a stock firewall.

## Success metrics

On a real phone and a real Windows PC (not emulator-only):

1. After both builds are installed, an existing pairing locks the session from PC detail without re-pair, once an IPv4 is stored.
2. Tap Lock PC → workstation is locked (Win+L equivalent) within a few seconds.
3. Desktop quit or wrong IP → phone shows a reachability error; session stays as it was.
4. Unlock over UDP/TCP still works after this feature; 43296 still serves login unlock.

## Constraints and dependencies

- Requires the project-wide exception: desktop may add lock packets and a logged-in listener. Unlock protocol stays frozen.
- Phone must have seen an IPv4 at least once after this feature ships (or at pair time going forward).
- Clocks still within 120s.
- User session must be logged on; `LockWorkStation` is a no-op/fail if there is no interactive session.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| DHCP changes the PC IP | Fail with reachability copy; next unlock/pair refreshes `lastIpv4`. No LAN-wide scan. |
| Windows firewall | Document + open 43298 for JarvisAuth.exe in the Windows installer if we already punch pairing/unlock ports. |
| Port clash with login unlock | Dedicated 43298. Never bind lock on 43296. |
| Stolen unlocked phone | Accepted for tap-to-lock. Unlock still needs biometrics. |
| Desktop not running | Fail closed; copy tells the user the PC app must be running. |
| Mis-tap at the desk | Accepted (one tap). No confirm sheet. |

## Acceptance criteria

- AC-L1. Lock PC on a paired Windows host with a stored IP locks that session.
- AC-L2. No re-pair required when upgrading from the current pairing file.
- AC-L3. No `passwordKey` on the lock wire or in logs.
- AC-L4. Unlock UDP/TCP still pass their existing hardware checks.
- AC-L5. Missing IP, timeout, time skew, and unknown phone each show the PRD one-line copy, not a hang.
- AC-L6. Linux/macOS desktop builds are unchanged for this feature (no lock listener).

## Release scope

**This feature**  
Windows desktop lock listener + Android Lock PC on PC detail. LAN TCP only.

**Out**  
Bluetooth lock, Linux lock, beacons, biometrics for lock, sleep/WoL, protocol changes to unlock/pair. Forced shutdown/restart is `agentic/features/power-control/`.
