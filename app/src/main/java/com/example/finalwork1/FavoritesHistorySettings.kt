package com.example.finalwork1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finalwork1.data.local.RecentVisitEntity
import com.example.finalwork1.data.local.CharacterEntity


@Composable
internal fun FavoritesScreen(
    characters: List<CharacterEntity>,
    onOpen: (CharacterEntity) -> Unit,
    onRemove: (CharacterEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Избранные персонажи", style = MaterialTheme.typography.headlineSmall)
        Text("Персонажи, отмеченные звёздочкой. Скрытые учитывают настройку видимости.")
        if (characters.isEmpty()) {
            EmptyState("Избранное пока пусто", "Открой каталог и нажми ☆ рядом с персонажем, чтобы добавить его сюда.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(characters, key = { it.id }) { character ->
                    CharacterRow(character, onClick = { onOpen(character) }, trailing = {
                        TextButton(onClick = { onRemove(character) }) { Text("★") }
                    })
                }
            }
        }
    }
}

@Composable
internal fun HistoryScreen(
    history: List<RecentVisitEntity>,
    characters: List<CharacterEntity>,
    onOpen: (CharacterEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Очистить историю?") },
        text = { Text("Все записи истории будут удалены.") },
        confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } })
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Недавно открытые", style = MaterialTheme.typography.headlineSmall)
        Text("Последние посещённые карточки сохраняются локально.")
        if (history.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Очистить историю") }
        if (history.isEmpty()) EmptyState("История пуста", "Открой карточку персонажа в каталоге.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(history, key = { it.characterId }) { visit ->
                val character = characters.firstOrNull { it.id == visit.characterId }
                if (character == null) {
                    ListItem(headlineContent = { Text(visit.characterName) },
                        supportingContent = { Text("Карточка ещё не загружена в кэш") },
                        trailingContent = { TextButton(onClick = { onDelete(visit.id) }) { Text("Удалить") } })
                } else CharacterRow(character, onClick = { onOpen(character) }, trailing = {
                    TextButton(onClick = { onDelete(visit.id) }) { Text("Удалить") }
                })
            }
        }
    }
}

@Composable
internal fun SettingsScreen(
    showHidden: Boolean, onShowHidden: (Boolean) -> Unit,
    preload: Int, onPreload: (Int) -> Unit,
    darkTheme: Boolean, onDarkTheme: (Boolean) -> Unit, onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        SettingSwitch("Показывать скрытых персонажей", showHidden, onShowHidden)
        SettingSwitch("Тёмная тема", darkTheme, onDarkTheme)
        Text("Предзагрузка страниц: $preload")
        Slider(value = preload.toFloat(), onValueChange = { onPreload(it.toInt().coerceIn(1, 5)) }, valueRange = 1f..5f, steps = 3)
        Text("Значение хранится в DataStore. Кэш, коллекции, заметки и история хранятся в Room.")
        Button(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) { Text("Обновить каталог сейчас") }
        Text("Фоновое обновление выполняется WorkManager при наличии сети (примерно раз в 12 часов).")
        Text("Источник каталога: публичный Rick and Morty API.")
    }
}

@Composable
internal fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
internal fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun trackingLabel(value: String): String = when (value) {
    "TRACKING" -> "Любимый"
    "COMPLETED" -> "Не нравится"
    "PLANNED" -> "Интересный"
    "Все" -> "Все"
    else -> "Без оценки"
}
