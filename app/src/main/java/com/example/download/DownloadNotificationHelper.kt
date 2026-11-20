package com.example.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.example.MainActivity
import com.example.R
import java.util.UUID

object DownloadNotificationHelper {
    const val CHANNEL_PROGRESS = "abdullah_downloads_channel"
    const val CHANNEL_COMPLETE = "abdullah_complete_channel"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val progressChannel = NotificationChannel(
                CHANNEL_PROGRESS,
                context.getString(R.string.notification_channel_downloads_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE,
                context.getString(R.string.notification_channel_complete_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(progressChannel)
            nm.createNotificationChannel(completeChannel)
        }
    }

    fun buildProgressNotification(
        context: Context,
        workerId: UUID,
        title: String,
        progressPercent: Int,
        detailText: String
    ): Notification {
        ensureChannels(context)
        val cancelIntent = WorkManager.getInstance(context).createCancelPendingIntent(workerId)
        val openIntent = PendingIntent.getActivity(
            context,
            workerId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TAB, "downloads")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(detailText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), progressPercent <= 0)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelIntent)
            .build()
    }

    fun showCompletedNotification(
        context: Context,
        downloadId: Long,
        title: String,
        subtitle: String,
        enabled: Boolean
    ) {
        if (!enabled) return
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TAB, "downloads")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()
        runCatching { nm.notify((10_000 + downloadId).toInt(), notif) }
    }
}
