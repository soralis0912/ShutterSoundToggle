package org.soralis.shuttersound

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import rikka.shizuku.Shizuku

/**
 * Talks to [ShutterService] through Shizuku. Everything here runs on the main
 * thread, and so do the callbacks.
 */
object ShutterSound {

    enum class Status { NOT_RUNNING, NOT_AUTHORIZED, READY }

    private const val TIMEOUT_MS = 8_000L

    private val main = Handler(Looper.getMainLooper())
    private var service: IShutterService? = null
    private val waiting = mutableListOf<(Result<IShutterService>) -> Unit>()

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShutterService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("audio")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) {
                finish(Result.failure(IllegalStateException("Shizuku returned a dead user service")))
                return
            }
            val bound = IShutterService.Stub.asInterface(binder)
            service = bound
            finish(Result.success(bound))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    // A cold start from the notification can get here before ShizukuProvider
    // has received the binder, so wait for it instead of failing.
    private val binderReceived = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            Shizuku.removeBinderReceivedListener(this)
            if (waiting.isNotEmpty()) connect()
        }
    }

    private val timeout = Runnable {
        finish(Result.failure(IllegalStateException("Timed out waiting for Shizuku")))
    }

    fun status(): Status = when {
        !Shizuku.pingBinder() || Shizuku.isPreV11() -> Status.NOT_RUNNING
        Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Status.NOT_AUTHORIZED
        else -> Status.READY
    }

    /**
     * Reads FOR_SYSTEM. The notification follows what was read, so that it
     * also appears once notifications are allowed after the change was made.
     */
    fun query(context: Context, callback: (Result<Int>) -> Unit) {
        val app = context.applicationContext
        withService { result ->
            val read = result.mapCatching { it.forSystem }
            read.onSuccess { sync(app, it) }
            callback(read)
        }
    }

    /** Sets FOR_SYSTEM and keeps the notification and the tile in step. */
    fun set(context: Context, config: Int, callback: (Result<Unit>) -> Unit) {
        val app = context.applicationContext
        withService { result ->
            val done = result.mapCatching { it.forSystem = config }
            if (done.isSuccess) {
                sync(app, config)
                TileService.requestListeningState(
                    app, ComponentName(app, ShutterTileService::class.java)
                )
            }
            callback(done)
        }
    }

    private fun sync(context: Context, config: Int) {
        if (config == ForceUse.deviceDefault()) Notifications.cancel(context)
        else Notifications.showChanged(context)
    }

    private fun withService(block: (Result<IShutterService>) -> Unit) {
        waiting += block
        if (waiting.size > 1) return
        main.postDelayed(timeout, TIMEOUT_MS)
        connect()
    }

    private fun connect() {
        service?.takeIf { it.asBinder().pingBinder() }?.let {
            finish(Result.success(it))
            return
        }
        when (status()) {
            Status.NOT_RUNNING -> if (!Shizuku.pingBinder()) {
                Shizuku.addBinderReceivedListener(binderReceived)
            } else {
                finish(Result.failure(IllegalStateException("Shizuku is too old")))
            }
            Status.NOT_AUTHORIZED ->
                finish(Result.failure(SecurityException("Shizuku permission is not granted")))
            Status.READY -> try {
                Shizuku.bindUserService(args, connection)
            } catch (e: RuntimeException) {
                finish(Result.failure(e))
            }
        }
    }

    private fun finish(result: Result<IShutterService>) {
        main.removeCallbacks(timeout)
        Shizuku.removeBinderReceivedListener(binderReceived)
        val callbacks = waiting.toList()
        waiting.clear()
        callbacks.forEach { it(result) }
    }
}
