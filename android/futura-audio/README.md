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

The supplied launcher category was corrected to `android.intent.category.LAUNCHER`. No DND-bypass behavior is included.

## On-demand ring (accessibility)

`accessibility/FuturaAccessibilityService.kt` rings the alarm in two ways:

- **Accessibility button:** the button in the navigation bar, from any screen.
- **Volume Up:** three presses within 1.5 seconds.

The user enables "Futura Audio: Ring alarm now" in Settings > Accessibility. The service never consumes volume keys, and it cannot read screen content (`canRetrieveWindowContent="false"`). It requests no event types, so it isn't woken for every UI event. Ringing this way does not cancel the next scheduled alarm. The in-app Wake Now action does.

## Stealth camouflage

`ui/alarm/AlarmCamouflageScreen.kt` shows a dim clock on a black background. Double-tap the background to reveal a 4-digit PIN pad. The screen compares the SHA-256 hash of the entered PIN against `correctPinHash`. It is a concealment feature, not a security boundary.

Launch flow (`ui/navigation/FuturaNavGraph.kt`): with no PIN saved, the app opens PIN setup (`ui/alarm/PinSetupScreen.kt`). Otherwise it opens the camouflage screen. The correct PIN opens the main app. The PIN hash is stored in DataStore (`data/security/SecurityPreferences.kt`). The start screen is chosen only after the stored PIN has loaded, so a configured user is never sent to PIN setup.

## Front end (matched to the design prototypes)

The Compose screens were ported from the two web prototypes. These are the mappings:

- **Music screen** (`ui/music/`): the design 1.1 now-playing UI, with synced lyrics, related tracks, waveform scrubber, stream inspector, EQ strip, and alarm deck overlay. It replaces the old `MusicDashboard` on the dashboard route. `MusicDashboard.kt` is no longer used. The queue shows the playback order and does not reorder.
- **Library matrix** (`ui/library/`): opened from the dock's library button, with a slide transition. It has songs, albums, artists, folders, and moods, with search, smart filters, and an alphabet strip. Tapping a track selects it, starts it if PCM is loaded, and returns to the deck. The node map and the 2D mood map are not ported; the folder view is flat, not a tree.
- **Wake-up screen** (`ui/WakeUpOverlay.kt`, `ui/SwipeToDismiss.kt`): the alarm prototype's status chips, clock, snooze, and swipe-to-dismiss.
- **Locked screen** (`ui/DeckLockedScreen.kt`): the alarm prototype's deck-locked view.

Known differences from the web prototypes:

- The music screen is driven by `FuturaViewModel`. Play, previous, next, and track selection call the view model, and Wake Now is in the dock behind a second tap. Play does nothing until PCM is supplied through `loadTrackPcm`, so the elapsed time stays at zero.
- The library's 36 entries come from the design 2 prototype, and the five design 1.1 preview tracks come first. Library entries have placeholder artwork, format (FLAC 16/44.1), and genre/folder values. Their BPM is estimated from energy, not measured, and their key is shown as "—".
- Web-only effects are approximated. Backdrop blur becomes translucent layers, glow shadows become borders and tints, and per-word lyric gradients become colour blends.
- The prototype fonts (Space Grotesk and JetBrains Mono) are not bundled. The screens use the system sans-serif and monospace.
- The queue reorder and lyric auto-scroll are approximations of the prototype's behaviour.
- Artwork (six 1024px PNGs in `res/drawable/`) adds about 9 MB to the APK.

## Background images

`res/drawable/art_bg_01.png` to `art_bg_20.png` are 20 generated background images (1024×1024). They are algorithmic abstract renders in the artwork's neon palette, not hand-made art, and some families repeat. No picker uses them yet.

## Supplied references

`reference/` contains the unmodified attached prototype archive, manifest, backend checklist, Kotlin blueprint, implementation notes, and the supplied Oboe engine snippet.
