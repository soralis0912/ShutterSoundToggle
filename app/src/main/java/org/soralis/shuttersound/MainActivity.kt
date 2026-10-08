package org.soralis.shuttersound

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import rikka.shizuku.Shizuku

class MainActivity : Activity() {

    private lateinit var shizukuText: TextView
    private lateinit var deviceText: TextView
    private lateinit var currentText: TextView
    private lateinit var errorText: TextView
    private lateinit var authorize: Button
    private lateinit var release: Button
    private lateinit var restore: Button

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
        ShutterSound.set(this, config) { result -> show(result.map { config }) }
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
        release.isEnabled = status == ShutterSound.Status.READY
        restore.isEnabled = status == ShutterSound.Status.READY
        if (status == ShutterSound.Status.READY) {
            ShutterSound.query(this, ::show)
        } else {
            currentText.setText(R.string.current_unknown)
            errorText.text = ""
        }
    }

    private fun show(result: Result<Int>) {
        result.onSuccess { config ->
            currentText.setText(
                when (config) {
                    ForceUse.FORCE_SYSTEM_ENFORCED -> R.string.current_enforced
                    ForceUse.FORCE_NONE -> R.string.current_none
                    else -> R.string.current_unknown
                }
            )
            errorText.text = ""
        }.onFailure { e ->
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
        private const val REQUEST_SHIZUKU = 1
        private const val REQUEST_NOTIFICATIONS = 2
    }
}
