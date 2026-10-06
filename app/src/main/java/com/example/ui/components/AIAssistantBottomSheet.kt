package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GraySubtle
import com.example.ui.theme.GrayText
import com.example.ui.theme.LocalAccentColor
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WireframeBorder
import com.example.util.ChatMessage
import com.example.util.GeminiAssistantHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantBottomSheet(
    initialPrompt: String = "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = LocalAccentColor.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf(initialPrompt) }
    var isStreaming by remember { mutableStateOf(false) }
    var currentStreamingResponse by remember { mutableStateOf("") }

    val messages = remember { mutableStateListOf<ChatMessage>() }

    fun sendPrompt(promptToSend: String) {
        val trimmed = promptToSend.trim()
        if (trimmed.isEmpty() || isStreaming) return

        messages.add(ChatMessage(role = "user", text = trimmed))
        inputText = ""
        isStreaming = true
        currentStreamingResponse = ""

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size)
        }

        coroutineScope.launch {
            try {
                GeminiAssistantHelper.streamChatResponse(
                    history = messages,
                    newPrompt = trimmed
                ).collect { chunk ->
                    currentStreamingResponse += chunk
                }

                if (currentStreamingResponse.isNotEmpty()) {
                    messages.add(ChatMessage(role = "model", text = currentStreamingResponse))
                    currentStreamingResponse = ""
                }
            } catch (e: Exception) {
                messages.add(ChatMessage(role = "model", text = "Помилка зв'язку: ${e.localizedMessage}"))
            } finally {
                isStreaming = false
                currentStreamingResponse = ""
                coroutineScope.launch {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
        }
    }

    // Auto-send if opened with a pre-filled prompt
    LaunchedEffect(Unit) {
        if (initialPrompt.isNotBlank()) {
            sendPrompt(initialPrompt)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureBlack,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor.copy(alpha = 0.4f))
            )
        },
        modifier = modifier
            .imePadding()
            .testTag("ai_assistant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.dp, accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Gemini",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GEMINI FLASH LITE",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Швидкий асистент лаунчера",
                            color = GraySubtle,
                            fontSize = 11.sp
                        )
                    }
                }

                Row {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = { messages.clear() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Clear Chat",
                                tint = GrayText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = GrayText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Messages Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(min = 120.dp, max = 380.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty() && !isStreaming) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Задайте будь-яке запитання або сформулюйте задачу...",
                                color = GraySubtle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                    }
                }

                items(messages) { msg ->
                    MessageBubble(
                        message = msg,
                        accentColor = accentColor,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Gemini", msg.text))
                            Toast.makeText(context, "Скопійовано", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Ongoing streaming chunk
                if (isStreaming && currentStreamingResponse.isNotEmpty()) {
                    item {
                        MessageBubble(
                            message = ChatMessage(role = "model", text = currentStreamingResponse),
                            accentColor = accentColor,
                            isStreaming = true,
                            onCopy = {}
                        )
                    }
                } else if (isStreaming) {
                    item {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Генерація...",
                                color = GraySubtle,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendPrompt(inputText) }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .testTag("ai_input_field"),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Запитати у Gemini Flash Lite...",
                                color = GraySubtle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Light
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = { sendPrompt(inputText) },
                    enabled = inputText.isNotBlank() && !isStreaming,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isStreaming) accentColor else GraySubtle,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    accentColor: androidx.compose.ui.graphics.Color,
    isStreaming: Boolean = false,
    onCopy: () -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isUser) 0.85f else 0.98f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isUser) DarkSurface else PureBlack)
                .border(
                    width = 1.dp,
                    color = if (isUser) accentColor.copy(alpha = 0.4f) else WireframeBorder,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "ВИ" else "GEMINI FLASH LITE",
                        color = if (isUser) GrayText else accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )

                    if (!isUser && !isStreaming) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = GraySubtle,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable(onClick = onCopy)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isUser) {
                    Text(
                        text = message.text,
                        color = PureWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal
                    )
                } else {
                    MarkdownText(
                        markdown = message.text,
                        accentColor = accentColor,
                        isStreaming = isStreaming
                    )
                }
            }
        }
    }
}
