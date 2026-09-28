package com.jarvisauth.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jarvisauth.app.JarvisAuthApp
import com.jarvisauth.app.service.UnlockForegroundService
import com.jarvisauth.app.ui.home.BatteryHelpScreen
import com.jarvisauth.app.ui.home.HomeScreen
import com.jarvisauth.app.ui.home.HomeViewModel
import com.jarvisauth.app.ui.home.LogsScreen
import com.jarvisauth.app.ui.home.LogsViewModel
import com.jarvisauth.app.ui.home.PcDetailScreen
import com.jarvisauth.app.ui.home.PcDetailViewModel
import com.jarvisauth.app.ui.pair.PairScreen
import com.jarvisauth.app.ui.pair.PairViewModel
import com.jarvisauth.app.ui.unlock.UnlockActivity

object Routes {
    const val HOME = "home"
    const val PAIR = "pair"
    const val BATTERY = "battery"
    const val LOGS = "logs"
    const val PC = "pc/{deviceId}"

    fun pc(deviceId: String): String = "pc/${Uri.encode(deviceId)}"
}

@Composable
fun JarvisAuthNav() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as JarvisAuthApp
    val factory = app.graph.viewModelFactory
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = factory)
            val pcs by vm.pcs.collectAsStateWithLifecycle()
            val armed by vm.armed.collectAsStateWithLifecycle()
            val listenerError by vm.listenerError.collectAsStateWithLifecycle()
            val needsBluetooth by vm.needsBluetooth.collectAsStateWithLifecycle()
            val permLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { UnlockForegroundService.start(context) }
            HomeScreen(
                pcs = pcs,
                armed = armed,
                listenerError = listenerError,
                onPairPc = { navController.navigate(Routes.PAIR) },
                onStartListening = {
                    val perms =
                        buildList {
                            if (Build.VERSION.SDK_INT >= 33) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            if (Build.VERSION.SDK_INT >= 31 && needsBluetooth) {
                                add(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }
                    if (perms.isEmpty()) {
                        UnlockForegroundService.start(context)
                    } else {
                        permLauncher.launch(perms.toTypedArray())
                    }
                },
                onStopListening = { UnlockForegroundService.stop(context) },
                onBatteryHelp = { navController.navigate(Routes.BATTERY) },
                onLogs = { navController.navigate(Routes.LOGS) },
                onPcClick = { navController.navigate(Routes.pc(it)) },
                onOpenBluetooth = {
                    context.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
        }
        composable(Routes.PAIR) {
            val vm: PairViewModel = viewModel(factory = factory)
            PairScreen(
                viewModel = vm,
                onClose = { navController.popBackStack() },
            )
        }
        composable(Routes.BATTERY) {
            BatteryHelpScreen(onClose = { navController.popBackStack() })
        }
        composable(Routes.LOGS) {
            val vm: LogsViewModel = viewModel(factory = factory)
            val lines by vm.lines.collectAsStateWithLifecycle()
            val debug by vm.debugEnabled.collectAsStateWithLifecycle()
            LogsScreen(
                lines = lines,
                debugEnabled = debug,
                onDebugChange = vm::setDebugEnabled,
                onClear = vm::clear,
                onClose = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PC,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
        ) { entry ->
            val deviceId = entry.arguments?.getString("deviceId").orEmpty()
            val vm: PcDetailViewModel =
                viewModel(
                    key = deviceId,
                    factory =
                        object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return PcDetailViewModel(
                                    deviceId = deviceId,
                                    store = app.graph.pairingStore,
                                    lockPc = app.graph.lockPc,
                                    unlockPc = app.graph.unlockPc,
                                    session = app.graph.unlockSession,
                                    onEmpty = { UnlockForegroundService.stop(context) },
                                ) as T
                            }
                        },
                )
            val pairing by vm.pairing.collectAsStateWithLifecycle()
            val status by vm.status.collectAsStateWithLifecycle()
            val plateUp by vm.plateUp.collectAsStateWithLifecycle()
            PcDetailScreen(
                pairing = pairing,
                status = status,
                plateUp = plateUp,
                onUnlock = {
                    vm.unlock {
                        context.startActivity(UnlockActivity.intent(context))
                    }
                },
                onLock = vm::lock,
                onShutdown = vm::shutdown,
                onRestart = vm::restart,
                onCancelUnlock = vm::cancelUnlock,
                onCancelCommand = vm::cancelCommand,
                onRemove = { vm.remove { navController.popBackStack() } },
                onClose = { navController.popBackStack() },
                missingAddressMessage = vm.missingAddressMessage(),
            )
        }
    }
}
