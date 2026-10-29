package com.realtimechat.feature.conversations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun NewChatDialog(
    onDismiss: () -> Unit,
    onStartChat: (String) -> Unit
) {
    var recipientInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New 1:1 Chat") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter the User UID or email of the person you want to chat with:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = recipientInput,
                    onValueChange = {
                        recipientInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("Recipient UID / Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )
                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = err, color = Color.Red)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = recipientInput.trim()
                    if (trimmed.isEmpty()) {
                        errorMessage = "Recipient ID cannot be empty"
                    } else {
                        onStartChat(trimmed)
                    }
                }
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
