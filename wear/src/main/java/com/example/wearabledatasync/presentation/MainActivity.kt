/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.wearabledatasync.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.example.wearabledatasync.CONFIRM_COUNT_PREFIX
import com.example.wearabledatasync.COUNTER_STEP
import com.example.wearabledatasync.MESSAGE_PATH_CONFIRM
import com.example.wearabledatasync.MESSAGE_PATH_DECREMENT
import com.example.wearabledatasync.MESSAGE_PATH_INCREMENT
import com.example.wearabledatasync.MESSAGE_PATH_RESET
import com.example.wearabledatasync.counterLimitLabel
import com.example.wearabledatasync.presentation.theme.NomadBackground
import com.example.wearabledatasync.presentation.theme.NomadError
import com.example.wearabledatasync.presentation.theme.NomadForest
import com.example.wearabledatasync.presentation.theme.NomadInk
import com.example.wearabledatasync.presentation.theme.NomadOcean
import com.example.wearabledatasync.presentation.theme.NomadSand
import com.example.wearabledatasync.presentation.theme.NomadWarning
import com.example.wearabledatasync.presentation.theme.NomadWhite
import com.example.wearabledatasync.presentation.theme.WearableDataSyncTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Student name shown in the lower part of the interface
private const val STUDENT_NAME = "Sanchez Trejo Cesar David "

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val tag = "WatchActivity"
    private val statusMessage = mutableStateOf("Tap to send RESET signal")
    private var awaitingConfirmation = false // true while a command is waiting for the phone's answer
    private lateinit var messageClient: MessageClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageClient = Wearable.getMessageClient(this)

        setContent {
            WearApp(
                statusMessage = statusMessage,
                onResetClicked = { sendResetMessage() },
                onIncrementClicked = { sendIncrementMessage() },
                onDecrementClicked = { sendDecrementMessage() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        messageClient.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        messageClient.removeListener(this)
    }

    private fun sendResetMessage() = sendCommandMessage(MESSAGE_PATH_RESET, "RESET")

    private fun sendIncrementMessage() = sendCommandMessage(MESSAGE_PATH_INCREMENT, "+$COUNTER_STEP")

    private fun sendDecrementMessage() = sendCommandMessage(MESSAGE_PATH_DECREMENT, "-$COUNTER_STEP")

    // Sends any command path to the connected phone(s) and reports the progress in the status text
    private fun sendCommandMessage(path: String, label: String) {
        lifecycleScope.launch {
            awaitingConfirmation = true
            statusMessage.value = "Searching for phone..."
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                if (nodes.isEmpty()) {
                    awaitingConfirmation = false
                    statusMessage.value = "Error: No connected phone found!"
                    return@launch
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, null)
                        .addOnSuccessListener {
                            Log.d(tag, "$label signal SENT to phone: ${node.displayName}")
                            // The phone may answer before this callback runs; never overwrite SUCCESS
                            if (awaitingConfirmation) {
                                statusMessage.value = "$label signal SENT. Waiting for confirmation..."
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(tag, "Failed to send $label to ${node.displayName}: ${e.message}")
                            awaitingConfirmation = false
                            statusMessage.value = "Send Failed: ${e.message}"
                        }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error getting nodes: ${e.message}")
                awaitingConfirmation = false
                statusMessage.value = "Connection Error"
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == MESSAGE_PATH_CONFIRM) {
            val receivedMsg = String(messageEvent.data, Charsets.UTF_8)
            runOnUiThread {
                Log.d(tag, "Received confirmation: $receivedMsg")
                awaitingConfirmation = false
                statusMessage.value = if (receivedMsg.startsWith(CONFIRM_COUNT_PREFIX)) {
                    // Answer to +5 / -5: the phone sends its new counter value
                    val phoneCount = receivedMsg.removePrefix(CONFIRM_COUNT_PREFIX)
                    val limit = phoneCount.toIntOrNull()?.let { counterLimitLabel(it) } ?: ""
                    "SUCCESS: Phone count is now $phoneCount$limit"
                } else {
                    // Answer to RESET
                    "SUCCESS: Phone Confirmed Reset ($receivedMsg)!"
                }
            }
        }
    }
}

// Text-based state plus color, so the meaning never depends on color alone
private fun statusColor(message: String): Color = when {
    message.startsWith("SUCCESS") -> NomadForest
    message.startsWith("Error") ||
            message.startsWith("Send Failed") ||
            message.startsWith("Connection Error") -> NomadError
    message.contains("Waiting") || message.startsWith("Searching") -> NomadWarning
    else -> NomadOcean
}

@Composable
fun WearApp(
    statusMessage: State<String>,
    onResetClicked: () -> Unit,
    onIncrementClicked: () -> Unit,
    onDecrementClicked: () -> Unit
) {
    WearableDataSyncTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NomadBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "WATCH APPLICATION",
                    color = NomadOcean,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Decrement / increment row
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onDecrementClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NomadOcean,
                            contentColor = NomadWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "-$COUNTER_STEP",
                            color = NomadWhite,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Button(
                        onClick = onIncrementClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NomadForest,
                            contentColor = NomadWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "+$COUNTER_STEP",
                            color = NomadWhite,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Primary action (Sand)
                Button(
                    onClick = onResetClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NomadSand,
                        contentColor = NomadInk
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "SEND RESET",
                        color = NomadInk,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusMessage.value,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    color = statusColor(statusMessage.value),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )

                // Pushes the name to the lower part of the screen
                Spacer(modifier = Modifier.weight(1f))

                // Narrow width so the full name wraps into two lines inside the round screen
                Text(
                    text = STUDENT_NAME,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = NomadInk,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }
        }
    }
}