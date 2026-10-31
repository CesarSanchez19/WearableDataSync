package com.example.wearabledatasync

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wearabledatasync.ui.theme.NomadInk
import com.example.wearabledatasync.ui.theme.NomadSand
import com.example.wearabledatasync.ui.theme.NomadSandSoft
import com.example.wearabledatasync.ui.theme.WearableDataSyncTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

// Student name shown in the lower part of the interface
private const val STUDENT_NAME = "Sanchez Trejo Cesar David"

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private val tag = "PhoneActivity"
    private val count = mutableIntStateOf(COUNTER_MAX)
    private val lastCommand = mutableStateOf<String?>(null) // null = no signal received yet
    private lateinit var messageClient: MessageClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light background: keep the system bar icons dark even if the phone is in dark mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        messageClient = Wearable.getMessageClient(this)

        setContent {
            WearableDataSyncTheme {
                WearApp(count.intValue, lastCommand.value)
            }
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == MESSAGE_PATH_RESET) {
            Log.d(tag, "Watch sent RESET signal. Node ID: ${messageEvent.sourceNodeId}")
            runOnUiThread {
                count.intValue = 0
                lastCommand.value = "RESET (counter set to 0)"
            }
            sendConfirmation(messageEvent.sourceNodeId)
        }
        if (messageEvent.path == MESSAGE_PATH_INCREMENT) {
            changeCounter(COUNTER_STEP, messageEvent.sourceNodeId, "+$COUNTER_STEP")
        }
        if (messageEvent.path == MESSAGE_PATH_DECREMENT) {
            changeCounter(-COUNTER_STEP, messageEvent.sourceNodeId, "-$COUNTER_STEP")
        }
    }

    // Applies a +/- step, keeps the value between COUNTER_MIN and COUNTER_MAX and confirms to the watch
    private fun changeCounter(delta: Int, targetNodeId: String, label: String) {
        Log.d(tag, "Watch sent $label signal. Node ID: $targetNodeId")
        runOnUiThread {
            val newValue = (count.intValue + delta).coerceIn(COUNTER_MIN, COUNTER_MAX)
            count.intValue = newValue
            lastCommand.value = "$label (counter is $newValue${counterLimitLabel(newValue)})"
            sendConfirmation(targetNodeId, "$CONFIRM_COUNT_PREFIX$newValue")
        }
    }

    private fun sendConfirmation(targetNodeId: String, reply: String = "RESET_OK") {
        val message = reply.toByteArray(Charsets.UTF_8)
        messageClient.sendMessage(targetNodeId, MESSAGE_PATH_CONFIRM, message)
            .addOnSuccessListener {
                Log.d(tag, "Confirmation message SENT back to watch: $reply")
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Failed to send confirmation: ${e.message}")
            }
    }
}

@Composable
fun WearApp(count: Int, lastCommand: String? = null) {
    // Surface gives the text a proper background/content color
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PHONE APPLICATION",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Counter card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, NomadSandSoft),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Count:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$count",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { count / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(50)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = NomadSandSoft
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "0", style = MaterialTheme.typography.labelMedium)
                            Text(text = "100", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (lastCommand == null) {
                    // Skeleton state: nothing received from the watch yet
                    Text(
                        text = "Awaiting Watch RESET signal...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonBar(modifier = Modifier.fillMaxWidth(0.6f))
                } else {
                    Text(
                        text = "Last command: $lastCommand",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Student name pinned to the lower part of the screen
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = STUDENT_NAME,
                    style = MaterialTheme.typography.titleMedium,
                    color = NomadInk,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// Pulsing placeholder bar shown while there is no data yet (offline-first skeleton)
@Composable
private fun SkeletonBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )
    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(50))
            .background(NomadSand.copy(alpha = alpha))
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WearableDataSyncTheme {
        WearApp(100)
    }
}