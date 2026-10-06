package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OffWhite
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance Markdown renderer for Jetpack Compose.
 * Offloads parsing to Dispatchers.Default to ensure locked 120 FPS during live streaming.
 */
@Composable
fun MarkdownText(
    markdown: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false
) {
    var annotatedString by remember {
        mutableStateOf(quickFallbackParse(markdown, accentColor))
    }

    // Reactively parse markdown on Dispatchers.Default as streaming tokens arrive
    LaunchedEffect(markdown, accentColor) {
        val parsed = withContext(Dispatchers.Default) {
            parseMarkdownToAnnotatedString(markdown, accentColor)
        }
        annotatedString = parsed
    }

    SelectionContainer {
        Text(
            text = if (isStreaming) {
                buildAnnotatedString {
                    append(annotatedString)
                    append(" ▍")
                }
            } else {
                annotatedString
            },
            color = OffWhite,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            modifier = modifier
        )
    }
}

/**
 * Comprehensive parser supporting headings, code blocks, bullet lists, bold, italic, and inline code.
 */
suspend fun parseMarkdownToAnnotatedString(
    input: String,
    accentColor: Color
): AnnotatedString = withContext(Dispatchers.Default) {
    if (input.isBlank()) return@withContext AnnotatedString("")

    buildAnnotatedString {
        val lines = input.lines()
        var inCodeBlock = false
        var codeBlockContent = StringBuilder()

        for (i in lines.indices) {
            val rawLine = lines[i]
            val trimmedLine = rawLine.trim()

            // Handle fenced code block delimiters
            if (trimmedLine.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    val codeText = codeBlockContent.toString().trimEnd()
                    val start = length
                    append("\n$codeText\n")
                    addStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            background = DarkSurfaceVariant,
                            color = accentColor
                        ),
                        start,
                        length
                    )
                    codeBlockContent.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockContent.append(rawLine).append("\n")
                continue
            }

            // Headings
            if (trimmedLine.startsWith("#")) {
                val level = trimmedLine.takeWhile { it == '#' }.length
                val title = trimmedLine.removePrefix("#".repeat(level)).trim()
                val start = length

                if (length > 0) append("\n")

                append(title)
                val headingSize = when (level) {
                    1 -> 16.sp
                    2 -> 15.sp
                    else -> 14.sp
                }
                addStyle(
                    SpanStyle(
                        color = accentColor,
                        fontSize = headingSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    start,
                    length
                )
                if (i < lines.lastIndex) append("\n")
                continue
            }

            // Bullet lists
            val isBullet = trimmedLine.startsWith("- ") || trimmedLine.startsWith("* ")
            val lineToProcess = if (isBullet) {
                "•  " + trimmedLine.substring(2)
            } else if (trimmedLine.matches(Regex("^\\d+\\.\\s.*"))) {
                trimmedLine
            } else {
                rawLine
            }

            // Process inline formatting (bold, italic, code)
            appendFormattedLine(lineToProcess, accentColor)

            if (i < lines.lastIndex) {
                append("\n")
            }
        }

        // Unclosed code block during active stream
        if (inCodeBlock && codeBlockContent.isNotEmpty()) {
            val start = length
            append("\n${codeBlockContent.toString().trimEnd()}")
            addStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    background = DarkSurfaceVariant,
                    color = accentColor
                ),
                start,
                length
            )
        }
    }
}

/**
 * Parses inline formatting: **bold**, *italic*, and `inline code`
 */
private fun AnnotatedString.Builder.appendFormattedLine(line: String, accentColor: Color) {
    var currentIndex = 0
    val length = line.length

    while (currentIndex < length) {
        val boldIdx = line.indexOf("**", currentIndex)
        val codeIdx = line.indexOf("`", currentIndex)
        val italicIdx = line.indexOf("*", currentIndex)

        // Find nearest token
        val nextTokenIdx = listOf(
            boldIdx.takeIf { it != -1 },
            codeIdx.takeIf { it != -1 },
            italicIdx.takeIf { it != -1 && (boldIdx == -1 || it != boldIdx) }
        ).filterNotNull().minOrNull()

        if (nextTokenIdx == null) {
            append(line.substring(currentIndex))
            break
        }

        // Append text before the token
        if (nextTokenIdx > currentIndex) {
            append(line.substring(currentIndex, nextTokenIdx))
            currentIndex = nextTokenIdx
        }

        // Handle **bold**
        if (line.startsWith("**", currentIndex)) {
            val endIdx = line.indexOf("**", currentIndex + 2)
            if (endIdx != -1) {
                val boldText = line.substring(currentIndex + 2, endIdx)
                val start = this.length
                append(boldText)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PureWhite), start, this.length)
                currentIndex = endIdx + 2
                continue
            }
        }

        // Handle `inline code`
        if (line.startsWith("`", currentIndex)) {
            val endIdx = line.indexOf("`", currentIndex + 1)
            if (endIdx != -1) {
                val codeText = line.substring(currentIndex + 1, endIdx)
                val start = this.length
                append(" $codeText ")
                addStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        background = DarkSurfaceVariant,
                        color = accentColor
                    ),
                    start,
                    this.length
                )
                currentIndex = endIdx + 1
                continue
            }
        }

        // Handle *italic*
        if (line.startsWith("*", currentIndex) && !line.startsWith("**", currentIndex)) {
            val endIdx = line.indexOf("*", currentIndex + 1)
            if (endIdx != -1) {
                val italicText = line.substring(currentIndex + 1, endIdx)
                val start = this.length
                append(italicText)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = OffWhite), start, this.length)
                currentIndex = endIdx + 1
                continue
            }
        }

        // If no matching closing token found, append current character
        append(line[currentIndex])
        currentIndex++
    }
}

/**
 * Fast synchronous fallback while coroutine calculates complex AST
 */
private fun quickFallbackParse(text: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        append(text)
    }
}
