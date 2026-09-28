# Product Requirements Document

## Product vision

JarvisAuth can force-shutdown or restart a logged-in Windows PC from the paired phone with one tap, using the same lock channel the user already has. The phone stays a key holder. It never sends `passwordKey` for power.

## Problem statement

Lock PC leaves the session at Win+L. Walking away sometimes needs the machine **off** or **rebooting**, not locked. The phone already reaches that PC on LAN and Tailscale. Lock-pc explicitly forbade remote shutdown; this feature amends that.

## Target users and needs

**Primary user:** Satyajit, JarvisAuth on his Windows PCs and the first-party Android app.

Need: open a paired Windows PC and shut it down or restart it without touching the PC.

Not a target: Linux, macOS, sleep/hibernate, Wake-on-LAN, Play Store users, a power-management dashboard.

## Goals

1. Forced Windows shutdown and restart from PC detail, one tap.
2. Reuse existing pairings, `encryptionKey`, lock port **43298**, and packets **`0xB3`/`0xB4`**. No re-pair.
3. Fail closed if unreachable, desktop not running, proto/crypto/time/device checks fail, or the old exe is still installed.
4. Keep unlock 3.0.0 / pairing 4.0.0 and lock-on-`1.0.0` working.

## Non-goals

- Linux, macOS, Bluetooth-only power, sleep, hibernate, WoL.
- Biometric or a second confirm for Shutdown/Restart.
- New packet IDs or a new port.
- Silent power without a user tap.
- Changing unlock/pair field names or `0xB0`–`0xB2`.
- Starting Tailscale, or powering on a PC that is already off.

## Use cases and user stories

**Shutdown from PC detail**  
As a user, I tap Shut down. Apps are killed. The PC powers off. I see a short success or a one-line failure.

**Restart from PC detail**  
As a user, I tap Restart. Apps are killed. The PC reboots.

**Existing pairing**  
As a user who already paired, I install the new phone and desktop builds and use power without scanning a QR.

**Old desktop exe**  
As a user who forgot to copy `JarvisAuth.exe` over Program Files, Shutdown/Restart fails with a version error. The session is not locked and not powered off.

**Unreachable / Quit**  
As a user whose process is gone (tray Quit) or whose IP is stale, I see the same reachability copy as lock. The PC is unchanged.

**Stolen unlocked phone**  
As an attacker with the unlocked phone and Tailscale, I can force-shutdown or restart from anywhere (accepted). I still cannot obtain `passwordKey`.

## Functional requirements

- FR-W1. PC detail shows **Shut down** and **Restart** when a LAN or Tailscale IPv4 is stored. Missing both → same missing-address copy as lock, not a silent no-op.
- FR-W2. One tap sends the command. No biometric. No second confirm.
- FR-W3. Phone uses the lock client: LAN `lastIpv4` then `lastTailscaleIpv4`, port **43298**. FGS is not required or started.
- FR-W4. Packets remain `0xB3` / `0xB4`. Shutdown and Restart send lock proto **`1.1.0`**. Encrypted inner JSON includes `action`: `shutdown` or `restart`.
- FR-W5. Lock PC still sends proto **`1.0.0`** with no `action`. Old desktop still locks.
- FR-W6. New desktop accepts `1.0.0` as lock. Accepts `1.1.0` only with `action` in `{shutdown, restart, lock}`. Unknown action or `1.1.0` without action → `DATA_ERROR`. Other proto → `PROTOCOL_ERROR`.
- FR-W7. On success the desktop writes `0xB4` success first, then forces the action (`ExitWindowsEx` with `EWX_FORCE`, plus `SE_SHUTDOWN_NAME`). No `passwordKey`.
- FR-W8. Forced means apps are killed. Unsaved work is discarded. No save prompts.
- FR-W9. Same pairing-id / decrypt / 120s skew / inner `pcDeviceId` checks as lock. Same wire error codes. Connect/timeout stay local on the phone.
- FR-W10. Linux/macOS: no listener change; power taps get `APP_ERROR` if they ever hit a non-Windows build.
- FR-W11. Logs may include `action=shutdown|restart`, truncated ids, path `lan|tailscale`, error names. Never keys or `encData`.

## Non-functional requirements

- NFR-W1. Same crypto/framing as lock (16-byte IV, PBKDF2 65535, header `0xDB065AC7AFDFA4CC`).
- NFR-W2. Same listener as lock: JarvisAuth.exe in the interactive session (tray hide counts). Not port 43296.
- NFR-W3. No new libraries. GPL-3. No HTTP/Tailscale SDK. Overlay reach is internet-reach’s host order on this socket.
- NFR-W4. In-app Install still does not replace `JarvisAuth.exe`. Power needs the new GUI copied over Program Files.

## Success metrics

On a real phone and a real Windows PC (not emulator-only):

1. Existing pairing, new APK + new exe: Shut down powers off; Restart reboots; apps with unsaved work are killed.
2. Lock PC still locks on proto `1.0.0`.
3. New APK + **old** exe: Shutdown/Restart fail closed (version/reachability); session stays as it was.
4. Tray Quit or no address → error; PC unchanged.
5. Unlock on 43296 still works after this feature.

## Constraints and dependencies

- Amends lock-pc non-goal “remote shutdown” and internet-reach PC detail dock.
- Clocks within 120s. User session logged on. `SE_SHUTDOWN_NAME` must be enableable.
- Tailscale off-LAN works only if overlay IPs were already learned (internet-reach).
- After shutdown the listener is dead until the next interactive session.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Stolen unlocked phone + Tailscale | Accepted. Same as lock-pc FR-L2 over overlay. Unlock still biometric. |
| Unsaved work lost | Forced by decision. Copy does not pretend it is graceful. |
| Old exe treats extra JSON as lock | Proto **1.1.0** exact-match fail on old exe (`PROTOCOL_ERROR`). Phone keeps lock on `1.0.0`. |
| ACK lost because TCP dies at shutdown | Write `0xB4` **before** `ExitWindowsEx`. Phone may still see Unreachable after success; treat that as possible, not as “didn’t send.” |
| Privilege missing | `APP_ERROR`; do not claim success. |
| Mis-tap | Accepted. One tap, no confirm. Buttons are quieter than Lock. |

## Acceptance criteria

- AC-W1. Shut down / Restart on a paired Windows host with a stored IP perform that forced action.
- AC-W2. No re-pair. No `passwordKey` on the wire or in logs.
- AC-W3. Lock PC and unlock UDP/TCP still pass their existing checks. 43296 unlock, 43298 lock/power.
- AC-W4. Old desktop + new phone power tap does not lock and does not power off.
- AC-W5. Missing IP, timeout, proto mismatch, and unknown phone show one-line copy, not a hang.
- AC-W6. Linux/macOS desktop builds gain no power listener.

## Release scope

**This feature**  
Windows forced shutdown/restart on the lock socket; Android side-by-side Shut down / Restart row; proto `1.1.0` + inner `action`.

**Out**  
Sleep, hibernate, WoL, new ports/packet IDs, confirm/biometric, Linux/macOS.
