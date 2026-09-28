# Product Requirements Document

## Product vision

JarvisAuth keeps working when the phone is not on the PC’s Wi-Fi: lock and unlock over Tailscale, using existing pairings. The phone stays a key holder. JarvisAuth does not become a VPN, a cloud, or a Tailscale client.

If the login notification is missed, the user can open the paired PC in the app and tap **Unlock PC**.

## Problem statement

Unlock and lock assume LAN addresses. UDP unlock is a LAN broadcast. TCP unlock stores an empty phone IP at pair time. Lock uses the PC’s Wi-Fi `lastIpv4`. Off-LAN those paths die even when Tailscale already connects the two devices.

Separately, unlock is PC-initiated via a notification / full-screen plate. OEM suppression or a dismissed notification leaves the PC waiting with no in-app way to finish the handshake.

## Target users and needs

**Primary user:** Satyajit, JarvisAuth on his Windows PCs and the first-party Android app, Tailscale.com on both.

Needs:

- Unlock at Windows login/lock/UAC when the phone is on cellular or another network, Tailscale up.
- Lock that session from PC detail the same way (LAN or Tailscale).
- Keep pairings already on the phone. No new QR.
- Recover a missed unlock notification from the app.

Not a target: Play Store users, Headscale operators, people without Tailscale, pairing over the internet, Linux/macOS lock.

## Goals

1. Lock and unlock off-LAN using Tailscale.com overlay IPs (`100.64.0.0/10`) without a JarvisAuth relay or public port-forward.
2. Reuse existing pairings and `encryptionKey`. No re-pair.
3. UDP (and Bluetooth) pairings keep their method. At login the desktop still does the LAN path and also TCP-dials the phone’s Tailscale IP.
4. Learn and persist Tailscale addresses after one home contact with Tailscale running. Keep LAN IPs for home.
5. PC detail **Unlock PC** completes a missed prompt (in-flight session) or starts a phone-initiated unlock handshake while the PC is waiting. Biometrics required.
6. Fail closed: no Tailscale IP, Tailscale down, PC not waiting, timeout, cancel, crypto/time mismatch → no `passwordKey`, no unlock, no lock on decrypt failure.
7. Keep pairing 4.0.0 / unlock 3.0.0 crypto, header, `pcbuIP`/`pcbuPort` names, ports 43296 / 43298. Optional additive address fields only.

## Non-goals

- Headscale, a JarvisAuth VPS/broker, router port-forward, STUN, meis-apps.com, `CLOUD_TCP` / `CLOUD_BT`.
- Tailscale SDK inside JarvisAuth. User installs the official Tailscale apps.
- Pairing, QR, or discovery over the internet.
- Silent auto-unlock. Biometric skip.
- Biometric (or extra confirm) for lock. FR-L2 stays: one tap on LAN and over Tailscale.
- Unlocking a session that is already at the desktop with nothing listening.
- Changing UDP broadcast into an internet beacon.
- Exposing 43296/43298 on the public WAN.
- Linux/macOS lock listener (still lock-pc out of scope).
- Play Store, Wear OS, iOS.

## Use cases and user stories

**Home, Tailscale unused**  
As a user on the same Wi-Fi, unlock and lock still use LAN addresses first. Behavior matches today aside from Unlock PC.

**Away, Tailscale up**  
As a user on cellular, I confirm login on the phone. The desktop reaches the phone at its stored `100.x` (and/or I tap Unlock PC). The PC unlocks.

**Lock away**  
As a user off-LAN, I tap Lock PC. The phone dials the PC’s stored Tailscale IP on 43298. The session locks. One tap. No biometric.

**First Tailscale contact**  
As a user at home with Tailscale running on PC and phone, one pairing/unlock/lock contact stores `100.x` on both sides. After that, away works. If that contact never happens, copy says Tailscale was not seen yet.

**Missed notification**  
As a user whose unlock notification was suppressed or dismissed while the PC is still waiting, I open PC detail, tap Unlock PC, authenticate, and the PC unlocks.

**PC not waiting**  
As a user who taps Unlock PC while the session is already logged in (or the login wait ended), I see that the PC is not asking to unlock. Nothing is sent that could unlock later.

**Existing pairing**  
As a user who already paired over UDP, I install the new builds, meet once on home Wi-Fi with Tailscale up, and do not scan a QR again.

**Stolen unlocked phone**  
As an attacker with the unlocked phone and Tailscale, I can lock from anywhere (accepted). I still need biometrics (or device PIN) to unlock.

**Tailscale down at login**  
As a user whose Tailscale Windows service is not running at the credential UI, remote unlock fails. The password field on the PC still works.

## Functional requirements

### Overlay and addresses

- FR-R1. JarvisAuth does not install, start, or authenticate Tailscale. Official Tailscale.com apps only.
- FR-R2. Treat IPv4 in `100.64.0.0/10` as a Tailscale overlay address. Persist it separately from LAN `lastIpv4` / phone LAN IP. Do not overwrite a LAN address with `100.x` or the reverse.
- FR-R3. Prefer LAN, then Tailscale. Home must not wait on a Tailscale timeout before trying LAN.
- FR-R4. Learn addresses only from local interfaces and from packets we already accept. Do not scan the tailnet or query Tailscale’s API.
- FR-R5. First overlay exchange requires both devices at home (or otherwise already reachable on LAN) with Tailscale up. No re-QR. If `100.x` is missing, Unlock/Lock over Tailscale is disabled with explicit copy, not a silent no-op.
- FR-R6. Tailscale IP changes are re-learned on the next successful home contact.

### Unlock off-LAN (PC-initiated)

- FR-R7. UDP pairings: desktop still UDP-broadcasts on the LAN and still opens the unlock TCP server. It **also** starts a TCP unlock client to the phone’s stored Tailscale IP and `tcpPort` when that IP exists.
- FR-R8. Bluetooth pairings: same TCP-to-Tailscale-IP fallback in addition to RFCOMM. BT-only with no overlay IP cannot unlock off-LAN.
- FR-R9. TCP pairings: desktop connects to the stored phone Tailscale IP when LAN connect fails or LAN IP is empty, using the existing `0xB1`-first client.
- FR-R10. Dual path (LAN + Tailscale): first successful unlock wins. The other path must not send a second success `passwordKey`. Cancel/timeout on the winner fails closed for that attempt. Ctrl+Alt on the PC still cancels; a late phone success after the PC gave up must not unlock.
- FR-R11. Phone unlock listeners bind `0.0.0.0` (all interfaces), including the Tailscale adapter.

### Phone-initiated Unlock PC

- FR-R12. PC detail shows **Unlock PC** when a LAN or Tailscale IPv4 is stored. Missing both → copy that the PC has not been seen on LAN or Tailscale.
- FR-R13. Unlock PC requires BiometricPrompt (device PIN/pattern/password fallback allowed). No silent approve.
- FR-R14. If an in-flight unlock session exists for that pairing, Unlock PC opens the unlock plate and completes that socket. It does not start a second handshake.
- FR-R15. If none exists, the phone TCP-connects to `{lan or Tailscale IP}:{unlockServerPort}` (default 43296), LAN first then Tailscale, sends `0xB0` deviceId, waits for `0xB1`, then the same biometric + `0xB2` path as UDP unlock.
- FR-R16. Desktop must accept that inbound unlock connection for **UDP, TCP, and Bluetooth** pairings while an unlock wait is active (login/lock/UAC). TCP/BT methods must bind the unlock TCP server during that wait, not only UDP.
- FR-R17. If connect fails or no `0xB1` arrives: copy “PC isn’t asking to unlock.” No `passwordKey`. The logged-in desktop is unchanged.
- FR-R18. Start listening / FGS is not required for Unlock PC (same as Lock PC). In-flight resume may still use the FGS socket if it is alive.

### Lock off-LAN

- FR-R19. Lock PC stays one tap, no biometric, on LAN and over Tailscale (lock-pc FR-L2 unchanged).
- FR-R20. Phone dials LAN `lastIpv4` first, then the PC Tailscale IP, port 43298. Same `0xB3`/`0xB4` lock proto `1.0.0`. No `passwordKey`.
- FR-R21. JarvisAuth.exe must be running (tray hide counts). Tray Quit still disables lock.

### Pairing

- FR-R22. Pairing QR, UDP discovery, and pairing TCP stay on the LAN. Do not put Tailscale IPs in the QR on purpose; overlay IPs are learned after pair.
- FR-R23. Pair init may include the phone’s current Tailscale IPv4 in `ipAddress` when one exists so new TCP pairings are not stuck with `""`. LAN pairings that already exist do not require this to be the only learning path.

### Protocol exception

- FR-R24. Unlock 3.0.0 / pairing 4.0.0 field **names** and crypto stay frozen. Allowed additive exception: optional Tailscale IPv4 on existing JSON that old peers ignore (UDP unlock broadcast on the PC; phone advertisement on a later LAN unlock/lock/pair init). No new unlock packet IDs unless those optional fields cannot carry the address. Do not rename `pcbuIP` / `pcbuPort`.

### Logging and security

- FR-R25. Logs may include method, path (LAN / Tailscale), truncated ids, error names. Never `encryptionKey`, `passwordKey`, `encKey`, `encData`, or raw payloads.
- FR-R26. `passwordKey` is sent only after a successful biometric (or device credential) for that unlock attempt.

## Non-functional requirements

- NFR-R1. Same crypto/framing as desktop `CryptUtils` / `BaseConnection` (16-byte IV, PBKDF2 65535, header `0xDB065AC7AFDFA4CC`).
- NFR-R2. JarvisAuth still has no HTTP client to the public internet, no Tailscale API, no meis-apps.com. Overlay packets are TCP to `100.x` (WireGuard/DERP is Tailscale’s).
- NFR-R3. Unlock wait on Windows requires Tailscale’s Windows service running **before** sign-in. Document it; do not try to start Tailscale from JarvisAuth.
- NFR-R4. GPL-3. Same Android/desktop stacks. No Firebase, OkHttp-for-protocol, Hilt, Room.
- NFR-R5. Dual-path unlock must not double-apply the password. Fail closed on races.
- NFR-R6. Lock port remains 43298. Unlock port remains 43296. Do not share them.

## Success metrics

On a real phone, real Windows PC, Tailscale.com on both, not emulator-only:

1. Existing UDP pairing, no re-QR: after one home contact with Tailscale up, cellular unlock succeeds (PC-initiated and/or Unlock PC).
2. Same pairing: Lock PC from cellular locks the session.
3. On home Wi-Fi, unlock still succeeds if Tailscale is quit (LAN first).
4. Missed notification while the PC is still waiting: Unlock PC + biometric unlocks.
5. Unlock PC while the PC is at the desktop (not waiting) shows the not-asking copy; session unchanged.
6. Cancel / timeout / bad crypto still does not unlock. `passwordKey` never in logs.
7. 43296 still serves login unlock; 43298 still serves lock.

## Constraints and dependencies

- User must install and log into Tailscale.com on PC and phone, same tailnet.
- Cross-cutting exceptions (project docs after this feature’s docs are approved): overlay IPs in-scope; lock-pc may use Tailscale IP; optional additive JSON for `100.x`; desktop unlock TCP server binds for TCP/BT waits too.
- lock-pc LAN TCP remains the home path. This feature extends reach, it does not replace 43298.
- Clocks still within 120s.
- Phone-initiated unlock cannot create a Windows login wait. The credential UI / PAM must already be waiting.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Dual LAN+Tailscale both succeed | One winner; loser must not send `passwordKey`. |
| Tailscale down at Winlogon | Fail closed; password fallback on the PC. Document the service. |
| `100.x` never learned | Explicit copy; one home contact required. No tailnet scan. |
| DERP path | Ciphertext (including `passwordKey`) may cross Tailscale relays. Accepted. No extra JarvisAuth encryption layer. |
| Stolen unlocked phone | Lock from anywhere accepted. Unlock still biometric. |
| OEM + Tailscale VPN + FGS | Bind `0.0.0.0`; hardware-check cellular unlock. |
| TCP pairing never listened on 43296 | Bind unlock server for all methods during wait so Unlock PC can connect. |
| Late `0xB2` after PC cancel | Existing AC7: phone must not complete a late success. |

## Acceptance criteria

- AC-R1. Existing pairing unlocks off-LAN over Tailscale without re-QR after overlay IPs are stored.
- AC-R2. Existing pairing locks off-LAN over Tailscale; one tap; no `passwordKey` on lock.
- AC-R3. Home LAN unlock/lock still work with Tailscale disconnected.
- AC-R4. Unlock PC resumes in-flight or dials 43296; biometric required; not-waiting → not-asking copy.
- AC-R5. UDP method string on the phone does not change. Desktop still broadcasts on LAN.
- AC-R6. No Tailscale/HTTP API in JarvisAuth. No meis-apps.com.
- AC-R7. Optional overlay fields do not break an old peer that ignores unknown JSON keys.
- AC-R8. Linux/macOS: no new lock listener. Unlock over Tailscale may work there if that desktop already unlocks.

## Release scope

**This feature**  
Tailscale overlay reach for lock + unlock; address learning without re-pair; UDP/BT TCP fallback; unlock TCP server on all methods during wait; PC detail Unlock PC.

**Out**  
Headscale, JarvisAuth relay, WAN port-forward, pairing over internet, Tailscale SDK, lock biometrics, silent unlock, Play Store.
