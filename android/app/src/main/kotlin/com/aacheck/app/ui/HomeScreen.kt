package com.aacheck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aacheck.app.core.ThemeMode
import com.aacheck.app.ui.theme.LocalThemeController

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenTravel: (Long) -> Unit, onLogout: () -> Unit) {
    val state by vm.state.collectAsState()
    val controller = LocalThemeController.current
    val isDark = when (controller.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var themeMenuOpen by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val active = state.travels.filter { it.status == "ACTIVE" }
            val ended = state.travels.filter { it.status == "ENDED" }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Row(
                        Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "我的旅行",
                                style = MaterialTheme.typography.displaySmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "与好友同行，把每一笔花销都记得体面。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { themeMenuOpen = true }) {
                            Icon(
                                if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "切换主题",
                            )
                        }
                        IconButton(onClick = { onLogout() }) {
                            Icon(Icons.Default.Logout, contentDescription = "退出登录")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                item { Text("进行中", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (active.isEmpty()) item { EmptyHint("还没有旅行，点右下角新建") }
                items(active, key = { it.id }) { t -> TravelCard(t, onOpenTravel) }

                if (ended.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { Text("已结束", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    items(ended, key = { it.id }) { t -> TravelCard(t, onOpenTravel) }
                }
            }

            // 主操作按钮（展开后显示「新建旅行 / 用邀请码加入」）
            Box(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { fabExpanded = false },
                    )
                }

                Column(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AnimatedVisibility(visible = fabExpanded) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            SpeedDialAction(
                                icon = Icons.Default.GroupAdd,
                                label = "用邀请码加入",
                                onClick = {
                                    fabExpanded = false
                                    showJoin = true
                                },
                            )
                            SpeedDialAction(
                                icon = Icons.Default.Add,
                                label = "新建旅行",
                                onClick = {
                                    fabExpanded = false
                                    showCreate = true
                                },
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (fabExpanded) "收起" else "新建或加入",
                        )
                    }
                }
            }
        }
    }

    DropdownMenu(expanded = themeMenuOpen, onDismissRequest = { themeMenuOpen = false }) {
        ThemeMode.entries.forEach { m ->
            DropdownMenuItem(
                text = {
                    Text(
                        when (m) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                        },
                    )
                },
                trailingIcon = if (controller.mode == m) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
                onClick = {
                    controller.select(m)
                    themeMenuOpen = false
                },
            )
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
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
private fun SpeedDialAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.width(12.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}
