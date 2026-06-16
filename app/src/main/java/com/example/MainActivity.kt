package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.composable
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            val navController = androidx.navigation.compose.rememberNavController()
            androidx.navigation.compose.NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    com.example.ui.home.HomeScreen(
                        onNavigateToAddProfile = { id -> 
                            if (id != null) {
                                navController.navigate("add?profileId=$id")
                            } else {
                                navController.navigate("add")
                            }
                        },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToScanner = { navController.navigate("scanner") }
                    )
                }
                composable(
                    route = "add?profileId={profileId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("profileId") {
                            type = androidx.navigation.NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val profileId = backStackEntry.arguments?.getInt("profileId") ?: 0
                    com.example.ui.add.AddProfileScreen(
                        profileId = profileId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("settings") {
                    com.example.ui.settings.SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("scanner") {
                    com.example.ui.scanner.QrScannerScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
      }
    }
  }
}

