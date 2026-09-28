# Engineering Rules

These rules apply to `agentic/features/internet-reach/` work in `android/` and desktop `common/` / `desktop/`. Project `agentic/prd.md` / `architecture.md` / `rules.md` still govern pairing and unlock crypto. lock-pc still governs `0xB3`/`0xB4` and port 43298. This feature is an **approved exception** for: optional JSON key `tsIP`, overlay IPv4 `100.64.0.0/10`, desktop unlock TCP server on all methods during a wait, and PC-initiated TCP to the phone’s overlay IP.

## Mandatory conventions

- Kotlin on Android; C++/Qt on desktop. No Java sources except generated.
- Compose / QML have no sockets, crypto, or Tailscale APIs. ViewModel → `UnlockPc` / `LockPc` → transport.
- Do not rename `pcbuIP` / `pcbuPort`. Overlay address field is `tsIP` only.
- `0xB0` payload stays a raw pairing-id string. Do not JSON-wrap it.
- Product copy: JarvisAuth. Never MeisApps / PC Bio Unlock / Tailscale branding in the UI (no logo). “Tailscale” is allowed in the spec-row label and error copy from the design doc.
- Linux/macOS must not grow a lock listener. Unlock overlay changes in `common/` may apply there.
- Pairing QR/discovery stay on the LAN. No Tailscale SDK, no Tailscale HTTP API.

## Coding and naming standards

- Android: `UnlockPc`, `OverlayIpv4`, `lastTailscaleIpv4`, `tsIP` on packet DTOs.
- Desktop: `PairedDevice.tsIP`; `UnlockHandler` owns dual-path cancel. Not QML.
- Classifier: `100.64.0.0/10` is overlay. RFC1918 is LAN. Do not store one in the other’s field.
- Errors: sealed types (`UnlockPcError.NotAsking`, `Unreachable`, existing unlock/lock wire codes).
- ktlint / existing C++ style. No wildcard imports in new Kotlin except existing Compose material3 exceptions.

## Testing requirements

- JVM: ignore-unknown `tsIP`; overlay vs LAN persistence (no clobber); UnlockPc in-flight vs dial; not-asking ⇒ no `passwordKey`; LockPc host order LAN then overlay.
- Desktop: TCP unlock server always on during wait; overlay TCP client when `tsIP` set; first SUCCESS cancels siblings; `0xB0` still raw id.
- Do not mock `javax.crypto` in golden tests.
- Hardware sign-off is real phone + real Windows + Tailscale.com. Emulator does not prove Winlogon or DERP.

## Security requirements

- Never log `passwordKey`, `encryptionKey`, `encKey`, `encData`, or raw payloads.
- `passwordKey` only after BiometricPrompt (or device credential) for that unlock attempt.
- Dual-path: first desktop success must cancel other connections before a second `0xB2` can apply.
- Late success after PC cancel is forbidden (project AC7).
- Lock stays one tap, no biometric, including overlay (lock-pc FR-L2).
- No HTTP client in JarvisAuth for this feature. Overlay TCP to `100.x` is not an HTTP exception.
- No debug skip-biometric that can ship in release.

## Dependency and version constraints

- No new libraries (no OkHttp, no Tailscale Android SDK, no STUN stack).
- minSdk 26, target 35, applicationId `com.jarvisauth.app`.
- Desktop still vcpkg/OpenSSL as today.

## Documentation expectations

- User-facing overlay / Unlock PC behavior lives in `agentic/features/internet-reach/prd.md` (and design/architecture here).
- After this feature’s docs are all approved, project-wide `agentic/prd.md`, `architecture.md`, `rules.md`, `design.md`, and `memory.md` get a short cross-cutting note: Tailscale overlay exception, `tsIP`, Unlock PC, Lock PC is ghost on detail.
- lock-pc design’s “Lock PC is brass primary” is superseded on PC detail by this feature’s dock (Unlock brass, Lock ghost). Update that sentence when project docs are synced.
- Comment why `tsIP` is additive and why `0xB0` is not JSON.

## Definition of done

1. Matches this feature PRD AC-R1–R8 for the slice under test.
2. Pairing/unlock crypto and lock packets still obey project + lock-pc rules.
3. Tests above pass for the slice that shipped.
4. PC detail matches internet-reach design: Unlock PC brass, Lock PC ghost, Tailscale spec row, unlock plate unchanged.
5. Hardware: home LAN with Tailscale quit still unlocks; cellular after one home overlay contact unlocks and locks; Unlock PC at desktop shows not-asking.

## Forbidden practices

- JarvisAuth relay, Headscale-in-app, WAN port-forward of 43296/43298, STUN/ICE.
- Tailscale SDK or API tokens in the repo.
- New unlock packet IDs without amending this architecture.
- Putting overlay IPv4 into `lastIpv4` / `ipAddress` or LAN into `lastTailscaleIpv4` / `tsIP`.
- JSON-wrapping `0xB0`.
- Renaming pairing method to Tailscale in the UI.
- Silent unlock or skipping biometrics on Unlock PC.
- Biometric gate on Lock PC.
- Starting FGS from Unlock PC / Lock PC.
- Talking to meis-apps.com.
- Business logic in composables or QML.
- `runBlocking` on the Android main thread.
- Catch-and-ignore of crypto failures then unlocking or locking.
- Scanning the tailnet or inventing IPs.
