package com.pramod.validator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Decorative abstract background with light geometric shapes
 */
@Composable
fun AbstractBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Light colors with varying opacity (increased by 5% from base) for aesthetic appeal
        val primaryLight = Color(0xFFE3F2FD).copy(alpha = 0.40f) // Light blue - 40%
        val secondaryLight = Color(0xFFF3E5F5).copy(alpha = 0.38f) // Light purple - 38%
        val accentLight = Color(0xFFE8F5E9).copy(alpha = 0.42f) // Light green - 42%
        
        // Draw large circles in the background
        drawCircle(
            color = primaryLight,
            radius = width * 0.4f,
            center = Offset(width * 0.2f, height * 0.15f)
        )
        
        drawCircle(
            color = secondaryLight,
            radius = width * 0.35f,
            center = Offset(width * 0.8f, height * 0.25f)
        )
        
        drawCircle(
            color = accentLight,
            radius = width * 0.3f,
            center = Offset(width * 0.5f, height * 0.7f)
        )
        
        // Draw abstract wave-like curves (full waves without flat backgrounds)
        val wavePath1 = Path().apply {
            moveTo(0f, height * 0.3f)
            quadraticTo(
                width * 0.25f, height * 0.25f,
                width * 0.5f, height * 0.3f
            )
            quadraticTo(
                width * 0.75f, height * 0.35f,
                width, height * 0.3f
            )
            // Continue the wave pattern instead of closing with flat lines
            quadraticTo(
                width * 1.25f, height * 0.25f,
                width * 1.5f, height * 0.3f
            )
        }
        drawPath(
            path = wavePath1,
            color = primaryLight.copy(alpha = 0.30f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 60f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        val wavePath2 = Path().apply {
            moveTo(0f, height * 0.6f)
            quadraticTo(
                width * 0.3f, height * 0.65f,
                width * 0.6f, height * 0.6f
            )
            quadraticTo(
                width * 0.9f, height * 0.55f,
                width, height * 0.6f
            )
            // Continue the wave pattern
            quadraticTo(
                width * 1.2f, height * 0.65f,
                width * 1.5f, height * 0.6f
            )
        }
        drawPath(
            path = wavePath2,
            color = secondaryLight.copy(alpha = 0.28f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 50f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Additional flowing wave at the bottom (fades into bottom bar)
        val wavePath3 = Path().apply {
            moveTo(0f, height * 0.85f)
            quadraticTo(
                width * 0.2f, height * 0.88f,
                width * 0.4f, height * 0.85f
            )
            quadraticTo(
                width * 0.6f, height * 0.82f,
                width * 0.8f, height * 0.85f
            )
            quadraticTo(
                width * 1.0f, height * 0.88f,
                width * 1.2f, height * 0.85f
            )
            // Extend to bottom edge
            lineTo(width * 1.2f, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = wavePath3,
            color = accentLight.copy(alpha = 0.25f)
        )
        
        // Draw some small decorative circles
        for (i in 0..5) {
            val x = width * (0.15f + i * 0.15f)
            val y = height * (0.1f + (i % 3) * 0.3f)
            drawCircle(
                color = accentLight.copy(alpha = 0.30f),
                radius = 30f + i * 5f,
                center = Offset(x, y)
            )
        }
    }
}

