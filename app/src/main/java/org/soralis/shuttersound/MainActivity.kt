package org.soralis.shuttersound

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var shizukuText: TextView
    private lateinit var deviceText: TextView
    private lateinit var currentText: TextView
    private lateinit var errorText: TextView
    private lateinit var messageText: TextView
    private lateinit var authorize: Button
    private lateinit var release: Button
    private lateinit var restore: Button
    private var current: Int? = null
    private var busy = false

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        shizukuText = findViewById(R.id.shizuku)
        deviceText = findViewById(R.id.device)
        currentText = findViewById(R.id.current)
        errorText = findViewById(R.id.error)
        messageText = findViewById(R.id.message)
        authorize = findViewById(R.id.authorize)
        release = findViewById(R.id.release)
        restore = findViewById(R.id.restore)

        authorize.setOnClickListener { Shizuku.requestPermission(REQUEST_SHIZUKU) }
        release.setOnClickListener {
            requestNotificationPermission()
            apply(ShutterTileService.released())
        }
        restore.setOnClickListener { apply(ForceUse.deviceDefault()) }
        findViewById<Button>(R.id.refresh).setOnClickListener { refresh() }

        deviceText.setText(
            if (ForceUse.deviceDefault() == ForceUse.FORCE_SYSTEM_ENFORCED) R.string.device_forced
            else R.string.device_not_forced
        )

        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        super.onDestroy()
    }

    private fun apply(config: Int) {
        if (busy) return
        val before = current
        if (before == config) {
            // Nothing to change, which is what a device that does not enforce
            // the sound through the audio policy looks like.
            toast(getString(R.string.result_unchanged))
            messageText.setText(
                if (config == ForceUse.FORCE_NONE) R.string.result_already_none
                else R.string.result_already_enforced
            )
            return
        }
        setBusy(true)
        ShutterSound.set(this, config) { result ->
            setBusy(false)
            messageText.text = ""
            result.onSuccess {
                Log.i(TAG, "FOR_SYSTEM $before -> $config")
                toast(getString(R.string.result_changed, label(before), label(config)))
            }.onFailure { e ->
                Log.e(TAG, "Could not set FOR_SYSTEM to $config", e)
                toast(getString(R.string.result_failed))
                messageText.text = e.message ?: e.toString()
            }
            // Read it back rather than trusting the call.
            refresh()
        }
    }

    private fun setBusy(value: Boolean) {
        busy = value
        if (value) currentText.setText(R.string.current_working)
        val ready = !value && ShutterSound.status() == ShutterSound.Status.READY
        release.isEnabled = ready
        restore.isEnabled = ready
    }

    private fun label(config: Int?): String = when (config) {
        ForceUse.FORCE_SYSTEM_ENFORCED -> getString(R.string.label_enforced)
        ForceUse.FORCE_NONE -> getString(R.string.label_none)
        null -> "?"
        else -> config.toString()
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun refresh() {
        val status = ShutterSound.status()
        shizukuText.setText(
            when (status) {
                ShutterSound.Status.NOT_RUNNING -> R.string.shizuku_not_running
                ShutterSound.Status.NOT_AUTHORIZED -> R.string.shizuku_not_authorized
                ShutterSound.Status.READY -> R.string.shizuku_ready
            }
        )
        authorize.isEnabled = status == ShutterSound.Status.NOT_AUTHORIZED
        release.isEnabled = !busy && status == ShutterSound.Status.READY
        restore.isEnabled = !busy && status == ShutterSound.Status.READY
        if (status == ShutterSound.Status.READY) {
            ShutterSound.query(this, ::show)
        } else {
            current = null
            currentText.setText(R.string.current_unknown)
            errorText.text = ""
        }
    }

    private fun show(result: Result<Int>) {
        if (busy) return
        result.onSuccess { config ->
            current = config
            currentText.text = getString(
                when (config) {
                    ForceUse.FORCE_SYSTEM_ENFORCED -> R.string.current_enforced
                    ForceUse.FORCE_NONE -> R.string.current_none
                    else -> R.string.current_unknown
                }
            ) + " (FOR_SYSTEM=$config)"
            errorText.text = ""
        }.onFailure { e ->
            Log.e(TAG, "Could not read FOR_SYSTEM", e)
            current = null
            currentText.setText(R.string.current_unknown)
            errorText.text = e.message ?: e.toString()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        refresh()
    }

    private fun requestNotificationPermission() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val TAG = "ShutterSound"
        private const val REQUEST_SHIZUKU = 1
        private const val REQUEST_NOTIFICATIONS = 2
    }
}
