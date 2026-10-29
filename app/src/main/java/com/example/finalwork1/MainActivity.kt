package com.example.finalwork1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            FINALWORK1Theme(darkTheme = prefs.darkTheme) {
                MediaShelfApp(vm)
            }
        }
    }
}

private enum class Screen(val label: String) {
    CATALOG("Каталог"), FAVORITES("Избранное"), COLLECTIONS("Коллекции"), HISTORY("История"), SETTINGS("Настройки")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaShelfApp(vm: AppViewModel) {
    val characters by vm.characters.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val preferences by vm.preferences.collectAsStateWithLifecycle()
    val message by vm.syncMessage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf(Screen.CATALOG) }
    var selected by remember { mutableStateOf<CharacterEntity?>(null) }
    var collectionId by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Все") }
    var personalStatusFilter by remember { mutableStateOf("Все") }
    var trackingOnly by remember { mutableStateOf(false) }
    var showCreateCollection by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Column {
                        Text("PORTAL ARCHIVE", fontWeight = FontWeight.Black)
                        Text("DIMENSION C-137 · CHARACTER DATABASE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.refresh() }, enabled = !busy) { Text(if (busy) "СИНХРОН..." else "ОБНОВИТЬ") }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item; collectionId = null },
                        icon = { Text(when(item) { Screen.CATALOG -> "⌕"; Screen.FAVORITES -> "★"; Screen.COLLECTIONS -> "▤"; Screen.HISTORY -> "◷"; Screen.SETTINGS -> "⚙" }) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(message, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            when (screen) {
                Screen.CATALOG -> CatalogScreen(
                    characters = characters,
                    query = query, onQuery = { query = it },
                    statusFilter = statusFilter, onStatus = { statusFilter = it },
                    personalStatusFilter = personalStatusFilter, onPersonalStatus = { personalStatusFilter = it },
                    trackingOnly = trackingOnly, onTrackingOnly = { trackingOnly = it },
                    showHidden = preferences.showHidden,
                    onOpen = { selected = it; vm.visit(it) },
                    onPin = { c -> vm.pin(c.id, !c.isPinned) },
                    onHide = { c -> vm.hide(c.id, !c.isHidden) },
                    onTrack = { c, s -> vm.track(c.id, s) },
                )
                Screen.FAVORITES -> FavoritesScreen(
                    characters = characters.filter { it.isPinned },
                    onOpen = { selected = it; vm.visit(it) },
                    onRemove = { vm.pin(it.id, false) },
                )
                Screen.COLLECTIONS -> {
                    if (collectionId == null) {
                        CollectionsScreen(collections, vm, onCreate = { showCreateCollection = true }, onOpen = { collectionId = it.id })
                    } else {
                        val id = collectionId!!
                        val members by vm.collectionCharacters(id).collectAsStateWithLifecycle(initialValue = emptyList())
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            TextButton(onClick = { collectionId = null }) { Text("← Все коллекции") }
                            Text(collections.firstOrNull { it.id == id }?.title ?: "Коллекция", style = MaterialTheme.typography.headlineSmall)
                            LazyColumn {
                                items(members, key = { it.id }) { c ->
                                    CharacterRow(c, onClick = { selected = c; vm.visit(c) }, trailing = {
                                        TextButton(onClick = { vm.removeFromCollection(id, c.id) }) { Text("Убрать") }
                                    })
                                }
                            }
                        }
                    }
                }
                Screen.HISTORY -> HistoryScreen(
                    history = history.mapNotNull { h -> characters.firstOrNull { it.id == h.characterId } }.distinctBy { it.id },
                    onOpen = { selected = it; vm.visit(it) },
                )
                Screen.SETTINGS -> SettingsScreen(
                    showHidden = preferences.showHidden, onShowHidden = vm::setShowHidden,
                    preload = preferences.preloadPages, onPreload = vm::setPreloadPages,
                    darkTheme = preferences.darkTheme, onDarkTheme = vm::setDarkTheme,
                    onRefresh = vm::refresh,
                )
            }
        }
    }

    if (selected != null) {
        CharacterDetailDialog(
            character = selected!!,
            collections = collections,
            onDismiss = { selected = null },
            onTracking = { vm.track(selected!!.id, it); selected = selected!!.copy(tracking = it) },
            onPin = { vm.pin(selected!!.id, !selected!!.isPinned); selected = selected!!.copy(isPinned = !selected!!.isPinned) },
            onHide = { vm.hide(selected!!.id, !selected!!.isHidden); selected = selected!!.copy(isHidden = !selected!!.isHidden) },
            onNote = { vm.note(selected!!.id, it); selected = selected!!.copy(note = it) },
            onRate = { vm.rate(selected!!.id, it); selected = selected!!.copy(rating = it) },
            onAddToCollection = { id -> vm.addToCollection(id, selected!!.id) },
        )
    }

    if (showCreateCollection) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollection = false },
            onCreate = { title, description -> vm.createCollection(title, description); showCreateCollection = false },
        )
    }
}

@Composable
private fun CatalogScreen(
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
        onlyTracking = false,
    )
    val filteredIds = CatalogFilterLogic.sort(
        characters.map { it.toCatalogRow() }.filter { CatalogFilterLogic.matches(it, filter) }
    ).map { it.character.id }.toSet()
    val filtered = characters.filter { character ->
        character.id in filteredIds &&
            (!trackingOnly || character.isPinned || character.tracking != "NONE") &&
            (personalStatusFilter == "Все" || character.tracking == personalStatusFilter)
    }.sortedBy { it.name.lowercase() }

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

@Composable
private fun CharacterRow(character: CharacterEntity, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun CharacterDetailDialog(
    character: CharacterEntity,
    collections: List<UserCollectionEntity>,
    onDismiss: () -> Unit, onTracking: (String) -> Unit, onPin: () -> Unit, onHide: () -> Unit,
    onNote: (String) -> Unit, onRate: (Int) -> Unit, onAddToCollection: (Long) -> Unit,
) {
    var note by remember(character.id) { mutableStateOf(character.note) }
    var addExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(character.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                OutlinedTextField(note, { note = it }, label = { Text("Личная заметка") }, minLines = 2, maxLines = 5)
                Button(onClick = { onNote(note) }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить заметку") }
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

@Composable
private fun CollectionsScreen(
    collections: List<UserCollectionEntity>, vm: AppViewModel, onCreate: () -> Unit, onOpen: (UserCollectionEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Мои коллекции", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Button(onClick = onCreate) { Text("Создать") }
        }
        Text("Коллекции хранятся локально и доступны без сети.", style = MaterialTheme.typography.bodySmall)
        if (collections.isEmpty()) EmptyState("Коллекций пока нет", "Создай, например, список любимых персонажей или тех, кого хочешь изучить.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(collections, key = { it.id }) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(c) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title, fontWeight = FontWeight.SemiBold)
                            if (c.description.isNotBlank()) Text(c.description, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.deleteCollection(c.id) }) { Text("Удалить") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCollectionDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая коллекция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, minLines = 2)
            }
        },
        confirmButton = { Button(onClick = { onCreate(title, description) }, enabled = title.isNotBlank()) { Text("Создать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun FavoritesScreen(
    characters: List<CharacterEntity>,
    onOpen: (CharacterEntity) -> Unit,
    onRemove: (CharacterEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Избранные персонажи", style = MaterialTheme.typography.headlineSmall)
        Text("Персонажи, отмеченные звёздочкой в каталоге.")
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
private fun HistoryScreen(history: List<CharacterEntity>, onOpen: (CharacterEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Недавно открытые", style = MaterialTheme.typography.headlineSmall)
        Text("Последние посещённые карточки сохраняются локально.")
        if (history.isEmpty()) EmptyState("История пуста", "Открой карточку персонажа в каталоге.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(history, key = { it.id }) { CharacterRow(it, onClick = { onOpen(it) }) }
        }
    }
}

@Composable
private fun SettingsScreen(
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
private fun SettingSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun trackingLabel(value: String): String = when (value) {
    "TRACKING" -> "Любимый"
    "COMPLETED" -> "Не нравится"
    "PLANNED" -> "Интересный"
    "Все" -> "Все"
    else -> "Без оценки"
}
