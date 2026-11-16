@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.fakenotifs

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pal.Secondary,
    unfocusedBorderColor = Pal.Border,
    focusedLabelColor = Pal.SecondaryDark,
    cursorColor = Pal.SecondaryDark,
    focusedTextColor = Pal.Text,
    unfocusedTextColor = Pal.Text
)

// ───────────────────────── Home (Library + Queue) ─────────────────────────

@Composable
fun HomeScreen(vm: MainVM, onNew: () -> Unit, onEdit: (NotifEntity) -> Unit) {
    val ctx = LocalContext.current
    val notifs by vm.notifs.collectAsState()
    val scheduled by vm.scheduled.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var delayFor by remember { mutableStateOf<NotifEntity?>(null) }
    var timeFor by remember { mutableStateOf<NotifEntity?>(null) }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Pal.PrimaryDark,
                    selectedTextColor = Pal.PrimaryDark,
                    indicatorColor = Pal.PrimaryLight,
                    unselectedIconColor = Pal.Muted,
                    unselectedTextColor = Pal.Muted
                )
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 }, colors = colors,
                    icon = { Icon(Icons.Default.Notifications, null) },
                    label = { Text("Library", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 }, colors = colors,
                    icon = { Icon(Icons.Default.Schedule, null) },
                    label = { Text("Queue (${scheduled.size})", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text("Fake Notifs", color = Pal.Text, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Text(
                    if (tab == 0) "Pick one and send it." else "Waiting to go off.",
                    color = Pal.Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
            if (tab == 0) {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { AppButton("New notification", onNew, Modifier.fillMaxWidth()) }
                    items(notifs, key = { it.id }) { n ->
                        AppCard {
                            NotificationPreview(n.appName, n.title, n.message, n.iconPath, n.imagePath, n.accent)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AppButton("Send", { vm.sendNow(n) }, Modifier.weight(1f), compact = true)
                                AppButton(
                                    "Delay", { delayFor = n }, Modifier.weight(1f),
                                    face = Pal.Secondary, compact = true
                                )
                                AppButton(
                                    "At time", { timeFor = n }, Modifier.weight(1f),
                                    face = Pal.Amber, compact = true
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = { onEdit(n) }) { Icon(Icons.Default.Edit, "Edit", tint = Pal.Muted) }
                                IconButton(onClick = { vm.delete(n) }) { Icon(Icons.Default.Delete, "Delete", tint = Pal.Red) }
                            }
                        }
                    }
                }
            } else {
                if (scheduled.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Nothing scheduled. Tap Delay or At time on a notification to queue it.",
                            color = Pal.Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val fmt = remember { SimpleDateFormat("EEE d MMM, h:mm:ss a", Locale.getDefault()) }
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(scheduled, key = { it.id }) { s ->
                            val n = notifs.firstOrNull { it.id == s.notifId }
                            AppCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(n?.title ?: "Deleted notification", color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                        Text(n?.appName ?: "", color = Pal.Muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(fmt.format(Date(s.triggerAt)), color = Pal.SecondaryDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    AppButton(
                                        "Cancel", { vm.cancelScheduled(s) }, Modifier.width(96.dp),
                                        face = Pal.Red, compact = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    delayFor?.let { n ->
        DelayDialog(
            onDismiss = { delayFor = null },
            onPick = { seconds ->
                vm.scheduleIn(n, seconds)
                Toast.makeText(ctx, "Queued", Toast.LENGTH_SHORT).show()
                delayFor = null
            }
        )
    }
    timeFor?.let { n ->
        TimeDialog(
            onDismiss = { timeFor = null },
            onPick = { millis ->
                vm.scheduleAt(n, millis)
                Toast.makeText(ctx, "Scheduled", Toast.LENGTH_SHORT).show()
                timeFor = null
            }
        )
    }
}

@Composable
fun DelayDialog(onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val presets = listOf(
        "5 sec" to 5L, "15 sec" to 15L, "30 sec" to 30L, "1 min" to 60L,
        "5 min" to 300L, "15 min" to 900L, "1 hour" to 3600L
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Send in…", color = Pal.Text, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { (label, secs) -> Pill(label) { onPick(secs) } }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it.filter(Char::isDigit).take(6) },
                        label = { Text("Custom seconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    AppButton(
                        "Go",
                        { custom.toLongOrNull()?.takeIf { it > 0 }?.let(onPick) },
                        Modifier.width(72.dp),
                        compact = true
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Pal.Muted, fontWeight = FontWeight.SemiBold) } }
    )
}

@Composable
fun TimeDialog(onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val ctx = LocalContext.current
    val now = remember { Calendar.getInstance() }
    val state = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = DateFormat.is24HourFormat(ctx)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Send at…", color = Pal.Text, fontWeight = FontWeight.SemiBold) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                val c = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, state.hour)
                    set(Calendar.MINUTE, state.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (c.timeInMillis <= System.currentTimeMillis()) c.add(Calendar.DAY_OF_YEAR, 1)
                onPick(c.timeInMillis)
            }) { Text("Schedule", color = Pal.PrimaryDark, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Pal.Muted, fontWeight = FontWeight.SemiBold) } }
    )
}

// ───────────────────────── Editor ─────────────────────────

private val swatches = listOf(
    0xFF4F46E5, 0xFF0EA5E9, 0xFFEF4444, 0xFFF59E0B, 0xFF8B5CF6,
    0xFFF97316, 0xFFEC4899, 0xFF10B981, 0xFF6B7280, 0xFF111827
).map { it.toInt() }

@Composable
fun EditorScreen(
    initial: NotifEntity?,
    loadApps: suspend () -> List<InstalledApp>,
    importIcon: suspend (String) -> String?,
    onCancel: () -> Unit,
    onSave: (NotifEntity, Any?, Any?) -> Unit
) {
    var appName by remember { mutableStateOf(initial?.appName ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var message by remember { mutableStateOf(initial?.message ?: "") }
    var accent by remember { mutableIntStateOf(initial?.accent ?: swatches[0]) }
    var iconSrc by remember { mutableStateOf<Any?>(initial?.iconPath) }
    var imageSrc by remember { mutableStateOf<Any?>(initial?.imagePath) }
    var showApps by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pickIcon = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) iconSrc = uri
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageSrc = uri
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    Scaffold(containerColor = Color.White) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Close", tint = Pal.Muted) }
                Text(
                    if (initial == null) "New notification" else "Edit notification",
                    color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 22.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            NotificationPreview(appName, title, message, iconSrc, imageSrc, accent)
            Spacer(Modifier.height(16.dp))

            AppButton(
                "Pick from installed apps", { showApps = true }, Modifier.fillMaxWidth(),
                face = Pal.Primary
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = appName, onValueChange = { appName = it },
                label = { Text("App name") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Title") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = message, onValueChange = { message = it },
                label = { Text("Message") }, minLines = 2,
                shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            Text("Accent color", color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                swatches.forEach { c ->
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (c == accent) 3.dp else 0.dp, if (c == accent) Pal.Text else Color.Transparent, CircleShape)
                            .clickable { accent = c },
                        contentAlignment = Alignment.Center
                    ) {
                        if (c == accent) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Pictures", color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton(
                    if (iconSrc == null) "Add app icon" else "Change icon",
                    { pickIcon.launch(imageOnly) }, Modifier.weight(1f),
                    face = Pal.Secondary, compact = true
                )
                AppButton(
                    if (imageSrc == null) "Attach picture" else "Change picture",
                    { pickImage.launch(imageOnly) }, Modifier.weight(1f),
                    face = Pal.Secondary, compact = true
                )
            }
            if (iconSrc != null || imageSrc != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = { iconSrc = null }, enabled = iconSrc != null, modifier = Modifier.weight(1f)) {
                        Text("Remove icon", color = if (iconSrc != null) Pal.Red else Pal.Muted, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { imageSrc = null }, enabled = imageSrc != null, modifier = Modifier.weight(1f)) {
                        Text("Remove picture", color = if (imageSrc != null) Pal.Red else Pal.Muted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            AppButton(
                "Save",
                {
                    onSave(
                        (initial ?: NotifEntity(appName = "", title = "", message = ""))
                            .copy(appName = appName.trim(), title = title.trim(), message = message.trim(), accent = accent),
                        iconSrc, imageSrc
                    )
                },
                Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() || message.isNotBlank()
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showApps) {
        AppPickerSheet(
            loadApps = loadApps,
            onDismiss = { showApps = false },
            onPick = { app ->
                showApps = false
                appName = app.label
                scope.launch { importIcon(app.packageName)?.let { iconSrc = it } }
            }
        )
    }
}

@Composable
fun AppPickerSheet(
    loadApps: suspend () -> List<InstalledApp>,
    onDismiss: () -> Unit,
    onPick: (InstalledApp) -> Unit
) {
    val pm = LocalContext.current.packageManager
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = loadApps() }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp)) {
            Text("Pick an app", color = Pal.Text, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Search apps") }, singleLine = true,
                shape = RoundedCornerShape(14.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Loading apps…", color = Pal.Muted)
                }
            } else {
                val shown = remember(list, query) { list.filter { it.label.contains(query.trim(), ignoreCase = true) } }
                if (shown.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No apps match.", color = Pal.Muted)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(shown, key = { it.packageName }) { app -> AppRow(app, pm) { onPick(app) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, pm: PackageManager, onClick: () -> Unit) {
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { pm.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp)) {
            icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(14.dp))
        Text(app.label, color = Pal.Text, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}
