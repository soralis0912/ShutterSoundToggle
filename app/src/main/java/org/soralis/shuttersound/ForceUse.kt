package org.soralis.shuttersound

import android.media.MediaActionSound

/**
 * AudioSystem force-use values from system/media audio-base.h. They are part
 * of the audio HAL interface, so they do not move between releases.
 */
object ForceUse {
    const val FOR_SYSTEM = 4
    const val FORCE_NONE = 0
    const val FORCE_SYSTEM_ENFORCED = 11

    /**
     * What AudioService itself sets FOR_SYSTEM to on this device: enforced when
     * the device, the SIM's carrier config or audio.camerasound.force asks for
     * an audible shutter, none otherwise.
     */
    fun deviceDefault(): Int =
        if (MediaActionSound.mustPlayShutterSound()) FORCE_SYSTEM_ENFORCED else FORCE_NONE
}
