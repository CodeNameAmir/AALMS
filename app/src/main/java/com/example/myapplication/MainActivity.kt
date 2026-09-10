package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.auth.AuthViewModel
import com.example.myapplication.ui.auth.AuthViewModelFactory
import com.example.myapplication.ui.auth.LoginScreen
import com.example.myapplication.ui.logs.AddEditLogScreen
import com.example.myapplication.ui.logs.LogListScreen
import com.example.myapplication.ui.logs.LogViewModel
import com.example.myapplication.ui.logs.LogViewModelFactory
import com.example.myapplication.ui.stats.StatsScreen
import com.example.myapplication.ui.stats.StatsViewModel
import com.example.myapplication.ui.stats.StatsViewModelFactory
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    private val logViewModel: LogViewModel by viewModels {
        val app = application as LogApplication
        LogViewModelFactory(app.repository, app.userManager)
    }

    private val statsViewModel: StatsViewModel by viewModels {
        val app = application as LogApplication
        StatsViewModelFactory(app.repository, app.userManager)
    }

    private val authViewModel: AuthViewModel by viewModels {
        val app = application as LogApplication
        AuthViewModelFactory(app.userManager, app.databaseManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LogManagerApp(logViewModel, statsViewModel, authViewModel)
                }
            }
        }
    }
}

@Composable
fun LogManagerApp(
    logViewModel: LogViewModel,
    statsViewModel: StatsViewModel,
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    val userId by authViewModel.loggedInUserId.collectAsState()
    val loading by authViewModel.loading.collectAsState()

    androidx.compose.runtime.LaunchedEffect(userId) {
        if (userId == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (userId == null) "login" else "logs"
        ) {
            composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = { 
                        navController.navigate("logs") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("logs") {
                LogListScreen(
                    viewModel = logViewModel,
                    onAddLogClick = { navController.navigate("add_log") },
                    onLogClick = { logWithCat -> 
                        navController.navigate("edit_log/${logWithCat.log.id}") 
                    },
                    onStatsClick = { navController.navigate("stats") }
                )
            }
            composable("add_log") {
                AddEditLogScreen(
                    viewModel = logViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_log/{logId}",
                arguments = listOf(navArgument("logId") { type = NavType.LongType })
            ) { backStackEntry ->
                val logId = backStackEntry.arguments?.getLong("logId")
                AddEditLogScreen(
                    viewModel = logViewModel,
                    logId = logId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("stats") {
                StatsScreen(
                    viewModel = statsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (loading) {
            Dialog(onDismissRequest = {}) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Syncing Data...",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
