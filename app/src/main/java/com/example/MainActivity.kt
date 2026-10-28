package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.FoodScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MeasurementUnitsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.HealthNavy
import com.example.ui.theme.HealthTeal
import com.example.ui.theme.NutriGlassTheme
import com.example.ui.viewmodel.HealthViewModel

import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.screens.WalkAndListenScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.WeeklyCheckInScreen
import com.example.ui.screens.WeeklyProgressDashboardScreen
import androidx.compose.material.icons.filled.AutoStories

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Food : Screen("food", "Food", Icons.Default.Restaurant)
    object Scan : Screen("scan", "AI Lens", Icons.Default.CenterFocusStrong)
    object Activity : Screen("activity", "Activity", Icons.Default.DirectionsWalk)
    object Progress : Screen("progress", "Progress", Icons.Default.EmojiEvents)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object AiCoach : Screen("ai_coach", "AI Coach", Icons.Default.Home)
    object MeasurementUnits : Screen("measurement_units", "Units", Icons.Default.Tune)
    object MonthlyReport : Screen("monthly_report", "Monthly Report", Icons.Default.Assessment)
    object WalkAndLearn : Screen("walk_and_learn", "Walk & Learn", Icons.Default.AutoStories)
    object PdfReader : Screen("pdf_reader", "PDF Reader", Icons.Default.AutoStories)
    object WeeklyCheckIn : Screen("weekly_check_in", "Weekend Check-In", Icons.Default.Assessment)
    object WeeklyProgressDashboard : Screen("weekly_progress", "Weekly Progress", Icons.Default.Assessment)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriGlassTheme {
                NutriGlassApp()
            }
        }
    }
}

@Composable
fun NutriGlassApp(
    viewModel: HealthViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var isOnboardingActive by remember { mutableStateOf(!userProfile.onboardingCompleted) }

    // Synchronize onboarding state if profile changes
    if (!userProfile.onboardingCompleted && isOnboardingActive) {
        OnboardingScreen(
            viewModel = viewModel,
            onOnboardingFinished = { isOnboardingActive = false }
        )
        return
    }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Food,
        Screen.Scan,
        Screen.Activity,
        Screen.Progress,
        Screen.Profile
    )

    // Handle Android system back gesture safely
    BackHandler(enabled = currentScreen != Screen.Home) {
        if (currentScreen == Screen.MeasurementUnits) {
            currentScreen = Screen.Profile
        } else {
            currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != Screen.AiCoach && currentScreen != Screen.MeasurementUnits) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentScreen.route == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HealthTeal,
                                selectedTextColor = HealthTeal,
                                indicatorColor = HealthTeal.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { currentScreen = Screen.Scan },
                    onNavigateToFood = { currentScreen = Screen.Food },
                    onNavigateToActivity = { currentScreen = Screen.Activity },
                    onNavigateToProgress = { currentScreen = Screen.Progress },
                    onNavigateToAiCoach = { currentScreen = Screen.AiCoach }
                )
                Screen.Food -> FoodScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { currentScreen = Screen.Scan }
                )
                Screen.Scan -> ScannerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onMealLogged = { currentScreen = Screen.Food }
                )
                Screen.Activity -> ActivityScreen(
                    viewModel = viewModel
                )
                Screen.Progress -> ProgressScreen(
                    viewModel = viewModel
                )
                Screen.Profile -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToMeasurementUnits = { currentScreen = Screen.MeasurementUnits },
                    onNavigateToMonthlyReport = { currentScreen = Screen.MonthlyReport },
                    onNavigateToWalkAndLearn = { currentScreen = Screen.WalkAndLearn },
                    onNavigateToWeeklyCheckIn = { currentScreen = Screen.WeeklyCheckIn },
                    onNavigateToWeeklyProgress = { currentScreen = Screen.WeeklyProgressDashboard }
                )
                Screen.MeasurementUnits -> MeasurementUnitsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Profile }
                )
                Screen.AiCoach -> AiCoachScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.MonthlyReport -> MonthlyReportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Profile }
                )
                Screen.WalkAndLearn -> WalkAndListenScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.PdfReader -> {
                    val bookId = viewModel.selectedBookId.collectAsState().value ?: 0L
                    PdfReaderScreen(
                        viewModel = viewModel,
                        bookId = bookId,
                        onNavigateBack = { currentScreen = Screen.WalkAndLearn }
                    )
                }
                Screen.WeeklyCheckIn -> WeeklyCheckInScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home }
                )
                Screen.WeeklyProgressDashboard -> WeeklyProgressDashboardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.Home },
                    onNavigateToCheckIn = { currentScreen = Screen.WeeklyCheckIn }
                )
            }
        }
    }
}
