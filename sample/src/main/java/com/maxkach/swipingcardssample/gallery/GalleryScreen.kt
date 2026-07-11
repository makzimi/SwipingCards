package com.maxkach.swipingcardssample.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The gallery list: one tappable card per demo, with a one-line explanation. */
@Composable
fun GalleryScreen(onOpen: (Destination) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().padding(16.dp)) {
            Text(
                text = "SwipingCards gallery",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = "Three demos, one deck component, three different card shapes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(Destination.entries) { destination ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(destination) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(destination.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    destination.blurb,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

/** Shared top bar with a back button, used by every example screen. */
@Composable
fun ExampleScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to gallery")
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

/** Temporary placeholder body for demos not yet implemented. */
@Composable
fun ComingSoonScreen(title: String, onBack: () -> Unit) {
    ExampleScaffold(title = title, onBack = onBack) { modifier ->
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("Coming soon", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
