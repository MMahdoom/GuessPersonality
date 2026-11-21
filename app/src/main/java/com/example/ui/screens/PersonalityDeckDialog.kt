package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.model.Personality

@Composable
fun PersonalityDeckDialog(
    allPersonalities: List<Personality>,
    selectionMode: Boolean,
    initialSelectedIds: Set<Int> = emptySet(),
    onConfirmSelection: ((List<Int>, Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedIds by remember(initialSelectedIds) { mutableStateOf(initialSelectedIds) }

    val regions = remember { listOf("All", "Tamil", "Pan-Indian") }
    val categories = remember {
        listOf(
            "All",
            "Cinema & Actors",
            "Directors & Music",
            "Sports",
            "Literature & History",
            "Public Figures"
        )
    }

    val filtered by remember(allPersonalities, searchQuery, selectedRegion, selectedCategory) {
        derivedStateOf {
            val q = searchQuery.trim().lowercase()
            allPersonalities.filter { p ->
                val matchesRegion = selectedRegion == "All" || p.region == selectedRegion
                val matchesCat = selectedCategory == "All" || p.category == selectedCategory
                val matchesQuery = q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.tamilName.lowercase().contains(q) ||
                    p.category.lowercase().contains(q) ||
                    p.era.lowercase().contains(q)
                matchesRegion && matchesCat && matchesQuery
            }
        }
    }

    val tamilCount = remember(allPersonalities) { allPersonalities.count { it.region == "Tamil" } }
    val panIndianCount = remember(allPersonalities) { allPersonalities.count { it.region == "Pan-Indian" } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectionMode) "Host Deck Builder" else "100 Indian Personalities",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = if (selectionMode) {
                                        "${selectedIds.size} selected • Players pick 1–${selectedIds.size.coerceAtLeast(1)}"
                                    } else {
                                        "$tamilCount Tamil • $panIndianCount Pan-Indian Icons"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_deck_dialog_button")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search English or தமிழ் name...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deck_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            regions.forEach { region ->
                                FilterChip(
                                    selected = selectedRegion == region,
                                    onClick = { selectedRegion = region },
                                    label = {
                                        val countLabel = when (region) {
                                            "Tamil" -> "Tamil ($tamilCount)"
                                            "Pan-Indian" -> "Pan-Indian ($panIndianCount)"
                                            else -> "All (${allPersonalities.size})"
                                        }
                                        Text(countLabel)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        if (selectionMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedIds = allPersonalities.shuffled().take(15).map { it.id }.toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preset_15_button")
                                ) {
                                    Text("Quick 15")
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedIds = allPersonalities
                                            .filter { it.region == "Tamil" }
                                            .shuffled()
                                            .take(25)
                                            .map { it.id }
                                            .toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preset_tamil25_button")
                                ) {
                                    Text("Tamil 25")
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedIds = allPersonalities.shuffled().take(40).map { it.id }.toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preset_40_button")
                                ) {
                                    Text("Mix 40")
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedIds = allPersonalities.map { it.id }.toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preset_all100_button")
                                ) {
                                    Text("All 100")
                                }
                                OutlinedButton(
                                    onClick = { selectedIds = emptySet() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("preset_clear_button")
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (selectionMode && onConfirmSelection != null) {
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onConfirmSelection(selectedIds.toList(), false)
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("save_deck_only_button")
                            ) {
                                Text("Lock (${selectedIds.size})")
                            }
                            Button(
                                onClick = {
                                    onConfirmSelection(selectedIds.toList(), true)
                                },
                                enabled = selectedIds.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(50.dp)
                                    .testTag("finalize_and_play_button")
                            ) {
                                Text("Finalize & Start (${selectedIds.size})")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { person ->
                    val isSelected = person.id in selectedIds
                    PersonalityListRow(
                        personality = person,
                        selectionMode = selectionMode,
                        selected = isSelected,
                        onToggle = {
                            if (selectionMode) {
                                selectedIds = if (isSelected) {
                                    selectedIds - person.id
                                } else {
                                    selectedIds + person.id
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalityListRow(
    personality: Personality,
    selectionMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected && selectionMode) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        border = BorderStroke(
            width = if (selected && selectionMode) 1.5.dp else 1.dp,
            color = if (selected && selectionMode) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selectionMode) {
                    Modifier.clickable(onClick = onToggle)
                } else {
                    Modifier
                }
            )
            .testTag("personality_item_${personality.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = personality.imageUrl,
                    contentDescription = personality.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Text(
                            text = personality.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    error = {
                        Text(
                            text = personality.initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${personality.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = personality.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (personality.tamilName.isNotBlank()) {
                    Text(
                        text = personality.tamilName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallTagBadge(
                        text = personality.region,
                        isAccent = personality.region == "Tamil"
                    )
                    SmallTagBadge(text = personality.category, isAccent = false)
                    SmallTagBadge(text = personality.era, isAccent = false)
                }
            }

            if (selectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallTagBadge(text: String, isAccent: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAccent) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isAccent) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
