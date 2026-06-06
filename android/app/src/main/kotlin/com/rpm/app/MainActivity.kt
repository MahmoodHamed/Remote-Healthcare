package com.rpm.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rpm.app.ui.navigation.RpmNavHost
import com.rpm.app.ui.theme.RpmTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingConversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingConversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        enableEdgeToEdge()
        setContent {
            RpmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RpmNavHost(
                        initialConversationId = pendingConversationId,
                        onDeepLinkConsumed = { pendingConversationId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingConversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversationId"
    }
}
