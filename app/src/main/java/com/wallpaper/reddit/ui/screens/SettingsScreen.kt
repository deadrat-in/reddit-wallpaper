package com.wallpaper.reddit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallpaper.reddit.data.model.OrientationFilter
import com.wallpaper.reddit.data.model.TargetScreen
import com.wallpaper.reddit.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Auto-Rotation
            Text(
                text = "AUTOMATIC ROTATION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enable Auto-Rotation", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Rotates wallpaper periodically in the background using downloaded wallpapers",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(
                            checked = uiState.autoRotationEnabled,
                            onCheckedChange = { viewModel.setAutoRotation(it) }
                        )
                    }

                    if (uiState.autoRotationEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Rotation Interval
                        Text("Interval: ${uiState.rotationIntervalHours} hours", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = uiState.rotationIntervalHours.toFloat(),
                            onValueChange = { viewModel.setRotationInterval(it.toLong()) },
                            valueRange = 1f..24f,
                            steps = 22
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target screen
                        Text("Apply to:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TargetScreen.values().forEach { target ->
                                FilterChip(
                                    selected = uiState.targetScreen == target,
                                    onClick = { viewModel.setTargetScreen(target) },
                                    label = { Text(target.displayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Only Favorites checkbox
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rotate Only Favorites", style = MaterialTheme.typography.bodyMedium)
                            Checkbox(
                                checked = uiState.onlyFavoritesForRotation,
                                onCheckedChange = { viewModel.setOnlyFavoritesForRotation(it) }
                            )
                        }
                    }
                }
            }

            // Section 2: Wallpaper Filters & Preferences
            Text(
                text = "PREFERENCES & FILTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Default Orientation", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrientationFilter.values().forEach { filter ->
                            FilterChip(
                                selected = uiState.defaultOrientation == filter,
                                onClick = { viewModel.setDefaultOrientation(filter) },
                                label = { Text(filter.displayName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allow NSFW Content", style = MaterialTheme.typography.titleMedium)
                            Text("Show posts marked as 18+ on Reddit", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = uiState.allowNsfw,
                            onCheckedChange = { viewModel.setAllowNsfw(it) }
                        )
                    }
                }
            }
        }
    }
}
