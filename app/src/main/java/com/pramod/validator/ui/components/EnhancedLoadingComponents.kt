package com.pramod.validator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

/**
 * Enhanced loading screen with animated elements
 */
@Composable
fun EnhancedLoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Animated loading icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                
                // Animated dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val dotScale by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, delayMillis = index * 200, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(dotScale)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Lottie animation component
 */
@Composable
fun LottieAnimationComponent(
    animationRes: Int,
    modifier: Modifier = Modifier,
    iterations: Int = Int.MAX_VALUE,
    speed: Float = 1f
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animationRes)
    )
    
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed
    )
    
    LottieAnimation(
        composition = composition,
        progress = progress,
        modifier = modifier
    )
}

/**
 * Enhanced swipe refresh with custom styling
 */
@Composable
fun EnhancedSwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh,
        content = content
    )
}

/**
 * Loading state with different types
 */
@Composable
fun LoadingState(
    isLoading: Boolean,
    loadingType: LoadingType = LoadingType.DEFAULT,
    message: String = "Loading...",
    content: @Composable () -> Unit
) {
    when {
        isLoading -> {
            when (loadingType) {
                LoadingType.DEFAULT -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                LoadingType.ENHANCED -> {
                    EnhancedLoadingScreen(message = message)
                }
                LoadingType.OVERLAY -> {
                    Box {
                        content()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            EnhancedLoadingScreen(message = message)
                        }
                    }
                }
            }
        }
        else -> {
            content()
        }
    }
}

enum class LoadingType {
    DEFAULT,
    ENHANCED,
    OVERLAY
}

/**
 * Permission loading component with animation
 */
@Composable
fun PermissionLoadingComponent(
    message: String = "Loading permissions...",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Animated success/error states
 */
@Composable
fun AnimatedStatusCard(
    isSuccess: Boolean,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isSuccess) 
                    Icons.Default.Info 
                else 
                    Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSuccess) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSuccess) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
