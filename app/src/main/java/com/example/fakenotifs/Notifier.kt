package com.example.fakenotifs

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min

object NotifHelper {
    private const val CHANNEL = "fake_notifs"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, "Fake notifications", NotificationManager.IMPORTANCE_HIGH)
        ch.description = "Notifications sent by Fake Notifs"
        nm.createNotificationChannel(ch)
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, n: NotifEntity) {
        ensureChannel(ctx)
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        // Tapping just dismisses it (AlarmReceiver ignores intents without a scheduled id).
        val tap = PendingIntent.getBroadcast(
            ctx, 0,
            Intent(ctx, AlarmReceiver::class.java).setAction("noop"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val icon = n.iconPath?.let { decode(it, 192) }?.let { circleCrop(it) }
            ?: letterIcon(n.appName.ifBlank { n.title }.take(1).uppercase().ifBlank { "?" }, n.accent)

        val extras = Bundle().apply { putString("android.substName", n.appName) }

        val b = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(n.accent)
            .setContentTitle(n.title)
            .setContentText(n.message)
            .setLargeIcon(icon)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(tap)
            .addExtras(extras)

        val pic = n.imagePath?.let { decode(it, 1024) }
        if (pic != null) {
            b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(pic).setSummaryText(n.message))
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(n.message))
        }

        val id = (System.nanoTime() and 0x7fffffff).toInt()
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    private fun decode(path: String, maxSize: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun circleCrop(src: Bitmap): Bitmap {
        val size = 192
        val side = min(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        Canvas(out).drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }

    private fun letterIcon(letter: String, color: Int): Bitmap {
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = Color.WHITE
        p.textSize = size * 0.5f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textAlign = Paint.Align.CENTER
        val y = size / 2f - (p.descent() + p.ascent()) / 2f
        c.drawText(letter, size / 2f, y, p)
        return bmp
    }
}

object Scheduler {
    fun arm(ctx: Context, scheduledId: Long, triggerAt: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, scheduledId)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(ctx: Context, scheduledId: Long) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, scheduledId))
    }

    private fun pending(ctx: Context, scheduledId: Long): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra("sid", scheduledId)
        return PendingIntent.getBroadcast(
            ctx, scheduledId.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sid = intent.getLongExtra("sid", -1L)
        if (sid < 0) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.get(app).dao()
                val row = dao.scheduledAll().firstOrNull { it.id == sid }
                if (row != null) {
                    dao.notif(row.notifId)?.let { NotifHelper.show(app, it) }
                    dao.removeScheduled(sid)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDb.get(app).dao()
                val now = System.currentTimeMillis()
                dao.scheduledAll().forEach {
                    if (it.triggerAt <= now) dao.removeScheduled(it.id)
                    else Scheduler.arm(app, it.id, it.triggerAt)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
