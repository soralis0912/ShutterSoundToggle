package org.soralis.shuttersound

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/** The reminder that stays up while FOR_SYSTEM differs from the device default. */
object Notifications {

    private const val CHANNEL = "changed"
    private const val ID = 1

    fun showChanged(context: Context) = post(context, R.string.notification_changed)

    fun showRestoreFailed(context: Context) = post(context, R.string.notification_restore_failed)

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }

    private fun post(context: Context, text: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val restore = PendingIntent.getBroadcast(
            context, 0, Intent(context, RestoreReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_shutter_off)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(text))
            .setStyle(Notification.BigTextStyle().bigText(context.getString(text)))
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(
                Notification.Action.Builder(
                    null, context.getString(R.string.action_restore), restore
                ).build()
            )
            .build()
        manager.notify(ID, notification)
    }
}
