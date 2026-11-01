package com.motionsense.har

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.motionsense.har.ui.screens.HistoryScreen
import com.motionsense.har.ui.screens.HomeScreen
import com.motionsense.har.ui.screens.ModelInfoScreen
import com.motionsense.har.ui.screens.SettingsScreen
import com.motionsense.har.ui.screens.SummaryScreen
import com.motionsense.har.ui.viewmodel.MotionViewModel

enum class NavScreen(val title: String, val icon: ImageVector) {
    HOME("Live", Icons.Default.DirectionsRun),
    HISTORY("History", Icons.Default.History),
    SUMMARY("Summary", Icons.Default.Assessment),
    MODELS("Models", Icons.Default.Memory),
    SETTINGS("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(viewModel: MotionViewModel) {
    var selectedScreen by rememberSaveable { mutableStateOf(NavScreen.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MotionSense HAR",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavScreen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedScreen == screen,
                        onClick = { selectedScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                NavScreen.HOME -> HomeScreen(viewModel = viewModel)
                NavScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                NavScreen.SUMMARY -> SummaryScreen(viewModel = viewModel)
                NavScreen.MODELS -> ModelInfoScreen()
                NavScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
