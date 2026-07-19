package com.callassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.callassistant.ui.theme.CallAssistantTheme

class SmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallAssistantTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(text = "Set Call Assistant as default SMS app from system settings to handle messages here.")
                }
            }
        }
    }
}
