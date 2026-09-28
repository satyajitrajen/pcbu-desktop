# Engineering Rules

These rules apply to `agentic/features/lock-pc/` work in `android/` and Windows desktop. Project `agentic/prd.md` / `architecture.md` / `rules.md` still govern unlock and pairing. Where they say the protocol is frozen, this feature is an **approved exception** for lock packets and the logged-in lock listener only.

## Mandatory conventions

- Kotlin on Android; C++/Qt on desktop. No Java sources except generated.
- Compose UI has no sockets, crypto, or `LockWorkStation`. ViewModel → `LockPc` → transport.
- Packet names stay protocol-shaped: `pcDeviceId`, `encData`, `deviceId`. Do not rename for taste.
- Product copy: JarvisAuth. Never MeisApps / PC Bio Unlock.
- Linux/macOS desktop must not grow a lock listener in this feature.
- Unlock packets `0xB0`–`0xB2`, pairing `0x50`–`0x51`, default unlock port 43296, BT UUID: unchanged.

## Coding and naming standards

- Android: `LockPc`, `TcpLockClient`, `PacketLockRequest`, `PACKET_ID_LOCK_REQUEST`.
- Desktop: lock server type name in connection code, not in QML.
- Errors: sealed `LockError` mapped to wire codes or local `Unreachable`.
- ktlint / existing C++ style. No wildcard imports in new Kotlin except existing Compose material3 exceptions.

## Testing requirements

- JVM tests for lock JSON, AES round-trip, proto mismatch, NOT_PAIRED, success body contains nonce and not `passwordKey`.
- Do not mock `javax.crypto` in golden tests.
- Hardware lock sign-off is Windows + real phone. Emulator may prove TCP to a fake server; it does not prove `LockWorkStation`.

## Security requirements

- Never log `passwordKey`, `encryptionKey`, `encData`, nonces in full, or raw payloads.
- Do not send `passwordKey` on 0xB4.
- Do not lock if decrypt or `pcDeviceId` check fails.
- `lastIpv4` is not a secret but is not a public API; do not put it in logcat at info unless debug logging is on (then still not keys).
- FLAG_SECURE remains on MainActivity; no new screenshot of keys.
- No debug “skip crypto” switch.

## Dependency and version constraints

- No new libraries for lock (no OkHttp, no extra Windows packages).
- minSdk 26, target 35, applicationId `com.jarvisauth.app`.
- Desktop still vcpkg/OpenSSL as today.

## Documentation expectations

- User-facing lock behavior stays in `agentic/features/lock-pc/prd.md` (and design/architecture here).
- After this feature’s docs are approved, project-wide `agentic/prd.md`, `architecture.md`, `rules.md`, and `memory.md` get a short cross-cutting note: lock exception, port 43298, packets 0xB3/0xB4.
- Comment why 43298 exists (clash with login 43296).

## Definition of done

1. Matches this feature PRD AC-L1–L6 for the slice.
2. Unlock/pairing still obey project rules.
3. Crypto/framing tests for lock packets pass.
4. PC detail matches design tokens; Lock PC is Brass primary; Remove stays refuse.
5. Manual Windows check: tap locks the session.

## Forbidden practices

- Reusing 0xB1/0xB2 or port 43296 for lock.
- Bluetooth or Linux lock in this slice.
- Biometric gate or confirm sheet (rejected in discovery).
- UDP logged-in beacon.
- Storing lock success as a reason to skip later unlock biometrics.
- Talking to meis-apps.com.
- Business logic in composables or QML.
- `runBlocking` on the Android main thread.
- Catch-and-ignore of crypto failures then locking anyway.
