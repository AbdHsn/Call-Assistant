package com.callassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.callassistant.ui.theme.CallAssistantTheme

class SmsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NUMBER = "extra_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val number = intent.getStringExtra(EXTRA_NUMBER)
        setContent {
            CallAssistantTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(
                        text = number?.let { "Send message to $it" }
                            ?: "Set Call Assistant as default SMS app from system settings to handle messages here."
                    )
                }
            }
        }
    }
}
