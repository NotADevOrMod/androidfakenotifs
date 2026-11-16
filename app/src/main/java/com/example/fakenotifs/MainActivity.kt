package com.example.fakenotifs

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed interface Screen {
    data object Home : Screen
    data class Edit(val notif: NotifEntity?) : Screen
}

class MainActivity : ComponentActivity() {
    private val vm: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotifHelper.ensureChannel(this)
        setContent {
            AppTheme { AppRoot(vm) }
        }
    }
}

@Composable
fun AppRoot(vm: MainVM) {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    when (val s = screen) {
        is Screen.Home -> HomeScreen(
            vm = vm,
            onNew = { screen = Screen.Edit(null) },
            onEdit = { screen = Screen.Edit(it) }
        )
        is Screen.Edit -> {
            BackHandler { screen = Screen.Home }
            EditorScreen(
                initial = s.notif,
                loadApps = { vm.loadApps() },
                importIcon = { vm.importIcon(it) },
                onCancel = { screen = Screen.Home },
                onSave = { n, icon, image ->
                    vm.save(n, icon, image)
                    screen = Screen.Home
                }
            )
        }
    }
}
