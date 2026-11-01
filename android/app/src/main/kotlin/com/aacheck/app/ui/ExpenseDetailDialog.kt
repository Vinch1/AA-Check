package com.aacheck.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aacheck.app.core.ServerConfig
import com.aacheck.app.data.ExpenseView
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.util.Category
import com.aacheck.app.util.centsToYuan
import com.aacheck.app.util.tint
import com.aacheck.app.util.formatTime

@Composable
fun ExpenseDetailDialog(
    expense: ExpenseView,
    members: List<MemberBrief>,
    currentUserId: Long?,
    isEnded: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWithdraw: () -> Unit,
    onDismiss: () -> Unit,
) {
    val nameOf = members.associate { it.userId to it.nickname }
    val cat = Category.fromKey(expense.category)
    val isAuthor = expense.authorId == currentUserId
    val openEdit = expense.openEdit
    val isDark = isSystemInDarkTheme()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录详情") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("${cat.emoji} ${cat.label}", color = cat.tint(isDark), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("金额：${centsToYuan(expense.amountCents)}", style = MaterialTheme.typography.titleMedium)
                Text("付款人：${nameOf[expense.payerId] ?: "?"}")
                Text("作者：${nameOf[expense.authorId] ?: "?"} · ${formatTime(expense.createdAt)}")
                Text("分摊：", fontWeight = FontWeight.Bold)
                expense.shares.forEach { s ->
                    Text("  ${nameOf[s.userId] ?: "?"}  ${centsToYuan(s.shareCents)}")
                }
                if (expense.note.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("备注：${expense.note}")
                }
                if (expense.images.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        expense.images.forEach { url ->
                            AsyncImage(model = ServerConfig.absoluteUrl(url), contentDescription = null, modifier = Modifier.size(width = 110.dp, height = 82.dp))
                        }
                    }
                }
                if (openEdit != null) {
                    Spacer(Modifier.height(12.dp))
                    when (openEdit.status) {
                        "PENDING" -> {
                            if (isAuthor) Text("有修改待你确认", color = MaterialTheme.colorScheme.primary)
                            else Text("修改待作者确认", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        "DISPUTED" -> Text("该记录有争议，请与修改人线下沟通", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row {
                if (openEdit != null && openEdit.editorId == currentUserId && !isEnded) {
                    TextButton(onClick = onWithdraw) { Text("撤回修改") }
                }
                if (isAuthor && !isEnded) {
                    TextButton(onClick = onDelete) { Text("删除") }
                    TextButton(onClick = onEdit) { Text("编辑") }
                } else if (!isEnded) {
                    TextButton(onClick = onEdit) { Text("编辑金额/分摊") }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}
