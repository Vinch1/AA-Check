package com.aacheck.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenTravel: (Long) -> Unit, onLogout: () -> Unit) {
    val state by vm.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的旅行") },
                actions = {
                    IconButton(onClick = { onLogout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "退出登录")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "新建旅行")
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val active = state.travels.filter { it.status == "ACTIVE" }
            val ended = state.travels.filter { it.status == "ENDED" }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("进行中", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline) }
                if (active.isEmpty()) item { EmptyHint("还没有旅行，点右下角新建") }
                items(active, key = { it.id }) { t -> TravelCard(t, onOpenTravel) }

                if (ended.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { Text("已结束", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline) }
                    items(ended, key = { it.id }) { t -> TravelCard(t, onOpenTravel) }
                }
            }
            TextButton(onClick = { showJoin = true }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("用邀请码加入")
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("新建旅行") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("旅行名称") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(note, { note = it }, label = { Text("备注（可选）") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.create(name, note.ifEmpty { null }) { onOpenTravel(it) }
                    showCreate = false
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("取消") } },
        )
    }

    if (showJoin) {
        AlertDialog(
            onDismissRequest = { showJoin = false },
            title = { Text("加入旅行") },
            text = {
                OutlinedTextField(code, { code = it }, label = { Text("6 位邀请码") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.join(code) { onOpenTravel(it) }
                    showJoin = false
                }) { Text("加入") }
            },
            dismissButton = { TextButton(onClick = { showJoin = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun TravelCard(t: com.aacheck.app.data.TravelDto, onClick: (Long) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick(t.id) }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (t.status == "ENDED") {
                    AssistChip(onClick = {}, label = { Text("已结束") })
                }
            }
            t.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                t.members.take(6).forEach { m ->
                    Avatar(m.nickname, 24.dp)
                    Spacer(Modifier.width(4.dp))
                }
                Spacer(Modifier.weight(1f))
                if (t.amCreator) Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}
