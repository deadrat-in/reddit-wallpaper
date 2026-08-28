package com.wallpaper.reddit.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallpaper.reddit.data.db.entities.SubredditEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubredditChips(
    subreddits: List<SubredditEntity>,
    selectedSubreddit: String,
    onSubredditSelected: (String) -> Unit,
    onAddSubredditClick: () -> Unit,
    onDeleteSubreddit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Subreddit Action Chip
        AssistChip(
            onClick = onAddSubredditClick,
            label = { Text("Add") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Subreddit",
                    modifier = Modifier.size(18.dp)
                )
            },
            shape = RoundedCornerShape(20.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                labelColor = MaterialTheme.colorScheme.primary,
                leadingIconContentColor = MaterialTheme.colorScheme.primary
            )
        )

        // Subreddit Chips
        subreddits.forEach { sub ->
            val isSelected = sub.name.equals(selectedSubreddit, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSubredditSelected(sub.name) },
                label = { Text(sub.displayName) },
                trailingIcon = if (!sub.isDefault) {
                    {
                        IconButton(
                            onClick = { onDeleteSubreddit(sub.name) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else null,
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
