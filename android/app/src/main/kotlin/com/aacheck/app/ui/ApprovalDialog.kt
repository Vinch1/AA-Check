package com.aacheck.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aacheck.app.data.AccountSnapshot
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.data.PendingApproval
import com.aacheck.app.util.Category
import com.aacheck.app.util.centsToYuan

/** 原作者视角：对比原始 vs 当前，同意 / 不同意 */
@Composable
fun ApprovalDialog(
    approval: PendingApproval,
    members: List<MemberBrief>,
    onAgree: () -> Unit,
    onDisagree: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nameOf = members.associate { it.userId to it.nickname }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("有人改了你的记录") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("编辑人：${nameOf[approval.editorId] ?: "?"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                SnapshotRow("原始", approval.before, nameOf)
                Spacer(Modifier.height(8.dp))
                SnapshotRow("修改后", approval.after, nameOf, highlight = true)
                Spacer(Modifier.height(8.dp))
                Text("不同意请与修改人线下沟通，修改人可撤回恢复原状。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("同意") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDisagree) { Text("不同意") }
                TextButton(onClick = onDismiss) { Text("稍后") }
            }
        },
    )
}

@Composable
private fun SnapshotRow(label: String, s: AccountSnapshot, nameOf: Map<Long, String>, highlight: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("金额：${centsToYuan(s.amountCents)}")
        Text("分类：${Category.fromKey(s.category).emoji} ${Category.fromKey(s.category).label}")
        Text("付款人：${nameOf[s.payerId] ?: "?"}")
        Text("分摊：" + s.shares.joinToString("、") { "${nameOf[it.userId] ?: "?"} ${centsToYuan(it.shareCents)}" })
    }
}
