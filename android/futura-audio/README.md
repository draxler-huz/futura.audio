# FUTURA AUDIO — native Android source

This is a Kotlin/Jetpack Compose Android project targeting API 36, with the supplied Oboe interface and a C++17 audio engine.

## Build

Requirements:

- JDK 17 or newer
- Android SDK Platform 36
- Android NDK and CMake 3.22.1
- Android SDK licenses accepted

From this directory:

```sh
./gradlew :app:assembleDebug
```

The Gradle wrapper downloads Gradle 8.13. The Android Gradle Plugin, Kotlin Compose compiler plugin, AndroidX Compose dependencies, and Oboe Prefab package are pinned in the project files.

## Audio input contract

The native engine accepts interleaved stereo Float32 PCM at 44,100 Hz. `OboePlayer.loadTrack()` checks the stereo frame alignment before sending the samples through JNI. `currentPositionFrames()` reports frames, not individual samples.

No decoded music files, streaming credentials, or media decoder were present in the supplied source package. The prototype's five track titles and format labels are retained as UI metadata; they do not correspond to bundled audio. `FuturaViewModel.loadTrackPcm(index, pcmData)` is the explicit input seam for a decoder or trusted PCM provider. Until PCM is supplied, the transport remains stopped and the Oboe callback emits silence rather than fabricated audio.

The alarm service uses Android's selected system alarm ringtone. It does not claim to play a queue track when there is no PCM source.

## Alarm behavior

- Exact one-shot alarms are scheduled with `AlarmManager.setAlarmClock()`.
- On Android 12 and newer, scheduling reports when the user must grant exact-alarm access; it does not silently substitute an inexact alarm.
- The saved trigger time is stored in device-protected preferences so the receiver can restore it after normal or locked boot.
- On Android 13 and newer, a rationale is shown before the notification permission request. “Not Now” and denial are remembered; the app continues and exact-alarm scheduling does not depend on notification permission. Users can re-enable notifications in Android's app notification settings.
- The alarm notification uses a full-screen intent and user-visible Snooze and Dismiss actions. It does not bypass Do Not Disturb.
- The visible “Deck Locked” screen is the prototype's dismissed state; it is not a hidden vault or authentication boundary.

The supplied launcher category was corrected to `android.intent.category.LAUNCHER`. No hidden-app/decoy mode, volume-key interception, or DND-bypass behavior is included.

## Supplied references

`reference/` contains the unmodified attached prototype archive, manifest, backend checklist, Kotlin blueprint, implementation notes, and the supplied Oboe engine snippet.
