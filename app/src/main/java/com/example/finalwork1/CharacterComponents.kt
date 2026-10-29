package com.example.finalwork1

import coil.compose.AsyncImage
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finalwork1.data.local.CharacterEntity
import com.example.finalwork1.data.local.UserCollectionEntity
import com.example.finalwork1.domain.CatalogFilter
import com.example.finalwork1.domain.CatalogFilterLogic
import com.example.finalwork1.domain.toCatalogRow
import com.example.finalwork1.ui.theme.FINALWORK1Theme


@Composable
internal fun CharacterRow(character: CharacterEntity, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = character.imageUrl,
                contentDescription = character.name,
                modifier = Modifier.size(64.dp).padding(end = 8.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(character.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${character.species} · ${character.status}", style = MaterialTheme.typography.bodySmall)
                Text("Происхождение: ${character.origin}", style = MaterialTheme.typography.bodySmall)
                if (character.tracking != "NONE") Text("Моя оценка: ${trackingLabel(character.tracking)}", style = MaterialTheme.typography.labelSmall)
                if (character.note.isNotBlank()) Text("Есть заметка", style = MaterialTheme.typography.labelSmall)
            }
            trailing()
        }
    }
}

@Composable
internal fun CharacterDetailDialog(
    character: CharacterEntity,
    collections: List<UserCollectionEntity>,
    onDismiss: () -> Unit, onTracking: (String) -> Unit, onPin: () -> Unit, onHide: () -> Unit,
    onNote: (String) -> Unit, onRate: (Int) -> Unit, onAddToCollection: (Long) -> Unit,
) {
    var note by remember(character.id) { mutableStateOf(character.note) }
    var addExpanded by remember { mutableStateOf(false) }
    var noteDirty by remember(character.id) { mutableStateOf(false) }
    LaunchedEffect(character.note, character.id) { if (!noteDirty) note = character.note }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(character.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AsyncImage(model = character.imageUrl, contentDescription = character.name,
                    modifier = Modifier.fillMaxWidth().height(180.dp))
                Text("Статус: ${character.status}")
                Text("Вид: ${character.species}")
                Text("Пол: ${character.gender}")
                Text("Происхождение: ${character.origin}")
                Text("Локация: ${character.location}")
                Text("Появлений в эпизодах: ${character.episodeCount}")
                Text("Данные кэша: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(character.fetchedAt))}")
                Text("Личный статус", fontWeight = FontWeight.SemiBold)
                var trackingExpanded by remember(character.id) { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { trackingExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("${trackingLabel(character.tracking)}  ▾")
                    }
                    DropdownMenu(expanded = trackingExpanded, onDismissRequest = { trackingExpanded = false }) {
                        listOf("NONE", "TRACKING", "PLANNED", "COMPLETED").forEach { state ->
                            DropdownMenuItem(
                                text = { Text(trackingLabel(state)) },
                                onClick = { onTracking(state); trackingExpanded = false },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPin) { Text(if (character.isPinned) "Убрать из избранного" else "В избранное") }
                    OutlinedButton(onClick = onHide) { Text(if (character.isHidden) "Показать" else "Скрыть") }
                }
                Text("Моя оценка: ${character.rating}/5")
                Row { (1..5).forEach { value -> TextButton(onClick = { onRate(value) }) { Text(if (value <= character.rating) "★" else "☆") } } }
                OutlinedTextField(note, { note = it; noteDirty = true }, label = { Text("Личная заметка") }, minLines = 2, maxLines = 5)
                Button(onClick = { onNote(note); noteDirty = false }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить заметку") }
                Box {
                    OutlinedButton(onClick = { addExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text("Добавить в коллекцию") }
                    DropdownMenu(expanded = addExpanded, onDismissRequest = { addExpanded = false }) {
                        if (collections.isEmpty()) DropdownMenuItem(text = { Text("Сначала создай коллекцию") }, onClick = { addExpanded = false })
                        collections.forEach { collection ->
                            DropdownMenuItem(text = { Text(collection.title) }, onClick = { onAddToCollection(collection.id); addExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

