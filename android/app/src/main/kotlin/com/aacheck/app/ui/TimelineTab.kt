package com.aacheck.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aacheck.app.data.ExpenseView
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.util.Category
import com.aacheck.app.util.centsToYuan
import com.aacheck.app.util.dayKey
import com.aacheck.app.util.formatDay
import com.aacheck.app.util.formatTime

@Composable
fun TimelineTab(
    expenses: List<ExpenseView>,
    members: List<MemberBrief>,
    filterMemberId: Long?,
    filterCategory: String?,
    onSetFilter: (Long?, String?) -> Unit,
    onOpenExpense: (Long) -> Unit,
) {
    val filtered = expenses.filter { e ->
        (filterMemberId == null || e.payerId == filterMemberId || e.shares.any { it.userId == filterMemberId }) &&
            (filterCategory == null || e.category == filterCategory)
    }
    val groups = filtered.groupBy { dayKey(it.createdAt) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        item { FilterRow(members, filterMemberId, filterCategory, onSetFilter) }
        if (filtered.isEmpty()) {
            item { Spacer(Modifier.height(40.dp)) }
            item { EmptyHint("还没有记录，点右下角记一笔") }
        }
        groups.forEach { (_, list) ->
            item {
                Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                    Text(
                        formatDay(list.first().createdAt),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            items(list, key = { it.id }) { e -> ExpenseCard(e, onOpenExpense) }
        }
    }
}

@Composable
private fun FilterRow(
    members: List<MemberBrief>,
    filterMemberId: Long?,
    filterCategory: String?,
    onSetFilter: (Long?, String?) -> Unit,
) {
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
        FilterChip(selected = filterMemberId == null, onClick = { onSetFilter(null, filterCategory) }, label = { Text("全部成员") })
        members.forEach { m ->
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = filterMemberId == m.userId, onClick = { onSetFilter(m.userId, filterCategory) }, label = { Text(m.nickname) })
        }
        Spacer(Modifier.width(16.dp))
        FilterChip(selected = filterCategory == null, onClick = { onSetFilter(filterMemberId, null) }, label = { Text("全部分类") })
        Category.entries.forEach { c ->
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = filterCategory == c.key,
                onClick = { onSetFilter(filterMemberId, if (filterCategory == c.key) null else c.key) },
                label = { Text("${c.emoji} ${c.label}") },
            )
        }
    }
}

@Composable
private fun ExpenseCard(e: ExpenseView, onClick: (Long) -> Unit) {
    val cat = Category.fromKey(e.category)
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick(e.id) }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Text(
                    cat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = cat.color,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTime(e.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.weight(1f))
                Text(centsToYuan(e.amountCents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (e.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(e.note, style = MaterialTheme.typography.bodyMedium)
            }
            // 缩略图
            if (e.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    e.images.take(2).forEach { url ->
                        AsyncImage(
                            model = com.aacheck.app.core.ServerConfig.absoluteUrl(url),
                            contentDescription = null,
                            modifier = Modifier.size(width = 72.dp, height = 54.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // 状态徽标
            e.openEdit?.let { edit ->
                when (edit.status) {
                    "DISPUTED" -> AssistChip(onClick = {}, label = { Text("有争议，请线下沟通") })
                    "PENDING" -> AssistChip(onClick = {}, label = { Text("待你确认修改") })
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
