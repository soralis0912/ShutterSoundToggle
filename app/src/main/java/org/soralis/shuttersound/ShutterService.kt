package org.soralis.shuttersound

import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method
import kotlin.system.exitProcess

/**
 * Runs in a Shizuku user service process, as shell (or root), which holds
 * MODIFY_AUDIO_ROUTING that audioserver checks in setForceUse.
 */
class ShutterService : IShutterService.Stub() {

    private val getForceUse: Method
    private val setForceUse: Method

    init {
        HiddenApiBypass.addHiddenApiExemptions("Landroid/media/AudioSystem;")
        val audioSystem = Class.forName("android.media.AudioSystem")
        getForceUse = audioSystem.getMethod("getForceUse", Int::class.java)
        setForceUse = audioSystem.getMethod("setForceUse", Int::class.java, Int::class.java)
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun getForSystem(): Int = getForceUse.invoke(null, ForceUse.FOR_SYSTEM) as Int

    override fun setForSystem(config: Int) {
        require(config == ForceUse.FORCE_NONE || config == ForceUse.FORCE_SYSTEM_ENFORCED) {
            "Only FORCE_NONE and FORCE_SYSTEM_ENFORCED are accepted, got $config"
        }
        val status = setForceUse.invoke(null, ForceUse.FOR_SYSTEM, config) as Int
        check(status == 0) { "AudioSystem.setForceUse returned $status" }
    }
}
