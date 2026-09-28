# Engineering Rules

These rules apply to `android/` and to any desktop change made for the Android companion. Approved `agentic/prd.md`, `design.md`, and `architecture.md` win over habit. The user’s web frontend standards (TypeScript, React, TanStack Query, Tailwind) do **not** apply to this Kotlin app.

## Mandatory conventions

- Kotlin only in `android/`. No Java source except generated.
- UI is Jetpack Compose. No XML layouts except the application theme/manifest.
- Composables are UI. No sockets, crypto, file IO, or Bluetooth in composables or in `ui/` besides calling a ViewModel.
- All protocol, crypto, and transport code lives under `protocol/` and `transport/` as specified in architecture.
- Packet field names, IDs, header, ports-as-defaults, and Bluetooth UUID are frozen for pairing and unlock. Do not rename `pcbuIP` / `pcbuPort`. Lock packets `0xB3`/`0xB4` and port 43298 are the approved exception in `agentic/features/lock-pc/`. Lock proto `1.1.0` + inner `action` (`shutdown`/`restart`) on that same socket is the approved exception in `agentic/features/power-control/`. Optional JSON `tsIP` and overlay IPv4 are the approved exception in `agentic/features/internet-reach/`. `0xB0` stays a raw pairing-id string.
- Application ID is `com.jarvisauth.app`. Do not suffix `.debug`.
- Product strings in the UI say **JarvisAuth**, never PC Bio Unlock or MeisApps.
- GPL-3 for Android sources. Keep a `LICENSE` copy in `android/` or point at the repo root license.

## Coding and naming standards

- Packages: `com.jarvisauth.app.<layer>`.
- Types: PascalCase. Functions/properties: camelCase. Compose screens: `HomeScreen`, `PairScreen`, `UnlockPlate`.
- ViewModels: `*ViewModel`, one per destination, no God VM.
- Use cases: verb phrases (`PairPc`, `StartListener`, `ConfirmUnlock`).
- Coroutines: `viewModelScope` / `applicationScope`. Never `GlobalScope`. IO on `Dispatchers.IO`.
- Errors: sealed types (`UnlockError.Cancel`, `TimeSkew`, …). Do not throw across the UI boundary.
- Format with the project Kotlin formatter (ktlint or `.editorconfig` 4-space indent). No `*` wildcard imports in new files except Compose `androidx.compose.material3` if the file is purely UI.
- Comments explain why a desktop quirk is copied (e.g. 16-byte GCM IV). Do not comment what the next line does.

## Testing requirements

- Crypto and framing **must** have JVM unit tests. A PR that touches `protocol/` without updating or adding tests is incomplete.
- Golden vectors must assert: 16-byte IV, 16-byte salt, 16-byte tag, BE timestamp prefix, PBKDF2-HMAC-SHA256 65535, AES-256-GCM no padding.
- Domain tests: unknown `deviceId` → `NOT_PAIRED`; biometric cancel → `CANCEL` and no `passwordKey` on the wire; proto mismatch → `PROTOCOL_ERROR`.
- Do not mock `javax.crypto` in golden tests. Call the real implementation.
- Bluetooth hardware tests are manual; do not mark BT done because an emulator test passed.

## Security requirements

- Never log `passwordKey`, `encryptionKey`, QR `encKey`, `encData`, or raw packet payloads. DeviceId may be logged truncated.
- Pairing secrets only in `EncryptedFile` (or equivalent authenticated encryption at rest). No plaintext `SharedPreferences`.
- `android:allowBackup="false"`. Unlock and pair activities use `FLAG_SECURE`.
- Unlock response with `passwordKey` is sent only after a successful BiometricPrompt (or device credential) in that session.
- Fail closed: timeout, cancel, decrypt failure, clock skew → error packet or close, never a success payload.
- No telemetry, crash-upload, ads, or HTTP clients to the public internet. Tailscale overlay TCP to `100.x` is not an HTTP client (`agentic/features/internet-reach/`).
- Release keystore is local and gitignored. Do not commit `.jks`, `.keystore`, or `key.properties` with passwords.

## Dependency and version constraints

- Add a library only if protocol, CameraX, ZXing, biometric, encrypted storage, or Compose cannot do the job.
- Forbidden: Firebase, ML Kit, Retrofit/OkHttp for the protocol, Room, Hilt, WorkManager, Play Billing, Analytics.
- Kotlin, Compose BOM, and Android Gradle Plugin versions are pinned in `android/gradle/libs.versions.toml`. No floating `+` versions.
- minSdk 26, targetSdk 35. Raising minSdk requires a PRD change.

## Documentation expectations

- User-facing behavior changes update `agentic/prd.md` (and design/architecture if UI or structure changes) **before** the code ships.
- Protocol quirks copied from desktop get a one-line comment pointing at the desktop file (e.g. `CryptUtils.cpp`).
- Do not add README marketing. `android/README.md` is build/install only.

## Definition of done

A change is done when:

1. It matches PRD acceptance for the slice it claims (pair, UDP, TCP, or BT).
2. Rules above hold (especially no secrets in logs, no protocol drift).
3. Crypto/framing tests pass if `protocol/` was touched.
4. UI matches `design.md` tokens (Night/Brass/Sora/Plex Mono, armed rail, unlock plate). Do not “improve” the palette in a random screen.
5. Manual check on a device for any transport slice marked complete.

v1 as a whole is done only when PRD success metrics 1–6 are true on hardware.

## Forbidden practices

- Inventing packet fields, wrapping the protocol in JSON-over-HTTP, or talking to `meis-apps.com`.
- Silent auto-unlock, Wear OS, cloud pairing, or Play Store packaging in v1.
- Changing desktop `pcbuIP` / header / UUID “for cleanliness” to match the phone.
- Business logic in composables.
- `runBlocking` on the main thread.
- Storing keys in `BuildConfig`, resources, or source.
- Copying PC Bio Unlock assets, package name, or UI.
- Skipping BiometricPrompt with a debug “always confirm” switch that can ship in release. A debug-only flag must be `if (BuildConfig.DEBUG)` and default off.
- Partial pairing records (abandon must delete in-flight state).
- Catch-and-ignore of crypto failures.
