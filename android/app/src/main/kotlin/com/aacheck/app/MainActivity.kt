package com.aacheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aacheck.app.core.TokenStore
import com.aacheck.app.ui.AuthScreen
import com.aacheck.app.ui.AuthViewModel
import com.aacheck.app.ui.HomeScreen
import com.aacheck.app.ui.HomeViewModel
import com.aacheck.app.ui.TravelScreen
import com.aacheck.app.ui.TravelViewModel
import com.aacheck.app.ui.theme.AaCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AaCheckApp).container
        setContent {
            AaCheckTheme { AppRoot(container) }
        }
    }
}

@Composable
fun AppRoot(container: com.aacheck.app.core.AppContainer) {
    val nav = rememberNavController()
    val start = if (container.tokenStore.isLoggedIn()) "home" else "auth"

    NavHost(navController = nav, startDestination = start) {
        composable("auth") {
            val vm: AuthViewModel = viewModel { AuthViewModel(container.repository) }
            AuthScreen(vm, onDone = {
                nav.navigate("home") { popUpTo("auth") { inclusive = true } }
            })
        }
        composable("home") {
            val vm: HomeViewModel = viewModel { HomeViewModel(container.repository) }
            HomeScreen(
                vm = vm,
                onOpenTravel = { id -> nav.navigate("travel/$id") },
                onLogout = { container.tokenStore.clear(); nav.navigate("auth") { popUpTo(0) } },
            )
        }
        composable(
            "travel/{travelId}",
            arguments = listOf(navArgument("travelId") { type = NavType.LongType }),
        ) { entry ->
            val travelId = entry.arguments?.getLong("travelId") ?: return@composable
            val vm: TravelViewModel = viewModel { TravelViewModel(container.repository, container.wsClient) }
            LaunchedEffect(travelId) {
                vm.load(travelId)
                container.wsClient.events.collect { ev ->
                    if (ev.travelId == travelId) vm.onWsEvent(travelId)
                }
            }
            TravelScreen(vm = vm, travelId = travelId, onBack = { nav.popBackStack() })
        }
    }
}
