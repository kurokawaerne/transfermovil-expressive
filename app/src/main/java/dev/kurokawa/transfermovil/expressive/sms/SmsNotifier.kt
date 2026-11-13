package dev.kurokawa.transfermovil.expressive.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dev.kurokawa.transfermovil.expressive.MainActivity
import dev.kurokawa.transfermovil.expressive.R
import dev.kurokawa.transfermovil.expressive.core.SmsRecord

/** Bubble-ready notifications for parsed PAGOxMOVIL results
 *  (Notification.BubbleMetadata, Android 17-ready; targetSdk 35).
 *  The bubble expands to MainActivity showing the SMS inbox. */
object SmsNotifier {
    const val CHANNEL = "pago_sms"
    fun notifyParsed(ctx: Context, record: SmsRecord, title: String, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "PAGOxMOVIL", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Resultados analizados de PAGOxMOVIL"
                setAllowBubbles(true)
            })
        }
        val target = Intent(ctx, MainActivity::class.java).putExtra("open", "sms")
        val pi = PendingIntent.getActivity(ctx, 0, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        val builder = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_brand_mark)
            .setContentTitle(title).setContentText(text)
            .setContentIntent(pi).setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= 29) {
            val bubble = Notification.BubbleMetadata.Builder(pi, R.drawable.ic_brand_mark)
                .setDesiredHeight(600)
                .setAutoExpandBubble(false)
                .setSuppressNotification(false)
                .build()
            builder.setBubbleMetadata(bubble)
        }
        nm.notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
    }
}
