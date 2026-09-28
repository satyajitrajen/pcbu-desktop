# Engineering Rules

These rules apply to `agentic/features/power-control/` work in `android/` and Windows desktop. Project docs still govern pairing/unlock. lock-pc still governs `0xB3`/`0xB4` and port 43298. internet-reach still governs overlay host order and the Unlock/Lock dock roles. This feature is an **approved exception** for lock proto **1.1.0**, inner `action`, and `ExitWindowsEx` force.

## Mandatory conventions

- Kotlin on Android; C++/Qt on desktop. No Java sources except generated.
- Compose / QML have no sockets, crypto, or `ExitWindowsEx`. ViewModel → lock domain → transport.
- Do not rename `pcbuIP` / `pcbuPort`. Do not JSON-wrap `0xB0`.
- Product copy: JarvisAuth. Buttons: **Shut down** and **Restart**. Never MeisApps.
- Linux/macOS must not grow a lock/power listener.
- Lock PC wire stays proto `1.0.0` without `action`.

## Coding and naming standards

- Reuse `LockPc` / `LockHandshake` / `TcpLockClient` / `LockServer`. Add `action` and proto `1.1.0` for power; do not add `ShutdownClient` or port 43299.
- Classifier and host order stay internet-reach (`lockHops`).
- Errors: existing `LockError` wire codes. No new wire codes.
- ktlint / existing C++ style. No wildcard imports in new Kotlin except existing Compose material3 exceptions.

## Testing requirements

- JVM: `1.0.0` lock omits `action` and `passwordKey`; `1.1.0` encodes `action`; old-style parse still works.
- Desktop: `1.0.0` still locks; `1.1.0` without action is `DATA_ERROR`; do not call `ExitWindowsEx` on crypto fail.
- Hardware: real Windows + real phone. Emulator does not prove force shutdown.
- Do not mock `javax.crypto` in golden tests.

## Security requirements

- Never log `passwordKey`, `encryptionKey`, `encKey`, `encData`, or raw payloads.
- Never send `passwordKey` on `0xB4`.
- Never power or lock if decrypt or `pcDeviceId` fails.
- No biometric skip switch. No confirm sheet (rejected).
- No HTTP client. Overlay TCP to `100.x` is internet-reach, not this feature’s to invent.

## Dependency and version constraints

- No new libraries.
- minSdk 26, target 35, `com.jarvisauth.app`.
- Desktop still vcpkg/OpenSSL.

## Documentation expectations

- User-facing power behavior lives in this folder’s `prd.md`.
- After this feature’s docs are all approved: lock-pc PRD non-goal “remote shutdown” is struck; internet-reach / project design dock gains the power row; project `agentic/prd.md` / `architecture.md` / `rules.md` / `memory.md` get a short 1.1.0 / `action` note.

## Definition of done

1. Matches this feature PRD AC-W1–W6 for the slice under test.
2. Lock and unlock still obey lock-pc + project rules.
3. Tests above pass for the slice that shipped.
4. PC detail matches this design: Unlock brass, Lock ghost full-width, Shut down | Restart row, Remove refuse.
5. Hardware: force shutdown and restart; lock still works; old exe fail-closes power.

## Forbidden practices

- New unlock/lock packet IDs or a new port for power.
- Putting `action` on outer `0xB3` JSON.
- Sending power as proto `1.0.0` (old exe would lock).
- Making new desktop accept **only** `1.1.0` (breaks Lock PC `1.0.0`).
- `ExitWindowsEx` before writing `0xB4` success.
- Graceful shutdown APIs that wait on save dialogs.
- Biometric or confirm on Shut down / Restart.
- Sleep/hibernate/WoL in this slice.
- Starting FGS from power taps.
- Talking to meis-apps.com.
- Business logic in composables or QML.
- Catch-and-ignore crypto then powering off.
