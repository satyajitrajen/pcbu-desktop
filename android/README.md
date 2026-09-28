# JarvisAuth Android

Sideload companion for the JarvisAuth desktop in this repository.

## Requirements

- JDK 17
- Android SDK 35
- Android Studio or command-line `sdkmanager`

Create `android/local.properties` with your SDK path:

```
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

## Build

From `android/`:

```bash
./gradlew assembleDebug
```

Windows:

```bat
gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

Release (unsigned unless you add a local keystore):

```bat
gradlew.bat assembleRelease
```

To sign release, create `android/key.properties` (gitignored) and a local `.jks` next to it:

```
storeFile=release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is relative to `android/` unless it is an absolute path.

Do not commit `key.properties`, `*.jks`, or `*.keystore`.

## Install

Enable install from this source, then:

```bash
./gradlew installDebug
```

or copy the APK to the phone.

Application id: `com.jarvisauth.app`.

## Use with the desktop

1. Install JarvisAuth on the PC.
2. Pair from the phone (QR or code) on the same LAN.
3. Start listening. Automatic UDP/TCP/Bluetooth and Manual UDP all use that button.
4. If unlock never appears, open Battery help and allow ignoring battery optimizations.
5. Bluetooth pairing: bond the phone from the PC, then Start listening with Nearby devices allowed.

Reinstalling the app creates a new device id and ports. Re-pair.

Fonts: Sora and IBM Plex Mono (SIL Open Font License) are bundled under `app/src/main/res/font/`.

License: GPL-3. See `../LICENSE`.
