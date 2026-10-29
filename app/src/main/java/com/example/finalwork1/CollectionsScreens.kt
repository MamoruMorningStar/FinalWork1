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
internal fun CollectionsScreen(
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
                        var showEdit by remember { mutableStateOf(false) }
                        TextButton(onClick = { showEdit = true }) { Text("Изменить") }
                        if (showEdit) EditCollectionDialog(c, onDismiss = { showEdit = false },
                            onSave = { title, description, done -> vm.editCollection(c.id, title, description, done) })
                        var confirmDelete by remember { mutableStateOf(false) }
                        TextButton(onClick = { confirmDelete = true }) { Text("Удалить") }
                        if (confirmDelete) AlertDialog(
                            onDismissRequest = { confirmDelete = false },
                            title = { Text("Удалить коллекцию?") },
                            text = { Text("Коллекция и её состав будут удалены.") },
                            confirmButton = { TextButton(onClick = { vm.deleteCollection(c.id); confirmDelete = false }) { Text("Удалить") } },
                            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CreateCollectionDialog(onDismiss: () -> Unit, onCreate: (String, String, (Boolean) -> Unit) -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("Новая коллекция") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it; error = false }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание") }, minLines = 2)
                if (error) Text("Коллекция с таким именем уже существует", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                submitting = true
                onCreate(title, description) { success ->
                    submitting = false
                    if (success) onDismiss() else error = true
                }
            }, enabled = title.isNotBlank() && !submitting) { Text(if (submitting) "Создание..." else "Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !submitting) { Text("Отмена") } },
    )
}


@Composable
internal fun EditCollectionDialog(collection: UserCollectionEntity, onDismiss: () -> Unit,
    onSave: (String, String, (Boolean) -> Unit) -> Unit) {
    var title by remember(collection.id) { mutableStateOf(collection.title) }
    var description by remember(collection.id) { mutableStateOf(collection.description) }
    var error by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, title = { Text("Изменить коллекцию") }, text = {
        Column {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") })
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Описание") })
            if (error) Text("Название занято или не удалось сохранить", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { TextButton(onClick = { saving = true; onSave(title, description) { ok -> saving = false; if (ok) onDismiss() else error = true } }, enabled = title.isNotBlank() && !saving) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Отмена") } })
}
