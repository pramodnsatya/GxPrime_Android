package com.pramod.validator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.platform.LocalDensity
import com.pramod.validator.data.models.User
import com.pramod.validator.data.models.UserPermission
import com.pramod.validator.utils.PermissionChecker

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    object Home : BottomNavItem("Home", Icons.Default.Home, "home")
    object History : BottomNavItem("History", Icons.Default.AccessTime, "history")
    object Fda483 : BottomNavItem("FDA 483", Icons.Default.Description, "fda483")
    object Resources : BottomNavItem("Resources", Icons.Default.MenuBook, "resources")
    object Profile : BottomNavItem("Profile", Icons.Default.Person, "profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    user: User? = null,
    permissions: UserPermission? = null
) {
    // Filter items based on user role and permissions
    val allItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.History,
        BottomNavItem.Fda483,
        BottomNavItem.Resources,
        BottomNavItem.Profile
    )
    
    val items = allItems.filter { item ->
        // For Super Admin, hide FDA 483 and History, but show Resources
        if (user?.role == "SUPER_ADMIN") {
            item != BottomNavItem.Fda483 && item != BottomNavItem.History
        }
        // For Enterprise Admin, hide FDA 483 and Resources
        else if (user?.role == "ENTERPRISE_ADMIN") {
            item != BottomNavItem.Fda483 && item != BottomNavItem.Resources
        } else {
            // For other users, show FDA 483 only if they have permission
            if (item == BottomNavItem.Fda483) {
                PermissionChecker.canAccessFda483Analysis(user, permissions)
            } else {
                true
            }
        }
    }

    // Check if device has system navigation bars (not gesture navigation)
    val density = LocalDensity.current
    val navigationBarBottomPx = WindowInsets.navigationBars.getBottom(density)
    val hasNavigationBars = navigationBarBottomPx > 0
    
    // White navigation bar with backdrop blur - bg-white/95 backdrop-blur-xl
    // Only add padding for system navigation bars if they actually exist
    Surface(
        modifier = modifier
            .then(
                if (hasNavigationBars) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier
                }
            ),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = when {
                    currentRoute == item.route -> true
                    currentRoute == "home" && item.route == "home" -> true
                    currentRoute == "enterprise_admin" && item.route == "home" -> true
                    currentRoute.startsWith("enterprise_admin_dashboard") && item.route == "home" -> true
                    currentRoute == "super_admin" && item.route == "home" -> true
                    currentRoute == "super_admin_dashboard" && item.route == "home" -> true
                    (currentRoute == "fda483_main" || currentRoute.startsWith("fda483_detail")) && item.route == "fda483" -> true
                    currentRoute == "resources" && item.route == "resources" -> true
                    else -> false
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(
                                            color = Color(0xFFEFF6FF), // blue-50
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                } else {
                                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                }
                            )
                    ) {
                        // Icon - Special handling for FDA 483 to show document with exclamation
                        if (item == BottomNavItem.Fda483) {
                            Box(
                                modifier = Modifier.size(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) {
                                        Color(0xFF2563EB) // blue-600
                                    } else {
                                        Color(0xFF64748B) // slate-500
                                    }
                                )
                                // Exclamation mark overlay
                                Text(
                                    text = "!",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        Color(0xFF2563EB) // blue-600
                                    } else {
                                        Color(0xFF64748B) // slate-500
                                    },
                                    modifier = Modifier.offset(x = 5.dp, y = (-5).dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(20.dp),
                                tint = if (isSelected) {
                                    Color(0xFF2563EB) // blue-600
                                } else {
                                    Color(0xFF64748B) // slate-500
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Label
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) {
                                Color(0xFF2563EB) // blue-600
                            } else {
                                Color(0xFF64748B) // slate-500
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Bottom indicator bar for active tab
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(2.dp)
                                    .background(
                                        color = Color(0xFF2563EB), // blue-600
                                        shape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(
    title: String,
    onMenuClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}
