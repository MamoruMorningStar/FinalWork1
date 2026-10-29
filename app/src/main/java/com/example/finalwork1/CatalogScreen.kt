package com.example.finalwork1

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
internal fun CatalogScreen(
    characters: List<CharacterEntity>,
    query: String, onQuery: (String) -> Unit,
    statusFilter: String, onStatus: (String) -> Unit,
    personalStatusFilter: String, onPersonalStatus: (String) -> Unit,
    trackingOnly: Boolean, onTrackingOnly: (Boolean) -> Unit,
    showHidden: Boolean,
    onOpen: (CharacterEntity) -> Unit, onPin: (CharacterEntity) -> Unit,
    onHide: (CharacterEntity) -> Unit, onTrack: (CharacterEntity, String) -> Unit,
) {
    val filter = CatalogFilter(
        query = query,
        status = if (statusFilter == "Все") "" else statusFilter,
        showHidden = showHidden,
        onlyTracking = trackingOnly,
        personalStatus = if (personalStatusFilter == "Все") "" else personalStatusFilter,
    )
    val filtered = CatalogFilterLogic.sort(
        characters.map { it.toCatalogRow() }.filter { row ->
            CatalogFilterLogic.matches(row, filter)
        }
    ).mapNotNull { row -> characters.firstOrNull { it.id == row.character.id } }


    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ИССЛЕДОВАТЬ МУЛЬТИВСЕЛЕННУЮ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Поиск по имени, виду или происхождению") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            var expanded by remember { mutableStateOf(false) }
            var personalStatusExpanded by remember { mutableStateOf(false) }
            val personalOptions = listOf(
                "Все" to "Все",
                "NONE" to "Без оценки",
                "TRACKING" to "Любимый",
                "PLANNED" to "Интересный",
                "COMPLETED" to "Не нравится",
            )
            Box {
                TextButton(onClick = { expanded = true }) { Text("Статус: $statusFilter") }
                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                    listOf("Все", "Alive", "Dead", "unknown").forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { onStatus(item); expanded = false })
                    }
                }
            }
            Box {
                TextButton(onClick = { personalStatusExpanded = true }) {
                    Text("Личный статус: ${trackingLabel(personalStatusFilter)}")
                }
                DropdownMenu(expanded = personalStatusExpanded, onDismissRequest = { personalStatusExpanded = false }) {
                    personalOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onPersonalStatus(value); personalStatusExpanded = false },
                        )
                    }
                }
            }
            Text("API", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = trackingOnly, onClick = { onTrackingOnly(!trackingOnly) }, label = { Text("Мой список") })
            Text("${filtered.size} персонажей", style = MaterialTheme.typography.labelMedium)
        }
        if (characters.isEmpty()) {
            EmptyState("Пока нет локальных данных", "Подключись к интернету и нажми «Обновить». После загрузки каталог доступен офлайн.")
        } else if (filtered.isEmpty()) {
            EmptyState("Ничего не найдено", "Измени запрос или фильтры.")
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { c ->
                    CharacterRow(c, onClick = { onOpen(c) }, trailing = {
                        Row {
                            TextButton(onClick = { onPin(c) }) { Text(if (c.isPinned) "★" else "☆") }
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) { Text("⋮") }
                                DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(text = { Text(if (c.isHidden) "Показать" else "Скрыть") }, onClick = { onHide(c); expanded = false })
                                    listOf("NONE" to "Без оценки", "TRACKING" to "Любимый", "PLANNED" to "Интересный", "COMPLETED" to "Не нравится").forEach { (value, label) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = { onTrack(c, value); expanded = false })
                                    }
                                }
                            }
                        }
                    })
                }
            }
        }
    }
}

