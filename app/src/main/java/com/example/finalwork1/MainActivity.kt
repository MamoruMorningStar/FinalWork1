package com.example.finalwork1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.finalwork1.ui.theme.FINALWORK1Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by vm.preferences.collectAsStateWithLifecycle()
            FINALWORK1Theme(darkTheme = prefs.darkTheme) { MediaShelfApp(vm) }
        }
    }
}

internal enum class Screen(val route: String, val label: String, val symbol: String) {
    CATALOG("catalog", "Каталог", "⌕"), FAVORITES("favorites", "Избранное", "★"),
    COLLECTIONS("collections", "Коллекции", "▤"), HISTORY("history", "История", "◷"),
    SETTINGS("settings", "Настройки", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MediaShelfApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val characters by vm.characters.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val message by vm.syncMessage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PORTAL ARCHIVE") }, actions = {
                TextButton(onClick = vm::refresh, enabled = !busy) { Text(if (busy) "СИНХРОН..." else "ОБНОВИТЬ") }
            })
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.route || (route.startsWith("collection/") && item == Screen.COLLECTIONS),
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Text(item.symbol) }, label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(message, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
            NavHost(navController = nav, startDestination = Screen.CATALOG.route, modifier = Modifier.weight(1f)) {
                composable(Screen.CATALOG.route) {
                    Column {
                        Box(Modifier.weight(1f)) {
                            CatalogDestination(
                                characters = characters, showHidden = prefs.showHidden,
                                onOpen = { vm.visit(it); nav.navigate("character/${it.id}") },
                                onPin = { vm.pin(it.id, !it.isPinned) },
                                onHide = { vm.hide(it.id, !it.isHidden) },
                                onTrack = { c, value -> vm.track(c.id, value) },
                            )
                        }
                        Button(onClick = vm::loadNextPage, modifier = Modifier.fillMaxWidth().padding(8.dp), enabled = !busy) {
                            Text("Загрузить следующую страницу")
                        }
                    }
                }
                composable(Screen.FAVORITES.route) {
                    FavoritesScreen(characters.filter { it.isPinned },
                        onOpen = { vm.visit(it); nav.navigate("character/${it.id}") },
                        onRemove = { vm.pin(it.id, false) })
                }
                composable(Screen.COLLECTIONS.route) {
                    CollectionsScreen(collections, vm, onCreate = { showCreate = true },
                        onOpen = { nav.navigate("collection/${it.id}") })
                }
                composable("collection/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backEntry ->
                    val id = backEntry.arguments?.getLong("id") ?: return@composable
                    val members by vm.collectionCharacters(id).collectAsStateWithLifecycle(initialValue = emptyList())
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        TextButton(onClick = { nav.popBackStack() }) { Text("← Все коллекции") }
                        Text(collections.firstOrNull { it.id == id }?.title ?: "Коллекция", style = MaterialTheme.typography.headlineSmall)
                        LazyColumn {
                            items(members, key = { it.id }) { c ->
                                CharacterRow(c, onClick = { vm.visit(c); nav.navigate("character/${c.id}") }, trailing = {
                                    TextButton(onClick = { vm.removeFromCollection(id, c.id) }) { Text("Убрать") }
                                })
                            }
                        }
                    }
                }
                composable(Screen.HISTORY.route) {
                    HistoryScreen(history, characters, onOpen = { vm.visit(it); nav.navigate("character/${it.id}") })
                }
                composable(Screen.SETTINGS.route) {
                    SettingsScreen(prefs.showHidden, vm::setShowHidden, prefs.preloadPages, vm::setPreloadPages,
                        prefs.darkTheme, vm::setDarkTheme, vm::refresh)
                }
                composable("character/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { backEntry ->
                    val id = backEntry.arguments?.getInt("id") ?: return@composable
                    val selected = characters.firstOrNull { it.id == id }
                    if (selected != null) {
                        CharacterDetailDialog(selected, collections, onDismiss = { nav.popBackStack() },
                            onTracking = { vm.track(id, it) }, onPin = { vm.pin(id, !selected.isPinned) },
                            onHide = { vm.hide(id, !selected.isHidden) }, onNote = { vm.note(id, it) },
                            onRate = { vm.rate(id, it) }, onAddToCollection = { vm.addToCollection(it, id) })
                    } else Text("Персонаж пока не загружен", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
    if (showCreate) CreateCollectionDialog(onDismiss = { showCreate = false },
        onCreate = { title, description, result -> vm.createCollection(title, description, result) })
}
