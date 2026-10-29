package com.aacheck.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aacheck.app.data.ExpenseUpsertRequest
import com.aacheck.app.data.ExpenseView
import com.aacheck.app.data.MemberBrief
import com.aacheck.app.data.toUserMessage
import com.aacheck.app.util.Category
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormSheet(
    travelId: Long,
    members: List<MemberBrief>,
    editing: ExpenseView?,
    isAuthor: Boolean,
    currentUserId: Long?,
    uploadImage: suspend (File) -> String,
    onSubmit: (ExpenseUpsertRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf(editing?.let { formatCents(it.amountCents) } ?: "") }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var category by remember { mutableStateOf(editing?.category ?: Category.OTHER.key) }
    var payerId by remember { mutableStateOf(editing?.payerId ?: currentUserId) }
    var participants by remember {
        mutableStateOf(editing?.shares?.map { it.userId }?.toSet() ?: members.map { it.userId }.toSet())
    }
    var imageUrls by remember { mutableStateOf(editing?.images ?: emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canEditNote = editing == null || isAuthor
    var showSourcePicker by remember { mutableStateOf(false) }
    var capturedFile by remember { mutableStateOf<File?>(null) }

    // 统一的上传处理：压缩 -> 上传 -> 汇入 imageUrls
    fun uploadAll(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        uploading = true
        error = null
        scope.launch {
            try {
                val result = uris.map { u ->
                    val tmp = com.aacheck.app.util.compressImage(ctx, u)
                        ?: error("图片无法读取（可能格式不支持，请换 JPG/PNG 图片）")
                    uploadImage(tmp)
                }
                imageUrls = (imageUrls + result).distinct().take(2)
            } catch (e: Exception) {
                error = "图片上传失败：${e.toUserMessage()}"
            } finally {
                uploading = false
            }
        }
    }

    val albumPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 2)
    ) { uris -> uploadAll(uris) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedFile?.let { uploadAll(listOf(android.net.Uri.fromFile(it))) }
        }
    }

    fun launchCamera() {
        val file = File(ctx.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
        capturedFile = file
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", file
        )
        cameraLauncher.launch(uri)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(if (editing == null) "记一笔" else "编辑记录", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("金额（元）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text("分类", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Category.entries.forEach { c ->
                    FilterChip(
                        selected = category == c.key,
                        onClick = { category = c.key },
                        label = { Text("${c.emoji} ${c.label}") },
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("谁付的钱", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                members.forEach { m ->
                    FilterChip(
                        selected = payerId == m.userId,
                        onClick = { payerId = m.userId },
                        label = { Text(m.nickname) },
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("参与分摊的人（可多选）", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                members.forEach { m ->
                    val sel = m.userId in participants
                    FilterChip(
                        selected = sel,
                        onClick = {
                            participants = if (sel) participants - m.userId else participants + m.userId
                        },
                        label = { Text(m.nickname) },
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                enabled = canEditNote,
                singleLine = false,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!canEditNote) {
                Text("备注只有作者本人可修改", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            // 图片
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("图片（最多 2 张）", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { showSourcePicker = true },
                    enabled = canEditNote && imageUrls.size < 2 && !uploading,
                ) { Text(if (imageUrls.isEmpty()) "添加图片" else "更换图片") }
            }
            if (imageUrls.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    imageUrls.forEach { url ->
                        AsyncImage(model = com.aacheck.app.core.ServerConfig.absoluteUrl(url), contentDescription = null, modifier = Modifier.size(width = 88.dp, height = 66.dp))
                    }
                }
            }
            if (uploading) Text("上传中…", style = MaterialTheme.typography.bodySmall)

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val cents = parseYuan(amountText)
                    if (cents == null || cents <= 0) { error = "请输入正确的金额"; return@Button }
                    if (participants.isEmpty()) { error = "请选择至少一个参与人"; return@Button }
                    if (payerId == null) { error = "请选择付款人"; return@Button }
                    onSubmit(
                        ExpenseUpsertRequest(
                            amountCents = cents,
                            category = category,
                            note = note,
                            payerId = payerId,
                            participantIds = participants.toList(),
                            imageUrls = imageUrls,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(if (editing == null) "保存" else "保存修改")
            }
        }
    }

    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("添加图片") },
            text = {
                Column {
                    TextButton(
                        onClick = { showSourcePicker = false; launchCamera() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("📷 拍照") }
                    TextButton(
                        onClick = {
                            showSourcePicker = false
                            albumPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("🖼 从相册选择") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSourcePicker = false }) { Text("取消") } },
        )
    }
}

fun parseYuan(s: String): Long? {
    val v = s.trim().toDoubleOrNull() ?: return null
    return Math.round(v * 100)
}

fun formatCents(cents: Long): String = "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
