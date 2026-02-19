package com.pramod.validator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Offline status indicator banner that appears at the top of the screen
 * when the device is offline or when data is syncing
 */
@Composable
fun OfflineIndicator(
    isOnline: Boolean,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline || isSyncing,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isOnline && isSyncing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (isOnline && isSyncing) {
                        Icons.Default.CloudSync
                    } else {
                        Icons.Default.CloudOff
                    },
                    contentDescription = null,
                    tint = if (isOnline && isSyncing) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = if (isOnline && isSyncing) {
                        "Syncing data..."
                    } else {
                        "You're offline. Changes will sync when connection is restored."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline && isSyncing) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}


