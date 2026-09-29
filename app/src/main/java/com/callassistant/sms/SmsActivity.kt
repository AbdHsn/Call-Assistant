package com.callassistant.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.callassistant.ui.MessagesViewModel
import com.callassistant.ui.messageai.MessageAiViewModel
import com.callassistant.ui.screens.MessageThreadScreen
import com.callassistant.ui.theme.CallAssistantTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmsActivity : ComponentActivity() {

    private val viewModel: MessagesViewModel by viewModels()
    private val aiViewModel: MessageAiViewModel by viewModels()

    companion object {
        const val EXTRA_NUMBER = "extra_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = resolveNumber() ?: return finish()

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        setContent {
            CallAssistantTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MessageThreadScreen(
                        number = number,
                        viewModel = viewModel,
                        aiViewModel = aiViewModel,
                        hasPermission = { permission ->
                            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                        },
                        requestPermissions = {
                            permissionLauncher.launch(Manifest.permission.SEND_SMS)
                        },
                        onBack = { finish() }
                    )
                }
            }
        }
    }

    private fun resolveNumber(): String? {
        intent?.data?.let { uri ->
            if (uri.scheme in listOf("sms", "smsto", "mms", "mmsto")) {
                return uri.schemeSpecificPart
            }
        }
        return intent.getStringExtra(EXTRA_NUMBER)
    }
}
