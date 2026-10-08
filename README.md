# Shutter Sound Toggle

Phones sold in Japan and a few other regions play the camera shutter sound
even in silent mode. This app switches that enforcement off and back on
through [Shizuku](https://shizuku.rikka.app/), with no root.

日本向けなどの端末は、マナーモードでもシャッター音が鳴るよう強制されています。
このアプリは Shizuku を使い、root なしでその強制を解除したり戻したりします。

## How it works

When a device, its SIM's carrier config or `audio.camerasound.force` asks
for an audible shutter, AudioService sets the audio policy's force-use for
`FOR_SYSTEM` to `FORCE_SYSTEM_ENFORCED`. The shutter then plays on
`STREAM_SYSTEM_ENFORCED`, which the ringer mode does not mute.

The app starts a Shizuku user service, running as `shell`, which holds the
`MODIFY_AUDIO_ROUTING` permission that audioserver checks. The service calls
`AudioSystem.setForceUse(FOR_SYSTEM, FORCE_NONE)` to lift the enforcement,
and `FORCE_SYSTEM_ENFORCED` to put it back. What to put back comes from
`MediaActionSound.mustPlayShutterSound()`, so a device that does not enforce
it is left alone.

Nothing device specific is involved; this is the AOSP audio policy.

## Things to know

- The change lives in audioserver only. A reboot or an audioserver restart
  restores the device default, and nothing else does. **Uninstalling the app
  does not undo it**, so restore first.
- With the enforcement off, the shutter follows the ringer mode: it is quiet
  in silent or vibrate mode only. Other sounds on the enforced stream are
  affected the same way.
- On ColorOS (OPPO, OnePlus, realme), turn on *Disable permission monitoring*
  in the developer options before starting Shizuku, or the shell calls are
  refused.
- Respect the privacy of the people around you and the rules where you live.

## Use

1. Install and start Shizuku (wireless debugging or adb; root also works).
2. Open the app, grant the Shizuku permission.
3. *Stop enforcing the shutter sound*, or use the quick settings tile.
   A notification stays while the setting differs from the device default
   and restores it from its button.

## Build

```sh
./gradlew assembleDebug
```

Requires Android 13 (API 33) or later.

## License

[NYSL](http://www.kmonos.net/nysl/) Version 0.9982. See [LICENSE](LICENSE).

The libraries it uses, Shizuku API and HiddenApiBypass, are under the
Apache License 2.0.
