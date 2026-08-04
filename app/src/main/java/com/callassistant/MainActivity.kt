package com.callassistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.callassistant.service.CallRecordingAccessibilityService
import com.callassistant.ui.SetupScreen
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callassistant.ui.MainApp
import com.callassistant.ui.MainViewModel
import com.callassistant.ui.MainViewModelFactory
import com.callassistant.ui.theme.CallAssistantTheme

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val setupPermissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // ViewModel observes permission state live
    }

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    private val defaultSmsAppLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    private val callScreeningRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    private val batteryOptLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user may have ignored/allowed battery opt */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        promptDefaultDialerIfNeeded()
        promptDefaultSmsAppIfNeeded()
        promptCallScreeningRoleIfNeeded()
        setContent {
            val viewModel = viewModel<MainViewModel>(factory = MainViewModelFactory(application))
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            var isReady by remember { mutableStateOf(isSetupCompleted()) }

            CallAssistantTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isReady) {
                        MainApp(
                            factory = MainViewModelFactory(application),
                            requestPermissions = { requestPermissionLauncher.launch(requiredPermissions) },
                            hasPermission = { permission ->
                                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                            },
                            isAccessibilityEnabled = ::isAccessibilityServiceEnabled,
                            isBatteryIgnored = ::isIgnoringBatteryOptimizations,
                            requestBatteryOpt = ::requestBatteryOptimizationExemption,
                            openBatterySettings = ::openBatteryOptimizationSettings,
                            openAppSettings = ::openApplicationDetailsSettings,
                            openAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                        )
                    } else {
                        SetupScreen(
                            hasPermission = { permission ->
                                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                            },
                            onRequestPermissions = { requestPermissionLauncher.launch(setupPermissions) },
                            onOpenAppSettings = { openApplicationDetailsSettings() },
                            isAccessibilityEnabled = { isAccessibilityServiceEnabled() },
                            onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            isBatteryOptimizationIgnored = { isIgnoringBatteryOptimizations() },
                            onRequestBatteryOpt = { requestBatteryOptimizationExemption() },
                            onOpenBatterySettings = { openBatteryOptimizationSettings() },
                            onContinue = { setSetupCompleted(); isReady = true }
                        )
                    }
                }
            }
        }
    }

    private fun promptDefaultDialerIfNeeded() {
        val telecom = getSystemService(TelecomManager::class.java) ?: return
        if (telecom.defaultDialerPackage != packageName) {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
            defaultDialerLauncher.launch(intent)
        }
    }

    private fun promptDefaultSmsAppIfNeeded() {
        if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) ?: return
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                defaultSmsAppLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
            }
            defaultSmsAppLauncher.launch(intent)
        }
    }

    /**
     * Without holding this role, Android never invokes [com.callassistant.service.CallScreeningServiceImpl]
     * so numbers in the blocked list (or matched by a spam rule) will still ring through as normal
     * incoming calls even though our local blocking logic is otherwise correct.
     */
    private fun promptCallScreeningRoleIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = getSystemService(RoleManager::class.java) ?: return
        if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            callScreeningRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
        }
    }

    private fun isSetupCompleted(): Boolean {
        return getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("setup_completed", false)
    }

    private fun setSetupCompleted() {
        getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().putBoolean("setup_completed", true).apply()
    }

    private fun isSetupComplete(): Boolean {
        val allPermissions = setupPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        return allPermissions && isAccessibilityServiceEnabled() && isIgnoringBatteryOptimizations()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(AccessibilityManager::class.java) ?: return false
        val service = ComponentName(this, CallRecordingAccessibilityService::class.java)
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK) ?: return false
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == service.packageName &&
                    it.resolveInfo.serviceInfo.name == service.className
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(PowerManager::class.java) ?: return false
        if (pm.isIgnoringBatteryOptimizations(packageName)) return true

        // Some OEMs disable the battery-optimization toggle for apps that hold
        // a telecom role, so treat those as already exempt.
        val telecom = getSystemService(TelecomManager::class.java)
        if (telecom != null && telecom.defaultDialerPackage == packageName) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) ?: return false
            if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) return true
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) return true
        } else {
            if (Telephony.Sms.getDefaultSmsPackage(this) == packageName) return true
        }

        return false
    }

    private fun requestBatteryOptimizationExemption() {
        val direct = requestIgnoreBatteryOptIntent()
        try {
            batteryOptLauncher.launch(direct)
            return
        } catch (_: Exception) { /* ignore and fall through */ }
        openBatteryOptimizationSettings()
    }

    private fun requestIgnoreBatteryOptIntent(): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.fromParts("package", packageName, null)
        )
    }

    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            openApplicationDetailsSettings()
        }
    }

    private fun openApplicationDetailsSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }
}
