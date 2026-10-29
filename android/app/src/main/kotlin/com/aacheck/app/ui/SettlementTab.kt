package com.aacheck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.data.SettlementDto
import com.aacheck.app.util.Category
import com.aacheck.app.util.centsToYuan

@Composable
fun SettlementTab(
    settlement: SettlementDto?,
    members: List<MemberBrief>,
) {
    if (settlement == null) {
        EmptyHint("加载中…")
        return
    }
    val nameOf = members.associate { it.userId to it.nickname }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("总支出 ${centsToYuan(settlement.totalCents)} · ${settlement.expenseCount} 笔",
                        style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        item { Text("每人净额", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(settlement.members) { m ->
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(m.nickname, 32.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(m.nickname, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    Text(
                        netText(m.netCents),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (m.netCents >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
        item { Text("给钱方案（谁转给谁）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (settlement.transfers.isEmpty()) {
            item { EmptyHint("已经平账，无需转账") }
        }
        items(settlement.transfers) { t ->
            Card {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(nameOf[t.fromUserId] ?: "?", fontWeight = FontWeight.Bold)
                    Text("  →  ", color = MaterialTheme.colorScheme.outline)
                    Text(nameOf[t.toUserId] ?: "?", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(centsToYuan(t.amountCents), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
        item { Text("每人的金额组成（垫付 / 分摊）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(settlement.breakdown) { b ->
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(b.nickname, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    b.categories.forEach { c ->
                        val cat = Category.fromKey(c.category)
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${cat.emoji} ${cat.label}", modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("垫付 ${centsToYuan(c.paidCents)}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                                Text("分摊 ${centsToYuan(c.owedCents)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun netText(net: Long): String =
    when {
        net > 0 -> "应收 ${centsToYuan(net)}"
        net < 0 -> "应付 ${centsToYuan(-net)}"
        else -> "已平账"
    }
