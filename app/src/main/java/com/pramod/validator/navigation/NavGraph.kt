package com.pramod.validator.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pramod.validator.utils.NetworkMonitor
import com.pramod.validator.utils.AISummaryGenerator
import android.content.Context
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pramod.validator.data.models.AnswerType
import com.pramod.validator.data.models.Domain
import com.pramod.validator.ui.screens.*
import com.pramod.validator.viewmodel.AuthViewModel
import com.pramod.validator.viewmodel.TrainingResourcesViewModel
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object SuperAdminDashboard : Screen("super_admin_dashboard")
    object SuperAdminHome : Screen("super_admin_home")
    object SuperAdminUsers : Screen("super_admin_users")
    object SuperAdminEnterprises : Screen("super_admin_enterprises")
    object SuperAdminStatistics : Screen("super_admin_statistics")
    object CreateEnterprise : Screen("create_enterprise")
    object CreateUser : Screen("create_user")
    object EnterpriseAdminDashboard : Screen("enterprise_admin_dashboard/{enterpriseId}") {
        fun createRoute(enterpriseId: String): String {
            return "enterprise_admin_dashboard/$enterpriseId"
        }
    }
    object EnterpriseAdminHome : Screen("enterprise_admin_home/{enterpriseId}") {
        fun createRoute(enterpriseId: String): String {
            return "enterprise_admin_home/$enterpriseId"
        }
    }
    object EnterpriseUsers : Screen("enterprise_users/{enterpriseId}") {
        fun createRoute(enterpriseId: String): String {
            return "enterprise_users/$enterpriseId"
        }
    }
    object EnterpriseFacilities : Screen("enterprise_facilities/{enterpriseId}") {
        fun createRoute(enterpriseId: String): String {
            return "enterprise_facilities/$enterpriseId"
        }
    }
    object EnterpriseDepartments : Screen("enterprise_departments/{enterpriseId}") {
        fun createRoute(enterpriseId: String): String {
            return "enterprise_departments/$enterpriseId"
        }
    }
    object CreateInvitation : Screen("create_invitation")
    object History : Screen("history")
    object PersonalDetails : Screen("personal_details")
    object ReportDetail : Screen("report_detail/{reportId}") {
        fun createRoute(reportId: String): String {
            return "report_detail/$reportId"
        }
    }
    object SubDomain : Screen("subdomain/{domainId}/{domainName}/{domainDescription}/{domainIcon}") {
        fun createRoute(domain: Domain): String {
            // URL encode parameters to handle special characters
            val encodedId = URLEncoder.encode(domain.id, StandardCharsets.UTF_8.toString())
            val encodedName = URLEncoder.encode(domain.name, StandardCharsets.UTF_8.toString())
            val encodedDescription = URLEncoder.encode(domain.description, StandardCharsets.UTF_8.toString())
            val encodedIcon = URLEncoder.encode(domain.icon, StandardCharsets.UTF_8.toString())
            return "subdomain/$encodedId/$encodedName/$encodedDescription/$encodedIcon"
        }
    }
    object Questionnaire : Screen("questionnaire/{domainId}/{domainName}/{subDomainId}/{subDomainName}/{assessmentName}/{facilityId}/{facilityName}") {
        fun createRoute(domainId: String, domainName: String, subDomainId: String, subDomainName: String, assessmentName: String, facilityId: String, facilityName: String): String {
            // URL encode parameters to handle special characters like "/" in names
            val encodedDomainId = URLEncoder.encode(domainId, StandardCharsets.UTF_8.toString())
            val encodedDomainName = URLEncoder.encode(domainName, StandardCharsets.UTF_8.toString())
            val encodedSubDomainId = URLEncoder.encode(subDomainId, StandardCharsets.UTF_8.toString())
            val encodedSubDomainName = URLEncoder.encode(subDomainName, StandardCharsets.UTF_8.toString())
            val encodedAssessmentName = URLEncoder.encode(assessmentName, StandardCharsets.UTF_8.toString())
            val encodedFacilityId = URLEncoder.encode(facilityId, StandardCharsets.UTF_8.toString())
            val encodedFacilityName = URLEncoder.encode(facilityName, StandardCharsets.UTF_8.toString())
            return "questionnaire/$encodedDomainId/$encodedDomainName/$encodedSubDomainId/$encodedSubDomainName/$encodedAssessmentName/$encodedFacilityId/$encodedFacilityName"
        }
    }
    object Report : Screen("report/{domainId}/{domainName}/{subDomainId}/{subDomainName}/{assessmentName}/{facilityId}/{facilityName}/{responses}/{questionTexts}") {
        fun createRoute(
            domainId: String, 
            domainName: String,
            subDomainId: String,
            subDomainName: String,
            assessmentName: String,
            facilityId: String,
            facilityName: String,
            responses: Map<String, AnswerType>, 
            questionTexts: Map<String, String>
        ): String {
            // URL encode parameters to handle special characters
            val encodedDomainId = URLEncoder.encode(domainId, StandardCharsets.UTF_8.toString())
            val encodedDomainName = URLEncoder.encode(domainName, StandardCharsets.UTF_8.toString())
            val encodedSubDomainId = URLEncoder.encode(subDomainId, StandardCharsets.UTF_8.toString())
            val encodedSubDomainName = URLEncoder.encode(subDomainName, StandardCharsets.UTF_8.toString())
            val encodedAssessmentName = URLEncoder.encode(assessmentName, StandardCharsets.UTF_8.toString())
            val encodedFacilityId = URLEncoder.encode(facilityId, StandardCharsets.UTF_8.toString())
            val encodedFacilityName = URLEncoder.encode(facilityName, StandardCharsets.UTF_8.toString())
            val responsesString = responses.entries.joinToString(",") { "${it.key}:${it.value.name}" }
            val questionTextsString = questionTexts.entries.joinToString("||") { "${it.key}::${it.value}" }
            val encodedResponses = URLEncoder.encode(responsesString, StandardCharsets.UTF_8.toString())
            val encodedQuestionTexts = URLEncoder.encode(questionTextsString, StandardCharsets.UTF_8.toString())
            return "report/$encodedDomainId/$encodedDomainName/$encodedSubDomainId/$encodedSubDomainName/$encodedAssessmentName/$encodedFacilityId/$encodedFacilityName/$encodedResponses/$encodedQuestionTexts"
        }
    }
    object Fda483Main : Screen("fda483_main")
    object Fda483Detail : Screen("fda483_detail/{assessmentId}") {
        fun createRoute(assessmentId: String): String {
            return "fda483_detail/$assessmentId"
        }
    }
    object CustomAssessmentMain : Screen("custom_assessment_main")
    object CustomAssessmentCreate : Screen("custom_assessment_create")
    object CustomAssessmentEdit : Screen("custom_assessment_edit/{assessmentId}") {
        fun createRoute(assessmentId: String): String {
            return "custom_assessment_edit/$assessmentId"
        }
    }
    object ResumeAssessment : Screen("resume_assessment/{inProgressAssessmentId}") {
        fun createRoute(inProgressAssessmentId: String): String {
            return "resume_assessment/$inProgressAssessmentId"
        }
    }
    object CustomQuestionnaire : Screen("custom_questionnaire/{assessmentId}/{assessmentName}") {
        fun createRoute(assessmentId: String, assessmentName: String): String {
            // URL encode parameters to handle special characters
            val encodedAssessmentId = URLEncoder.encode(assessmentId, StandardCharsets.UTF_8.toString())
            val encodedAssessmentName = URLEncoder.encode(assessmentName, StandardCharsets.UTF_8.toString())
            return "custom_questionnaire/$encodedAssessmentId/$encodedAssessmentName"
        }
    }
    object Resources : Screen("resources")
    object ResourceDetail : Screen("resource_detail/{resourceId}") {
        fun createRoute(resourceId: String): String = "resource_detail/$resourceId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Helper to navigate to a top-level destination from anywhere while preserving state
    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    // Helper to navigate to Home main page (do not restore sub-domain state)
    val navigateToHomeMain: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    // Helper function for role-based home navigation
    val navigateToHome: () -> Unit = {
        val user = currentUser
        when (user?.role) {
            "ENTERPRISE_ADMIN" -> {
                user.enterpriseId?.let { enterpriseId ->
                    navigateTopLevel(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
                }
            }
            "SUPER_ADMIN" -> {
                navigateTopLevel(Screen.SuperAdminHome.route)
            }
            else -> {
                navigateTopLevel(Screen.Home.route)
            }
        }
    }

    // Helper function for sign out navigation
    val signOut: () -> Unit = {
        authViewModel.signOut()
        navController.navigate(Screen.Login.route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { userRole, enterpriseId ->
                    // Route based on user role
                    val destination = when (userRole) {
                        "SUPER_ADMIN" -> Screen.SuperAdminHome.route
                        "ENTERPRISE_ADMIN" -> Screen.EnterpriseAdminHome.createRoute(enterpriseId)
                        else -> Screen.Home.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable(route = Screen.Home.route) { backStackEntry ->
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val inProgressViewModel: com.pramod.validator.viewmodel.InProgressAssessmentViewModel = viewModel(homeEntry)
            
            // Reload in-progress assessments when navigating to Home
            // Use the route as key to ensure it reloads when we navigate back
            LaunchedEffect(backStackEntry.id) {
                android.util.Log.d("NavGraph", "🔄 HomeScreen visible, reloading in-progress assessments")
                inProgressViewModel.loadUserInProgressAssessments()
            }
            
            HomeScreen(
                onDomainSelected = { domain ->
                    navController.navigate(Screen.SubDomain.createRoute(domain))
                },
                onNavigateToHistory = {
                    navigateTopLevel(Screen.History.route)
                },
                onNavigateToPersonalDetails = {
                    navigateTopLevel(Screen.PersonalDetails.route)
                },
                onNavigateToFda483 = {
                    navigateTopLevel(Screen.Fda483Main.route)
                },
                onNavigateToResources = {
                    navigateTopLevel(Screen.Resources.route)
                },
                onNavigateToCustomAssessment = {
                    navigateTopLevel(Screen.CustomAssessmentMain.route)
                },
                onResumeAssessment = { inProgressAssessmentId ->
                    navController.navigate(Screen.ResumeAssessment.createRoute(inProgressAssessmentId))
                },
                onSignOut = signOut,
                inProgressViewModel = inProgressViewModel
            )
        }
        
        composable(route = Screen.SuperAdminDashboard.route) {
            // Redirect to new home screen
            LaunchedEffect(Unit) {
                navController.navigate(Screen.SuperAdminHome.route) {
                    popUpTo(Screen.SuperAdminDashboard.route) { inclusive = true }
                }
            }
            // Show loading while redirecting
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        composable(route = Screen.SuperAdminHome.route) {
            val homeEntry = remember {
                navController.getBackStackEntry(Screen.SuperAdminHome.route)
            }
            val superAdminViewModel: com.pramod.validator.viewmodel.SuperAdminViewModel = viewModel(homeEntry)
            val superAdminAuthViewModel: com.pramod.validator.viewmodel.AuthViewModel = viewModel(homeEntry)
            
            SuperAdminHomeScreen(
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onNavigateToUsers = { navController.navigate(Screen.SuperAdminUsers.route) },
                onNavigateToEnterprises = { navController.navigate(Screen.SuperAdminEnterprises.route) },
                onNavigateToStatistics = { navController.navigate(Screen.SuperAdminStatistics.route) },
                superAdminViewModel = superAdminViewModel,
                authViewModel = superAdminAuthViewModel
            )
        }
        
        composable(route = Screen.SuperAdminUsers.route) {
            val usersEntry = remember {
                navController.getBackStackEntry(Screen.SuperAdminHome.route)
            }
            val superAdminViewModel: com.pramod.validator.viewmodel.SuperAdminViewModel = viewModel(usersEntry)
            val usersAuthViewModel: com.pramod.validator.viewmodel.AuthViewModel = viewModel(usersEntry)
            
            SuperAdminUsersScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.SuperAdminHome.route) {
                        popUpTo(Screen.SuperAdminHome.route) { inclusive = false }
                    }
                },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onNavigateToCreateUser = {
                    navController.navigate(Screen.CreateUser.route)
                },
                superAdminViewModel = superAdminViewModel,
                authViewModel = usersAuthViewModel
            )
        }
        
        composable(route = Screen.SuperAdminEnterprises.route) {
            val enterprisesEntry = remember {
                navController.getBackStackEntry(Screen.SuperAdminHome.route)
            }
            val superAdminViewModel: com.pramod.validator.viewmodel.SuperAdminViewModel = viewModel(enterprisesEntry)
            val enterprisesAuthViewModel: com.pramod.validator.viewmodel.AuthViewModel = viewModel(enterprisesEntry)
            
            SuperAdminEnterprisesScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.SuperAdminHome.route) {
                        popUpTo(Screen.SuperAdminHome.route) { inclusive = false }
                    }
                },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onNavigateToCreateEnterprise = {
                    navController.navigate(Screen.CreateEnterprise.route)
                },
                superAdminViewModel = superAdminViewModel,
                authViewModel = enterprisesAuthViewModel
            )
        }
        
        composable(route = Screen.SuperAdminStatistics.route) {
            val statisticsEntry = remember {
                navController.getBackStackEntry(Screen.SuperAdminHome.route)
            }
            val superAdminViewModel: com.pramod.validator.viewmodel.SuperAdminViewModel = viewModel(statisticsEntry)
            val statisticsAuthViewModel: com.pramod.validator.viewmodel.AuthViewModel = viewModel(statisticsEntry)
            
            SuperAdminStatisticsScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.SuperAdminHome.route) {
                        popUpTo(Screen.SuperAdminHome.route) { inclusive = false }
                    }
                },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                superAdminViewModel = superAdminViewModel,
                authViewModel = statisticsAuthViewModel
            )
        }

        composable(route = Screen.CreateEnterprise.route) {
            CreateEnterpriseScreen(
                onBack = {
                    navController.popBackStack()
                },
                onCreateEnterpriseSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.CreateUser.route) {
            CreateUserScreen(
                onBack = {
                    navController.popBackStack()
                },
                onCreateUserSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.EnterpriseAdminHome.route,
            arguments = listOf(
                navArgument("enterpriseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enterpriseId = backStackEntry.arguments?.getString("enterpriseId") ?: ""
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
            }
            val enterpriseAdminViewModel: com.pramod.validator.viewmodel.EnterpriseAdminViewModel = viewModel(homeEntry)
            val departmentViewModel: com.pramod.validator.viewmodel.DepartmentViewModel = viewModel(homeEntry)
            
            EnterpriseAdminHomeScreen(
                enterpriseId = enterpriseId,
                onNavigateToHistory = {
                    navigateTopLevel(Screen.History.route)
                },
                onNavigateToPersonalDetails = {
                    navigateTopLevel(Screen.PersonalDetails.route)
                },
                onNavigateToFda483 = {
                    navigateTopLevel(Screen.Fda483Main.route)
                },
                onNavigateToResources = {
                    navigateTopLevel(Screen.Resources.route)
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.EnterpriseUsers.createRoute(enterpriseId))
                },
                onNavigateToFacilities = {
                    navController.navigate(Screen.EnterpriseFacilities.createRoute(enterpriseId))
                },
                onNavigateToDepartments = {
                    navController.navigate(Screen.EnterpriseDepartments.createRoute(enterpriseId))
                },
                enterpriseAdminViewModel = enterpriseAdminViewModel,
                departmentViewModel = departmentViewModel
            )
        }
        
        composable(
            route = Screen.EnterpriseAdminDashboard.route,
            arguments = listOf(
                navArgument("enterpriseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enterpriseId = backStackEntry.arguments?.getString("enterpriseId") ?: ""
            // Redirect to new home screen
            LaunchedEffect(enterpriseId) {
                if (enterpriseId.isNotEmpty()) {
                    navController.navigate(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) {
                        popUpTo(Screen.EnterpriseAdminDashboard.createRoute(enterpriseId)) { inclusive = true }
                    }
                }
            }
            // Show loading while redirecting
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        composable(
            route = Screen.EnterpriseUsers.route,
            arguments = listOf(
                navArgument("enterpriseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enterpriseId = backStackEntry.arguments?.getString("enterpriseId") ?: ""
            val usersEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
            }
            val enterpriseAdminViewModel: com.pramod.validator.viewmodel.EnterpriseAdminViewModel = viewModel(usersEntry)
            
            EnterpriseUsersScreen(
                enterpriseId = enterpriseId,
                onNavigateToHome = {
                    navController.navigate(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) {
                        popUpTo(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) { inclusive = false }
                    }
                },
                onNavigateToHistory = {
                    navigateTopLevel(Screen.History.route)
                },
                onNavigateToPersonalDetails = {
                    navigateTopLevel(Screen.PersonalDetails.route)
                },
                onNavigateToFda483 = {
                    navigateTopLevel(Screen.Fda483Main.route)
                },
                onNavigateToResources = {
                    navigateTopLevel(Screen.Resources.route)
                },
                onNavigateToCreateInvitation = {
                    navController.navigate(Screen.CreateInvitation.route)
                },
                enterpriseAdminViewModel = enterpriseAdminViewModel
            )
        }
        
        composable(
            route = Screen.EnterpriseFacilities.route,
            arguments = listOf(
                navArgument("enterpriseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enterpriseId = backStackEntry.arguments?.getString("enterpriseId") ?: ""
            val facilitiesEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
            }
            val enterpriseAdminViewModel: com.pramod.validator.viewmodel.EnterpriseAdminViewModel = viewModel(facilitiesEntry)
            
            EnterpriseFacilitiesScreen(
                enterpriseId = enterpriseId,
                onNavigateToHome = {
                    navController.navigate(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) {
                        popUpTo(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) { inclusive = false }
                    }
                },
                onNavigateToHistory = {
                    navigateTopLevel(Screen.History.route)
                },
                onNavigateToPersonalDetails = {
                    navigateTopLevel(Screen.PersonalDetails.route)
                },
                onNavigateToFda483 = {
                    navigateTopLevel(Screen.Fda483Main.route)
                },
                onNavigateToResources = {
                    navigateTopLevel(Screen.Resources.route)
                },
                enterpriseAdminViewModel = enterpriseAdminViewModel
            )
        }
        
        composable(
            route = Screen.EnterpriseDepartments.route,
            arguments = listOf(
                navArgument("enterpriseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enterpriseId = backStackEntry.arguments?.getString("enterpriseId") ?: ""
            val departmentsEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
            }
            val departmentViewModel: com.pramod.validator.viewmodel.DepartmentViewModel = viewModel(departmentsEntry)
            
            EnterpriseDepartmentsScreen(
                enterpriseId = enterpriseId,
                onNavigateToHome = {
                    navController.navigate(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) {
                        popUpTo(Screen.EnterpriseAdminHome.createRoute(enterpriseId)) { inclusive = false }
                    }
                },
                onNavigateToHistory = {
                    navigateTopLevel(Screen.History.route)
                },
                onNavigateToPersonalDetails = {
                    navigateTopLevel(Screen.PersonalDetails.route)
                },
                onNavigateToFda483 = {
                    navigateTopLevel(Screen.Fda483Main.route)
                },
                onNavigateToResources = {
                    navigateTopLevel(Screen.Resources.route)
                },
                departmentViewModel = departmentViewModel
            )
        }
        
        composable(route = Screen.CreateInvitation.route) { backStackEntry ->
            // Get the parent dashboard entry to share the same ViewModel
            val enterpriseId = navController.previousBackStackEntry?.arguments?.getString("enterpriseId") ?: ""
            
            val dashboardEntry = remember(backStackEntry) {
                if (enterpriseId.isNotEmpty()) {
                    navController.getBackStackEntry(Screen.EnterpriseAdminHome.createRoute(enterpriseId))
                } else {
                    navController.getBackStackEntry(Screen.EnterpriseAdminDashboard.createRoute(enterpriseId))
                }
            }
            val enterpriseAdminViewModel: com.pramod.validator.viewmodel.EnterpriseAdminViewModel = viewModel(dashboardEntry)
            
            CreateInvitationScreen(
                onBack = {
                    navController.popBackStack()
                },
                onCreateInvitationSuccess = {
                    navController.popBackStack()
                },
                viewModel = enterpriseAdminViewModel
            )
        }

        composable(route = Screen.PersonalDetails.route) {
            PersonalDetailsScreen(
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToHome = navigateToHome,
                onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onSignOut = signOut
            )
        }

        composable(route = Screen.Resources.route) {
            val trainingResourcesViewModel: TrainingResourcesViewModel = viewModel()
            val user = currentUser
            if (user?.role == "SUPER_ADMIN") {
                val resourcesAuthViewModel: com.pramod.validator.viewmodel.AuthViewModel = viewModel()
                SuperAdminTrainingResourcesScreen(
                    currentUserId = user.uid,
                    onNavigateToHome = navigateToHome,
                    onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                    onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                    onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                    onSignOut = signOut,
                    viewModel = trainingResourcesViewModel,
                    authViewModel = resourcesAuthViewModel
                )
            } else {
                UserTrainingResourcesScreen(
                    onNavigateToHome = navigateToHome,
                    onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                    onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                    onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                    onResourceSelected = { resource ->
                        navController.navigate(Screen.ResourceDetail.createRoute(resource.id))
                    },
                    viewModel = trainingResourcesViewModel
                )
            }
        }

        composable(route = Screen.History.route) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.History.route)
            }
            val historyContext = LocalContext.current
            val historyScope = rememberCoroutineScope()
            
            // Trigger background AI summary generation when history screen is opened
            LaunchedEffect(Unit) {
                android.util.Log.d("NavGraph", "🔄 History screen opened - triggering background AI summary generation...")
                AISummaryGenerator.processPendingSummaries(historyContext, historyScope)
            }
            
            HistoryScreen(
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onReportClick = { reportId ->
                    navController.navigate(Screen.ReportDetail.createRoute(reportId))
                },
                onNavigateToHome = navigateToHome,
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onSignOut = signOut,
                historyViewModel = viewModel(parentEntry)
            )
        }
        
        composable(
            route = Screen.ResourceDetail.route,
            arguments = listOf(
                navArgument("resourceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId").orEmpty()
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Resources.route)
            }
            val trainingResourcesViewModel: TrainingResourcesViewModel = viewModel(parentEntry)
            ResourceDetailScreen(
                resourceId = resourceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToResources = { 
                    // Try to pop back to resources screen
                    if (!navController.popBackStack(Screen.Resources.route, inclusive = false)) {
                        // If resources screen is not in back stack, navigate to it
                        navController.navigate(Screen.Resources.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
                viewModel = trainingResourcesViewModel
            )
        }

        composable(
            route = Screen.ReportDetail.route,
            arguments = listOf(
                navArgument("reportId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getString("reportId") ?: ""
            val historyEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.History.route)
            }
            
            ReportDetailScreen(
                reportId = reportId,
                onBack = {
                    // More robust back navigation
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                historyViewModel = viewModel(historyEntry)
            )
        }

        composable(
            route = Screen.SubDomain.route,
            arguments = listOf(
                navArgument("domainId") { type = NavType.StringType },
                navArgument("domainName") { type = NavType.StringType },
                navArgument("domainDescription") { type = NavType.StringType },
                navArgument("domainIcon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // URL decode parameters to handle special characters
            val domainId = URLDecoder.decode(backStackEntry.arguments?.getString("domainId") ?: "", StandardCharsets.UTF_8.toString())
            val domainName = URLDecoder.decode(backStackEntry.arguments?.getString("domainName") ?: "", StandardCharsets.UTF_8.toString())
            val domainDescription = URLDecoder.decode(backStackEntry.arguments?.getString("domainDescription") ?: "", StandardCharsets.UTF_8.toString())
            val domainIcon = URLDecoder.decode(backStackEntry.arguments?.getString("domainIcon") ?: "", StandardCharsets.UTF_8.toString())

            val domain = remember {
                Domain(
                    id = domainId,
                    name = domainName,
                    description = domainDescription,
                    icon = domainIcon
                )
            }

            SubDomainScreen(
                domain = domain,
                onSubDomainSelected = { subDomain, assessmentName, facilityId, facilityName ->
                    try {
                        if (subDomain.id.isNotBlank() && subDomain.name.isNotBlank() && assessmentName.isNotBlank()) {
                            navController.navigate(
                                Screen.Questionnaire.createRoute(
                                    domainId, domainName,
                                    subDomain.id, subDomain.name,
                                    assessmentName, facilityId, facilityName
                                )
                            )
                        } else {
                            android.util.Log.w("NavGraph", "Invalid parameters for questionnaire navigation")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NavGraph", "Error navigating to questionnaire: ${e.message}", e)
                    }
                },
                onBack = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                onNavigateToHome = navigateToHomeMain,
                onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) }
            )
        }

        composable(
            route = Screen.Questionnaire.route,
            arguments = listOf(
                navArgument("domainId") { type = NavType.StringType },
                navArgument("domainName") { type = NavType.StringType },
                navArgument("subDomainId") { type = NavType.StringType },
                navArgument("subDomainName") { type = NavType.StringType },
                navArgument("assessmentName") { type = NavType.StringType },
                navArgument("facilityId") { type = NavType.StringType },
                navArgument("facilityName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // URL decode parameters to handle special characters
            val domainId = URLDecoder.decode(backStackEntry.arguments?.getString("domainId") ?: "", StandardCharsets.UTF_8.toString())
            val domainName = URLDecoder.decode(backStackEntry.arguments?.getString("domainName") ?: "", StandardCharsets.UTF_8.toString())
            val subDomainId = URLDecoder.decode(backStackEntry.arguments?.getString("subDomainId") ?: "", StandardCharsets.UTF_8.toString())
            val subDomainName = URLDecoder.decode(backStackEntry.arguments?.getString("subDomainName") ?: "", StandardCharsets.UTF_8.toString())
            val assessmentName = URLDecoder.decode(backStackEntry.arguments?.getString("assessmentName") ?: "", StandardCharsets.UTF_8.toString())
            val facilityId = URLDecoder.decode(backStackEntry.arguments?.getString("facilityId") ?: "", StandardCharsets.UTF_8.toString())
            val facilityName = URLDecoder.decode(backStackEntry.arguments?.getString("facilityName") ?: "", StandardCharsets.UTF_8.toString())

            // Share the same ViewModel instance from Home route so assessmentToRestore is accessible
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val sharedInProgressViewModel: com.pramod.validator.viewmodel.InProgressAssessmentViewModel = viewModel(homeEntry)

            val context = LocalContext.current
            val reportViewModel: com.pramod.validator.viewmodel.ReportViewModel = viewModel()
            val scope = rememberCoroutineScope()
            
            QuestionnaireScreen(
                subDomainId = subDomainId,
                subDomainName = subDomainName,
                facilityId = facilityId,
                facilityName = facilityName,
                domainId = domainId,
                domainName = domainName,
                assessmentName = assessmentName,
                onComplete = { responses, questionTexts ->
                    // Check network connectivity before navigating
                    val networkMonitor = NetworkMonitor(context)
                    val isOnline = networkMonitor.isCurrentlyOnline()
                    
                    if (!isOnline) {
                        // Offline: Generate report immediately (will be fast, marks AI summary as pending)
                        // Then navigate to show completion screen
                        android.util.Log.w("NavGraph", "⚠️ Offline: Generating report immediately before navigation")
                        scope.launch {
                            reportViewModel.generateReport(
                                domainId = domainId,
                                domainName = domainName,
                                subDomainId = subDomainId,
                                subDomainName = subDomainName,
                                assessmentName = assessmentName,
                                facilityId = facilityId,
                                facilityName = facilityName,
                                responses = responses,
                                questionTexts = questionTexts, // Pass question texts already loaded
                                context = context
                            )
                            // Small delay to ensure report is set
                            delay(100)
                            // Navigate to report screen
                            navController.navigate(
                                Screen.Report.createRoute(
                                    domainId, domainName,
                                    subDomainId, subDomainName,
                                    assessmentName, facilityId, facilityName, responses, questionTexts
                                )
                            ) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    } else {
                        // Online: Generate report first (will use question texts), then navigate
                        scope.launch {
                            reportViewModel.generateReport(
                                domainId = domainId,
                                domainName = domainName,
                                subDomainId = subDomainId,
                                subDomainName = subDomainName,
                                assessmentName = assessmentName,
                                facilityId = facilityId,
                                facilityName = facilityName,
                                responses = responses,
                                questionTexts = questionTexts, // Pass question texts already loaded
                                context = context
                            )
                            // Small delay to ensure report is set
                            delay(100)
                            // Navigate to report screen
                            navController.navigate(
                                Screen.Report.createRoute(
                                    domainId, domainName,
                                    subDomainId, subDomainName,
                                    assessmentName, facilityId, facilityName, responses, questionTexts
                                )
                            ) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                },
                onBack = {
                    // More robust back navigation
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                },
                inProgressViewModel = sharedInProgressViewModel
            )
        }

        composable(
            route = Screen.Report.route,
            arguments = listOf(
                navArgument("domainId") { type = NavType.StringType },
                navArgument("domainName") { type = NavType.StringType },
                navArgument("subDomainId") { type = NavType.StringType },
                navArgument("subDomainName") { type = NavType.StringType },
                navArgument("assessmentName") { type = NavType.StringType },
                navArgument("facilityId") { type = NavType.StringType },
                navArgument("facilityName") { type = NavType.StringType },
                navArgument("responses") { type = NavType.StringType },
                navArgument("questionTexts") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // URL decode parameters to handle special characters
            val domainId = URLDecoder.decode(backStackEntry.arguments?.getString("domainId") ?: "", StandardCharsets.UTF_8.toString())
            val domainName = URLDecoder.decode(backStackEntry.arguments?.getString("domainName") ?: "", StandardCharsets.UTF_8.toString())
            val subDomainId = URLDecoder.decode(backStackEntry.arguments?.getString("subDomainId") ?: "", StandardCharsets.UTF_8.toString())
            val subDomainName = URLDecoder.decode(backStackEntry.arguments?.getString("subDomainName") ?: "", StandardCharsets.UTF_8.toString())
            val assessmentName = URLDecoder.decode(backStackEntry.arguments?.getString("assessmentName") ?: "", StandardCharsets.UTF_8.toString())
            val facilityId = URLDecoder.decode(backStackEntry.arguments?.getString("facilityId") ?: "", StandardCharsets.UTF_8.toString())
            val facilityName = URLDecoder.decode(backStackEntry.arguments?.getString("facilityName") ?: "", StandardCharsets.UTF_8.toString())
            // URL decode responses and questionTexts
            val responsesString = URLDecoder.decode(backStackEntry.arguments?.getString("responses") ?: "", StandardCharsets.UTF_8.toString())
            val questionTextsString = URLDecoder.decode(backStackEntry.arguments?.getString("questionTexts") ?: "", StandardCharsets.UTF_8.toString())

            val responses = remember {
                if (responsesString.isNotEmpty()) {
                    responsesString.split(",")
                        .associate {
                            val parts = it.split(":")
                            parts[0] to AnswerType.valueOf(parts[1])
                        }
                } else {
                    emptyMap()
                }
            }
            
            val questionTexts = remember {
                if (questionTextsString.isNotEmpty()) {
                    questionTextsString.split("||")
                        .associate {
                            val parts = it.split("::")
                            if (parts.size == 2) parts[0] to parts[1] else "" to ""
                        }
                        .filterKeys { it.isNotEmpty() }
                } else {
                    emptyMap()
                }
            }

            // Check if this is a custom assessment to navigate back correctly
            val isCustomAssessment = domainId == "custom"
            val customAssessmentViewModel: com.pramod.validator.viewmodel.CustomAssessmentViewModel? = 
                if (isCustomAssessment) viewModel() else null
            val scope = rememberCoroutineScope()
            
            ReportScreen(
                domainId = domainId,
                domainName = domainName,
                subDomainId = subDomainId,
                subDomainName = subDomainName,
                assessmentName = assessmentName,
                facilityId = facilityId,
                facilityName = facilityName,
                responses = responses,
                questionTexts = questionTexts,
                onBackToHome = {
                    if (isCustomAssessment && subDomainId.isNotBlank()) {
                        // Check if assessment came from checklist
                        customAssessmentViewModel?.let { vm ->
                            scope.launch {
                                // Check if assessment is from checklist
                                val isFromChecklist = vm.isAssessmentFromChecklist(subDomainId)
                                if (isFromChecklist) {
                                    vm.setShouldShowChecklistTab(true)
                                }
                                // Navigate to Custom Assessment screen
                                navController.navigate(Screen.CustomAssessmentMain.route) {
                                    popUpTo(Screen.CustomAssessmentMain.route) { inclusive = true }
                                }
                            }
                        } ?: run {
                            // If ViewModel is null, just navigate to Custom Assessment screen
                            navController.navigate(Screen.CustomAssessmentMain.route) {
                                popUpTo(Screen.CustomAssessmentMain.route) { inclusive = true }
                            }
                        }
                    } else {
                        // Regular assessment - navigate to Home
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(route = Screen.Fda483Main.route) {
            Fda483MainScreen(
                onNavigateToHome = navigateToHome,
                onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onNavigateToAssessmentDetail = { assessmentId ->
                    navController.navigate(Screen.Fda483Detail.createRoute(assessmentId))
                }
            )
        }
        
        composable(
            route = Screen.Fda483Detail.route,
            arguments = listOf(
                navArgument("assessmentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: ""
            val fda483MainEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Fda483Main.route)
            }
            val fda483ViewModel: com.pramod.validator.viewmodel.Fda483ViewModel = viewModel(fda483MainEntry)
            
            Fda483DetailScreen(
                assessmentId = assessmentId,
                onNavigateToHome = navigateToHome,
                onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToFda483History = {
                    fda483ViewModel.setShouldShowHistoryTab(true)
                    navController.popBackStack(Screen.Fda483Main.route, inclusive = false)
                },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) },
                onNavigateToCustomAssessment = { navigateTopLevel(Screen.CustomAssessmentMain.route) }
            )
        }
        
        composable(route = Screen.CustomAssessmentMain.route) {
            CustomAssessmentScreen(
                onBack = navigateToHome,
                onNavigateToHome = navigateToHome,
                onNavigateToHistory = { navigateTopLevel(Screen.History.route) },
                onNavigateToPersonalDetails = { navigateTopLevel(Screen.PersonalDetails.route) },
                onNavigateToCustomQuestionnaire = { assessmentId, assessmentName ->
                    navController.navigate(Screen.CustomQuestionnaire.createRoute(assessmentId, assessmentName))
                },
                onNavigateToCreateAssessment = {
                    navController.navigate(Screen.CustomAssessmentCreate.route)
                },
                onNavigateToEditAssessment = { assessmentId ->
                    navController.navigate(Screen.CustomAssessmentEdit.createRoute(assessmentId))
                },
                onNavigateToFda483 = { navigateTopLevel(Screen.Fda483Main.route) },
                onNavigateToResources = { navigateTopLevel(Screen.Resources.route) }
            )
        }

        composable(route = Screen.CustomAssessmentCreate.route) {
            CustomAssessmentCreateScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.CustomAssessmentEdit.route,
            arguments = listOf(
                navArgument("assessmentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: ""
            CustomAssessmentEditScreen(
                assessmentId = assessmentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.CustomQuestionnaire.route,
            arguments = listOf(
                navArgument("assessmentId") { type = NavType.StringType },
                navArgument("assessmentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val assessmentId = backStackEntry.arguments?.getString("assessmentId") ?: ""
            val assessmentName = backStackEntry.arguments?.getString("assessmentName") ?: "Custom Assessment"
            val customReportViewModel: com.pramod.validator.viewmodel.ReportViewModel = viewModel()
            
            // Share the same ViewModel instance from Home route so assessmentToRestore is accessible
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val sharedInProgressViewModel: com.pramod.validator.viewmodel.InProgressAssessmentViewModel = viewModel(homeEntry)
            
            val customContext = LocalContext.current
            val customScope = rememberCoroutineScope()
            
            CustomQuestionnaireScreen(
                assessmentId = assessmentId,
                assessmentName = assessmentName,
                onComplete = { responses, questionTexts ->
                    // Check network connectivity before navigating
                    val networkMonitor = NetworkMonitor(customContext)
                    val isOnline = networkMonitor.isCurrentlyOnline()
                    
                    if (!isOnline) {
                        // Offline: Generate report immediately (will be fast, marks AI summary as pending)
                        // Then navigate to show completion screen
                        android.util.Log.w("NavGraph", "⚠️ Offline: Generating custom assessment report immediately before navigation")
                        customScope.launch {
                            customReportViewModel.generateCustomAssessmentReport(
                                assessmentId = assessmentId,
                                assessmentName = assessmentName,
                                responses = responses,
                                questionTexts = questionTexts,
                                context = customContext
                            )
                            // Small delay to ensure report is set
                            delay(100)
                            // Navigate to report screen
                            navController.navigate(
                                Screen.Report.createRoute(
                                    domainId = "custom",
                                    domainName = "Custom Assessment",
                                    subDomainId = assessmentId,
                                    subDomainName = assessmentName,
                                    assessmentName = assessmentName,
                                    facilityId = "",
                                    facilityName = "",
                                    responses = responses,
                                    questionTexts = questionTexts
                                )
                            ) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    } else {
                        // Online: Generate report and navigate (will show loading screen and generate summary)
                        customReportViewModel.generateCustomAssessmentReport(
                            assessmentId = assessmentId,
                            assessmentName = assessmentName,
                            responses = responses,
                            questionTexts = questionTexts,
                            context = customContext
                        )
                        // Navigate to report screen
                        navController.navigate(
                            Screen.Report.createRoute(
                                domainId = "custom",
                                domainName = "Custom Assessment",
                                subDomainId = assessmentId,
                                subDomainName = assessmentName,
                                assessmentName = assessmentName,
                                facilityId = "",
                                facilityName = "",
                                responses = responses,
                                questionTexts = questionTexts
                            )
                        ) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                inProgressViewModel = sharedInProgressViewModel
            )
        }
        
        composable(
            route = Screen.ResumeAssessment.route,
            arguments = listOf(
                navArgument("inProgressAssessmentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val inProgressAssessmentId = backStackEntry.arguments?.getString("inProgressAssessmentId") ?: ""
            // Use the same ViewModel instance from Home route so assessmentToRestore persists
            val homeEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val inProgressViewModel: com.pramod.validator.viewmodel.InProgressAssessmentViewModel = viewModel(homeEntry)
            
            LaunchedEffect(inProgressAssessmentId) {
                if (inProgressAssessmentId.isNotBlank()) {
                    inProgressViewModel.getInProgressAssessmentById(inProgressAssessmentId) { assessment ->
                        if (assessment != null) {
                            // Store assessment to restore in ViewModel
                            inProgressViewModel.setAssessmentToRestore(assessment)
                            
                            if (assessment.isCustomAssessment) {
                                // Navigate to custom questionnaire
                                navController.navigate(
                                    Screen.CustomQuestionnaire.createRoute(
                                        assessment.subDomainId,
                                        assessment.assessmentName
                                    )
                                ) {
                                    popUpTo(Screen.Home.route)
                                }
                            } else {
                                // Navigate to regular questionnaire
                                navController.navigate(
                                    Screen.Questionnaire.createRoute(
                                        assessment.domainId,
                                        assessment.domainName,
                                        assessment.subDomainId,
                                        assessment.subDomainName,
                                        assessment.assessmentName,
                                        assessment.facilityId,
                                        assessment.facilityName
                                    )
                                ) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        }
                    }
                }
            }
            
            // Show loading while fetching assessment
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

