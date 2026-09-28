# Product Requirements Document

## Product vision

JarvisAuth is a personal PC unlock system: confirm login on an Android phone with fingerprint or face, and the paired Windows/Linux/macOS desktop authenticates with the real account password. The desktop app already exists in this repository. This PRD defines the Android companion that makes it usable without the MeisApps Play Store client.

The phone is a key holder, not a password store. It never receives the PC password. It holds the wrapping key, proves possession with biometrics, and returns that key over an encrypted local channel.

## Problem statement

The JarvisAuth desktop can pair and unlock only with a phone that speaks its protocol. The only working client today is PC Bio Unlock on Google Play, which is a third-party app, branded to MeisApps, and not under this project’s control.

Without a first-party Android app, JarvisAuth on the PC is a shell: pairing QR codes have nothing trusted to scan them, and login prompts have nothing to answer them.

## Target users and needs

**Primary user:** Satyajit, using JarvisAuth on his own PCs and Android phone.

Needs:

- Pair a phone to a PC by scanning the desktop QR (or typing the code) on the same LAN.
- Unlock Windows login/lock/UAC and Linux sudo/polkit/login by confirming biometrics on the phone.
- Use UDP at home, TCP when a static phone IP is available, Bluetooth when there is no Wi-Fi.
- Pair more than one PC to the same phone.
- Cancel or fall back to the PC password if the phone is unreachable.

**Not a target:** public Play Store customers, Wear OS, enterprise MDM, iOS.

## Goals

1. Ship a sideloadable APK (`com.jarvisauth.app`) that pairs with the current JarvisAuth desktop.
2. Complete an unlock over UDP, TCP, and Bluetooth using the desktop’s existing protocol versions (pairing 4.0.0, unlock 3.0.0).
3. Keep the PC password off the phone and off the network.
4. Make automatic unlock actually fire while the phone is locked, via a foreground service the user can see and stop.
5. Fail closed: timeout, cancel, clock skew, or protocol mismatch must not unlock the PC.

## Non-goals

- Wear OS, cloud pairing, silent auto-unlock without biometrics.
- A MeisApps-compatible Play Store listing or their updater.
- Changing desktop packet fields, ports as defaults, or the Bluetooth UUID. **Exceptions:** `agentic/features/internet-reach/` may add optional JSON `tsIP` (Tailscale overlay IPv4). `agentic/features/power-control/` may add lock proto `1.1.0` and encrypted inner `action` on `0xB3`. Do not rename `pcbuIP` / `pcbuPort`.
- iOS, tablets-as-primary-UX, or a redesigned desktop.
- Replacing Windows/PAM authentication.
- Copying PC Bio Unlock UI, icons, or source.

## Use cases and user stories

**Pair over QR (happy path)**  
As a user, I open JarvisAuth on the phone, scan the desktop QR, confirm, and see the PC listed as paired, so later unlocks work without typing a password on the PC.

**Pair with typed code**  
As a user, if the camera fails, I enter the pairing code from the desktop and complete the same pairing.

**Unlock over UDP**  
As a user who chose Automatic / UDP on the PC, when the PC login appears, my phone shows an unlock prompt; I authenticate with biometrics; the PC unlocks.

**Unlock over TCP**  
As a user who chose Automatic / TCP, the PC connects to my phone; I confirm biometrics; the PC unlocks.

**Unlock over Bluetooth**  
As a user with no usable LAN, the PC connects over RFCOMM; I confirm biometrics; the PC unlocks.

**Manual UDP**  
As a user who chose Manual, I open the app and start listening; the PC’s unlock request is handled the same as automatic UDP.

**Multiple PCs**  
As a user, I pair a laptop and a desktop; unlock requests show which PC and which program (login, sudo, UAC) is asking.

**Cancel / miss**  
As a user, I dismiss the biometric prompt or ignore it; the PC stays locked and I can type the password there (Ctrl+Alt on the PC still cancels).

**Unpair**  
As a user, I remove a PC from the phone; that device can no longer obtain `passwordKey` from this phone.

## Functional requirements

### Pairing

- FR-P1. Scan the desktop QR and parse `{serverId, port, method, encKey}` (current) or `{ip, port, method, encKey}` (legacy).
- FR-P2. Accept a typed pairing payload equivalent to the QR JSON.
- FR-P3. Discover the PC via UDP beacon when `serverId` is present; connect to pairing TCP on port from QR / beacon (desktop default 43295).
- FR-P4. Send `PACKET_ID_PAIR_INIT` (0x50) with protocol version `4.0.0`, a stable device UUID, device name, current IPv4, `tcpPort`, `udpPort`, `udpManualPort`, empty `cloudToken`.
- FR-P5. Receive `PACKET_ID_PAIR_RESPONSE` (0x51), persist `deviceId`, `encryptionKey` (QR `encKey`), `passwordKey`, `pairingMethod`, `unlockServerPort`, `userName`, PC name/OS, MAC list.
- FR-P6. Show a clear error on protocol mismatch, decrypt/timestamp failure, or desktop `errMsg`.
- FR-P7. Support pairing methods the desktop already offers: UDP, TCP, Bluetooth, MANUAL_UDP. Do not implement CLOUD_TCP/CLOUD_BT.

### Unlock

- FR-U1. For UDP / MANUAL_UDP: listen on the advertised UDP port; on `PacketUDPBroadcast` matching a stored `deviceId`, connect to `pcbuIP:pcbuPort` and run the unlock handshake.
- FR-U2. For TCP: listen on the advertised TCP port; accept the PC connection and run the handshake.
- FR-U3. For Bluetooth: listen on RFCOMM UUID `62182bf7-97c8-45f9-aa2c-53c5f2008bdf`; accept the PC connection and run the handshake.
- FR-U4. Handshake: send `PACKET_ID_DEVICE_ID` (0xB0) when required; receive `PACKET_ID_UNLOCK_REQUEST` (0xB1); decrypt `encData` with `encryptionKey`; prompt biometrics; send `PACKET_ID_UNLOCK_RESPONSE` (0xB2) with encrypted `{unlockToken, passwordKey}` or an error (`CANCEL`, `TIME_ERROR`, `DATA_ERROR`, `NOT_PAIRED`, `PROTOCOL_ERROR`, `APP_ERROR`).
- FR-U5. Echo the request’s `unlockToken` unchanged. Do not invent a new token.
- FR-U6. Show PC name, user, and program (`Windows-Login`, sudo, etc.) on the prompt.
- FR-U7. Automatic methods run from a user-visible foreground service the user can stop. Manual UDP runs while the user has started listening in the app.

### App UX

- FR-X1. Home screen lists paired PCs, method, and listener status.
- FR-X2. User can add a PC (QR / code), remove a PC, and open a short explanation of battery-optimization requirements.
- FR-X3. BiometricPrompt is required to approve an unlock. Device PIN/pattern/password is allowed as fallback. There is no “always allow” / silent approve in v1.
- FR-X4. Debug logging can be enabled in-app and copied; it must not print `passwordKey`, `encKey`, or raw packet payloads.

## Non-functional requirements

- NFR-1. Packet crypto and framing must be byte-compatible with `common/src/utils/CryptUtils.cpp` and `common/src/connection/BaseConnection.cpp` (AES-256-GCM, PBKDF2-HMAC-SHA256 65535 iterations, 16-byte IV, 16-byte salt, 16-byte tag, timestamp prefix, header `0xDB065AC7AFDFA4CC`).
- NFR-2. Reject packets whose timestamp differs by more than 120 seconds.
- NFR-3. Store secrets with Android encrypted storage (not plaintext SharedPreferences).
- NFR-4. minSdk 26, targetSdk 35, applicationId `com.jarvisauth.app`.
- NFR-5. Foreground service types declared for connected-device / data-sync as required by the target SDK; Bluetooth and location/nearby permissions requested only when needed.
- NFR-6. No telemetry, no accounts, no network calls except LAN/Bluetooth to paired PCs.
- NFR-7. GPL-3, same as the desktop.
- NFR-8. Kotlin + Jetpack Compose only for UI. Protocol/crypto/IO live outside composables.

## Success metrics

v1 is successful if all of the following are true on a real phone and a real JarvisAuth desktop (not only an emulator):

1. QR pairing completes and the PC shows the device as paired.
2. A Windows or Linux unlock via UDP succeeds after biometric confirm.
3. The same for TCP and for Bluetooth (Bluetooth may be retested on hardware that actually exposes RFCOMM).
4. Canceling the biometric prompt leaves the PC locked.
5. A second PC can be paired and unlocked independently.
6. After force-stopping the app, automatic unlock does not proceed until the user starts the listener again (fail closed).

No analytics. These checks are manual acceptance, plus unit tests for crypto/framing.

## Constraints and dependencies

- Desktop protocol is frozen for pairing 4.0.0 and unlock 3.0.0. Default ports: pairing TCP 43295, unlock 43296, UDP discovery 43297. **Exception:** lock-from-phone (`agentic/features/lock-pc/`) adds packets `0xB3`/`0xB4` and default TCP port 43298 on Windows only. **Exception:** internet-reach (`agentic/features/internet-reach/`) uses Tailscale.com overlay IPs (`100.64.0.0/10`) and optional `tsIP`; JarvisAuth still has no HTTP/cloud API. Do not change unlock/pair field names or 43296.
- Phone and PC clocks must agree within two minutes.
- Pairing QR carries `encKey` in plaintext; physical access during pairing is in-scope as a threat.
- OEM battery savers can silently kill listeners; the app can warn, it cannot override OEM policy.
- Companion desktop must already be built/installed; this app does not bundle the PC installer.
- Android 12+ Bluetooth connect/scan permissions and FGS type enforcement apply.
- The official Play Store app is not a dependency and must not be required.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Crypto/framing mismatch with desktop | Port the desktop algorithms with golden tests against known vectors from `CryptUtils`; do not “simplify” GCM or skip the timestamp prefix. |
| OEM kills the listener | Foreground service + explicit battery-optimization guidance; fail closed rather than a hidden background hack. |
| Bluetooth RFCOMM flaky on many phones | Ship BT as a first-class method but treat UDP as the reliability baseline; surface BT errors instead of retrying forever. |
| Clock skew | Surface `TIME_ERROR` in the UI with a clock-sync explanation. |
| Stolen phone | Biometric gate on every unlock; secrets in encrypted storage; still not a true 2FA if the attacker has the phone unlocked. |
| QR photographed during pairing | Document it; pairing screen instruction to not leave the QR up. Cannot be fixed in v1 without changing desktop protocol. |

## Acceptance criteria

- AC1. Installing the APK does not require the Play Store PC Bio Unlock app.
- AC2. Pairing with a current JarvisAuth desktop QR succeeds on the same LAN.
- AC3. UDP, TCP, and Bluetooth unlock each succeed once against that desktop with biometrics.
- AC4. Wrong protocol version shows a mismatch error, not a hang.
- AC5. `passwordKey` / `encKey` never appear in logcat at info level or in the in-app log viewer.
- AC6. Removing a pairing on the phone makes subsequent unlock requests from that PC return `NOT_PAIRED` (or equivalent failure), not a success.
- AC7. Left Ctrl+Left Alt on the PC still cancels; the phone must not complete a late success after the PC has given up.

## Release scope

**v1 (this PRD)**  
Sideload APK in `android/` of this repo: QR/code pairing, encrypted local storage, foreground listener, biometric unlock over UDP, TCP, and Bluetooth.

**Out of this release**  
Play Store, Wear OS, cloud methods, silent auto-unlock, iOS, unlock/pair protocol changes, custom icons beyond a simple launcher mark if no asset is provided. Phone-initiated Windows lock is a separate feature (`agentic/features/lock-pc/`). Forced shutdown/restart is `agentic/features/power-control/`. Neither is Android v1 Phase 6.
