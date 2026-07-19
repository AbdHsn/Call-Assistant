package com.callassistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.callassistant.ui.MainApp
import com.callassistant.ui.MainViewModelFactory
import com.callassistant.ui.theme.CallAssistantTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.CALL_PHONE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // no extra
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // ViewModel observes permission state live
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallAssistantTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        factory = MainViewModelFactory(application),
                        requestPermissions = { requestPermissionLauncher.launch(requiredPermissions) },
                        hasPermission = { permission ->
                            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                        }
                    )
                }
            }
        }
    }
}
