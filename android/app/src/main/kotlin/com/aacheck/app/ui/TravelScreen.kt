package com.aacheck.app.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aacheck.app.data.ExpenseUpsertRequest
import com.aacheck.app.data.ExpenseView
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.util.makeQr
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelScreen(vm: TravelViewModel, travelId: Long, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseView?>(null) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var showInvite by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val travel = state.travel
    val members = travel?.members ?: emptyList()
    val currentUserId = vm.currentUserId
    val isEnded = travel?.status == "ENDED"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(travel?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    if (travel?.amCreator == true) {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("邀请码 / 二维码") }, onClick = { menuOpen = false; showInvite = true })
                            DropdownMenuItem(text = { Text("修改名称") }, onClick = { menuOpen = false; showRename = true })
                            if (!isEnded) DropdownMenuItem(text = { Text("结束旅行") }, onClick = {
                                menuOpen = false
                                vm.endTravel(travelId) { }
                            })
                            DropdownMenuItem(text = { Text("删除旅程", color = MaterialTheme.colorScheme.error) }, onClick = {
                                menuOpen = false
                                showDelete = true
                            })
                        }
                    } else {
                        IconButton(onClick = { showInvite = true }) { Text("邀请", style = MaterialTheme.typography.labelLarge) }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isEnded && tab == 0) {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("时间线") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("结算") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("成员") })
            }
            when (tab) {
                0 -> TimelineTab(
                    expenses = state.expenses,
                    members = members,
                    filterMemberId = state.filterMemberId,
                    filterCategory = state.filterCategory,
                    onSetFilter = { m, c -> vm.setFilter(m, c) },
                    onOpenExpense = { detailId = it },
                )
                1 -> SettlementTab(settlement = state.settlement, members = members)
                2 -> MembersTab(travel, members, isEnded)
            }
        }
    }

    // 待追认弹窗（作者视角）
    state.approvals.firstOrNull()?.let { approval ->
        ApprovalDialog(
            approval = approval,
            members = members,
            onAgree = { vm.approve(approval.expenseId, travelId) },
            onDisagree = { vm.disagree(approval.expenseId, travelId) },
            onDismiss = {},
        )
    }

    // 详情
    detailId?.let { id ->
        state.expenses.firstOrNull { it.id == id }?.let { e ->
            ExpenseDetailDialog(
                expense = e,
                members = members,
                currentUserId = currentUserId,
                isEnded = isEnded,
                onEdit = { editing = e; showForm = true; detailId = null },
                onDelete = { vm.deleteExpense(e.id, travelId); detailId = null },
                onWithdraw = { vm.withdraw(e.id, travelId); detailId = null },
                onDismiss = { detailId = null },
            )
        }
    }

    // 记一笔 / 编辑
    if (showForm) {
        val edit = editing
        ExpenseFormSheet(
            travelId = travelId,
            members = members,
            editing = edit,
            isAuthor = edit == null || edit.authorId == currentUserId,
            currentUserId = currentUserId,
            uploadImage = { f -> vm.repo.uploadImage(f) },
            onSubmit = { req ->
                if (edit == null) {
                    vm.createExpense(travelId, req) { showForm = false }
                } else {
                    vm.updateExpense(edit.id, travelId, req) { showForm = false }
                }
            },
            onDismiss = { showForm = false },
        )
    }

    // 邀请码
    if (showInvite && travel != null) {
        InviteDialog(travel, vm, onDismiss = { showInvite = false })
    }

    // 改名
    if (showRename && travel != null) {
        RenameDialog(travelName = travel.name, onSave = { name ->
            vm.renameTravel(travelId, name)
            showRename = false
        }, onDismiss = { showRename = false })
    }

    // 删除旅程
    if (showDelete && travel != null) {
        DeleteTravelDialog(
            onConfirm = {
                showDelete = false
                vm.deleteTravel(travelId) { onBack() }
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun MembersTab(travel: com.aacheck.app.data.TravelDto?, members: List<MemberBrief>, isEnded: Boolean) {
    if (travel == null) { EmptyHint("加载中…"); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("邀请码：${travel.inviteCode}", style = MaterialTheme.typography.titleMedium)
                    Text("状态：${if (isEnded) "已结束" else "进行中"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        items(members) { m ->
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(m.nickname, 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(m.nickname, style = MaterialTheme.typography.bodyLarge)
                    if (m.isCreator) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text("创建者") })
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteDialog(travel: com.aacheck.app.data.TravelDto, vm: TravelViewModel, onDismiss: () -> Unit) {
    val qr = remember(travel.inviteCode) { makeQr(travel.inviteCode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请成员") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("邀请码：${travel.inviteCode}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                qr?.let { Image(it.asImageBitmap(), contentDescription = "二维码", modifier = Modifier.size(220.dp)) }
            }
        },
        confirmButton = {
            if (travel.amCreator) {
                TextButton(onClick = { vm.regenerateCode(travel.id) }) { Text("重新生成") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun RenameDialog(travelName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(travelName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改旅行名称") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("名称") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DeleteTravelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除旅程") },
        text = { Text("将一并删除该旅程下的所有记账记录与图片，且不可恢复。确定删除？") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
