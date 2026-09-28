# Project Memory

Do not record secrets, credentials, access tokens, or sensitive personal data.

## Approved decisions and rationale

- Feature: lock **and** unlock off-LAN via **Tailscale.com** overlay IPs (`100.64.0.0/10`). Not a JarvisAuth relay, not Headscale, not WAN port-forward. Post-v1; not Android Phase 6.
- Official Tailscale apps on PC + phone. No Tailscale SDK or API in JarvisAuth. Packets still go to IPv4 on the adapter (DERP is Tailscale’s).
- Pairing stays on the LAN. No re-QR. Learn/store overlay IPs after one home contact with Tailscale up. Keep LAN and overlay in **separate** fields.
- UDP (and BT) keep their method. Desktop still does the LAN path and **also** TCP-dials the phone `tsIP`. First success cancels the other path.
- During unlock wait, desktop **always** binds TCP unlock server on 43296 so Unlock PC can connect, including TCP/BT pairings.
- Lock over Tailscale stays **one tap**, no biometric (lock-pc FR-L2). Stolen unlocked phone can lock from anywhere. Accepted.
- PC detail **Unlock PC** (brass): resume in-flight plate or dial 43296 LAN then overlay, biometric required. Lock PC becomes ghost. Forced **Shut down | Restart** is `agentic/features/power-control/` (same 43298 socket). If the PC is not waiting: “PC isn’t asking to unlock.”
- Protocol exception: optional JSON `tsIP` only. Do not rename `pcbuIP`/`pcbuPort`. `0xB0` stays a raw pairing-id string.
- No HTTP client in JarvisAuth for this. No meis-apps.com.

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
| 0 Overlay addresses and `tsIP` | complete |
| 1 Desktop wait: server + overlay client | complete |
| 2 Android overlay learn + lock | complete |
| 3 Unlock PC | complete |
| 4 Hardware | in-progress |

## Important context

- Phone `Pairing.lastIpv4` = LAN PC. `lastTailscaleIpv4` = PC `100.x`. Desktop `PairedDevice.ipAddress` = phone LAN/TCP target. `tsIP` = phone `100.x`. Never clobber.
- `0xB0` is a raw `PairedDevice.id` string (SHA256 machineID+phoneUUID+userName), not JSON.
- UDP broadcast `pcbuIP` stays LAN; additive `tsIP` is the PC overlay. Same plaintext-on-LAN split as today.
- Unlock PC is the UDP-style client (`0xB0` then `0xB1`). PC-initiated overlay client is `0xB1` first (TCP method).
- Tailscale Windows service must already be running at the credential UI or remote PC-initiated unlock is dead. JarvisAuth must not start Tailscale.
- In-app Install still does not replace `JarvisAuth.exe`. Copy `pkg/build/desktop/Release/JarvisAuth.exe` over Program Files after desktop builds. Tray hide keeps lock; tray Quit does not.
- 2026-08-18: internet-reach docs approved. Phase 0 overlay/`tsIP` parse-store complete. Phase 1 desktop wait compiled Release. Phase 2 Android overlay learn + lock: unit tests passed. Phase 3 Unlock PC (brass dock, in-flight resume, LAN-then-Tailscale dial 43296, not-asking): ktlint + unit tests + assembleDebug passed. APK: `android/app/build/outputs/apk/debug/app-debug.apk`. Copy `pkg/build/desktop/Release/JarvisAuth.exe` over Program Files. Hardware is Phase 4.

## Active risks

- Dual-path two plates / double `passwordKey` if sibling cancel is late.
- Tailscale down at Winlogon.
- Overlay never learned (user never met on LAN with Tailscale up).
- OEM + Tailscale VPN + FGS bind issues.
- `passwordKey` ciphertext may cross Tailscale DERP.

## Unresolved noncritical questions

- Exact wording if we show the overlay IP in logs at info vs debug only (design already allows spec-row `100.x`).
- Whether desktop Unlock test should try overlay (out of this slice unless it falls out of `UnlockHandler`).

## Changes affecting future work

- Headscale or a JarvisAuth relay would be a new feature, not a flag here.
- Pairing over the internet is still forbidden.
- lock-pc design “Lock PC is brass” is superseded on PC detail by internet-reach (Unlock brass, Lock ghost). Sync project `agentic/design.md` and lock-pc design after this memory file is approved.
- Project “no internet” means no JarvisAuth cloud/HTTP. Overlay TCP to `100.x` is the approved exception.
