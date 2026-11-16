package com.example.fakenotifs

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class InstalledApp(val label: String, val packageName: String)

class MainVM(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val dao = AppDb.get(ctx).dao()

    val notifs: StateFlow<List<NotifEntity>> =
        dao.notifs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val scheduled: StateFlow<List<ScheduledEntity>> =
        dao.scheduledFlow().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.count() == 0) seed()
        }
    }

    private suspend fun seed() {
        val green = 0xFF4F46E5.toInt()
        val blue = 0xFF0EA5E9.toInt()
        val orange = 0xFFF97316.toInt()
        val purple = 0xFF8B5CF6.toInt()
        val pink = 0xFFEC4899.toInt()
        dao.upsert(NotifEntity(appName = "Social", title = "Alex liked your photo", message = "Alex and 12 others liked your photo.", accent = pink))
        dao.upsert(NotifEntity(appName = "Calendar", title = "Meeting in 5 minutes", message = "Team sync starts at 3:00 PM.", accent = purple))
        dao.upsert(NotifEntity(appName = "Delivery", title = "Your package is arriving", message = "Out for delivery. 2 stops away.", accent = orange))
        dao.upsert(NotifEntity(appName = "Mail", title = "Your invoice is ready", message = "Invoice #2481 has been issued and is ready to view.", accent = blue))
        dao.upsert(NotifEntity(appName = "Messages", title = "Mom", message = "Are you coming home for dinner?", accent = green))
    }

    fun sendNow(n: NotifEntity) {
        viewModelScope.launch(Dispatchers.IO) { NotifHelper.show(ctx, n) }
    }

    fun scheduleIn(n: NotifEntity, seconds: Long) =
        scheduleAt(n, System.currentTimeMillis() + seconds * 1000)

    fun scheduleAt(n: NotifEntity, time: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = dao.addScheduled(ScheduledEntity(notifId = n.id, triggerAt = time))
            Scheduler.arm(ctx, id, time)
        }
    }

    fun cancelScheduled(s: ScheduledEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            Scheduler.cancel(ctx, s.id)
            dao.removeScheduled(s.id)
        }
    }

    fun save(base: NotifEntity, iconSrc: Any?, imageSrc: Any?) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(base.copy(iconPath = resolve(iconSrc), imagePath = resolve(imageSrc)))
        }
    }

    fun delete(n: NotifEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.scheduledAll().filter { it.notifId == n.id }.forEach { Scheduler.cancel(ctx, it.id) }
            dao.removeScheduledFor(n.id)
            dao.delete(n)
        }
    }

    suspend fun loadApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun importIcon(packageName: String): String? = withContext(Dispatchers.IO) {
        try {
            val bmp = ctx.packageManager.getApplicationIcon(packageName).toBitmap(256, 256)
            val dir = File(ctx.filesDir, "images").apply { mkdirs() }
            val f = File(dir, "${UUID.randomUUID()}.png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            f.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun resolve(src: Any?): String? = when (src) {
        is Uri -> copyUri(src)
        is String -> src
        else -> null
    }

    private fun copyUri(uri: Uri): String? = try {
        val dir = File(ctx.filesDir, "images").apply { mkdirs() }
        val f = File(dir, "${UUID.randomUUID()}.img")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            f.outputStream().use { out -> input.copyTo(out) }
        }
        f.absolutePath
    } catch (e: Exception) {
        null
    }
}
