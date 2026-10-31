package com.futura.audio.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val PREFS_NAME = "futura_notification_permission"
private const val KEY_PERMISSION_DECISION_RECORDED = "decision_recorded"

@Composable
fun NotificationPermissionGate(isAlarmLaunch: Boolean) {
    val context = LocalContext.current
    var showRationaleDialog by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        rememberDecision(context)
        // A denial is an opt-out from the prompt, not a reason to block alarm scheduling.
    }

    LaunchedEffect(isAlarmLaunch) {
        if (isAlarmLaunch || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return@LaunchedEffect
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) return@LaunchedEffect

        val decisionRecorded = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMISSION_DECISION_RECORDED, false)
        if (!decisionRecorded) {
            showRationaleDialog = true
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showRationaleDialog = false
                rememberDecision(context)
            },
            title = { Text("Enable Alarm Notifications") },
            text = {
                Text(
                    "FUTURA AUDIO uses notifications for visible alarm and player alerts. " +
                        "You can continue without notification access; alarm scheduling is handled separately.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        rememberDecision(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationaleDialog = false
                        rememberDecision(context)
                    },
                ) {
                    Text("Not Now")
                }
            },
        )
    }
}

private fun rememberDecision(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_PERMISSION_DECISION_RECORDED, true)
        .apply()
}
