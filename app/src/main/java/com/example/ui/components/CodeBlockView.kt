package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBackgroundDark
import com.example.ui.theme.CodeBorderDark

@Composable
fun CodeBlockView(
    code: String,
    language: String = "kotlin",
    onSendToColtef: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val horizontalScroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBackgroundDark)
            .testTag("code_block_${language}")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF27272A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase(),
                color = Color(0xFFA1A1AA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onSendToColtef != null) {
                    FilledTonalButton(
                        onClick = { onSendToColtef(language, code) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("send_to_coltef_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Send to Coltef",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send to Coltef", fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("copy_code_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code body with line numbering
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val lines = code.lines()
            val lineNumbersText = lines.indices.joinToString("\n") { (it + 1).toString().padStart(2, ' ') }

            Text(
                text = lineNumbersText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = Color(0xFF52525B),
                modifier = Modifier.padding(end = 12.dp)
            )

            Box(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                Text(
                    text = highlightCode(code),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// Light-weight high-performance syntax highlighting
private fun highlightCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        val keywords = setOf(
            "fun", "val", "var", "class", "interface", "package", "import", "return",
            "if", "else", "when", "for", "while", "data", "override", "def", "import",
            "from", "class", "async", "await", "function", "const", "let", "export"
        )
        val tokens = code.split(Regex("(?<=[^a-zA-Z0-9_])|(?=[^a-zA-Z0-9_])"))
        for (token in tokens) {
            when {
                token in keywords -> {
                    pushStyle(SpanStyle(color = Color(0xFFF472B6), fontWeight = FontWeight.SemiBold))
                    append(token)
                    pop()
                }
                token.startsWith("\"") || token.endsWith("\"") -> {
                    pushStyle(SpanStyle(color = Color(0xFF34D399)))
                    append(token)
                    pop()
                }
                token.startsWith("//") || token.startsWith("#") -> {
                    pushStyle(SpanStyle(color = Color(0xFF6B7280)))
                    append(token)
                    pop()
                }
                token.all { it.isDigit() } -> {
                    pushStyle(SpanStyle(color = Color(0xFFFBBF24)))
                    append(token)
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = Color(0xFFE4E4E7)))
                    append(token)
                    pop()
                }
            }
        }
    }
}
