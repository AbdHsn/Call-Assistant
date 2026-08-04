package com.callassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.screens.MessageThreadScreen
import com.callassistant.ui.theme.CallAssistantTheme

class SmsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NUMBER = "extra_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = resolveNumber() ?: return finish()

        val viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

        setContent {
            CallAssistantTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MessageThreadScreen(
                        number = number,
                        viewModel = viewModel,
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
