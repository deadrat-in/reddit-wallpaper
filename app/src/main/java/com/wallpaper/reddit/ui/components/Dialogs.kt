package com.wallpaper.reddit.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallpaper.reddit.data.model.TargetScreen

@Composable
fun AddSubredditDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subreddit") },
        text = {
            Column {
                Text(
                    text = "Enter subreddit name (e.g. wallpapers, EarthPorn, Cyberpunk)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isError = false
                    },
                    label = { Text("Subreddit Name") },
                    placeholder = { Text("wallpapers") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = text.trim().removePrefix("r/").removePrefix("/")
                    if (clean.isNotBlank()) {
                        onConfirm(clean)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SetWallpaperDialog(
    onDismiss: () -> Unit,
    onSelectTarget: (TargetScreen) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Wallpaper") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Choose which screen to set this wallpaper on:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onSelectTarget(TargetScreen.BOTH) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(TargetScreen.BOTH.displayName)
                }
                OutlinedButton(
                    onClick = { onSelectTarget(TargetScreen.HOME) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(TargetScreen.HOME.displayName)
                }
                OutlinedButton(
                    onClick = { onSelectTarget(TargetScreen.LOCK) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(TargetScreen.LOCK.displayName)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
