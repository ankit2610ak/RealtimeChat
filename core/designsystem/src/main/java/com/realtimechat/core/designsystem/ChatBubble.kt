package com.realtimechat.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}

@Composable
fun ChatBubble(
    text: String,
    isFromMe: Boolean,
    timestamp: Long,
    modifier: Modifier = Modifier,
    status: MessageStatus = MessageStatus.SENT,
    imageBitmap: Bitmap? = null,
    uploadProgress: Float? = null
) {
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isFromMe) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isFromMe) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromMe) 16.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 16.dp
            ),
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (imageBitmap != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "Encrypted image attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        if (uploadProgress != null && uploadProgress < 1.0f) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (text.isNotEmpty()) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (status) {
                            MessageStatus.SENDING -> CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp
                            )
                            MessageStatus.SENT -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                modifier = Modifier.size(14.dp),
                                tint = contentColor.copy(alpha = 0.7f)
                            )
                            MessageStatus.DELIVERED, MessageStatus.READ -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Delivered",
                                modifier = Modifier.size(14.dp),
                                tint = if (status == MessageStatus.READ) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSenderBubble() {
    MaterialTheme {
        ChatBubble(
            text = "Hey! Is this message end-to-end encrypted?",
            isFromMe = true,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.READ
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReceiverBubble() {
    MaterialTheme {
        ChatBubble(
            text = "Yes, strictly using AES-256-GCM!",
            isFromMe = false,
            timestamp = System.currentTimeMillis()
        )
    }
}
