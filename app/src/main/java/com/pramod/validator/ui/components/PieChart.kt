package com.pramod.validator.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class PieChartData(
    val label: String,
    val value: Int,
    val color: Color
)

@Composable
fun ResponsesPieChart(
    totalQuestions: Int,
    compliantCount: Int,
    nonCompliantCount: Int,
    notApplicableCount: Int,
    modifier: Modifier = Modifier
) {
    val compliantColor = Color(0xFF4CAF50) // Green for compliant
    val nonCompliantColor = MaterialTheme.colorScheme.error
    val notApplicableColor = Color(0xFF9E9E9E) // Gray for N/A
    
    val data = listOf(
        PieChartData("Compliant", compliantCount, compliantColor),
        PieChartData("Non-Compliant", nonCompliantCount, nonCompliantColor),
        PieChartData("Not Applicable", notApplicableCount, notApplicableColor)
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Responses",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A), // slate-900
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Pie chart and legend in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedPieChart(
                        data = data,
                        totalQuestions = totalQuestions,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Center text showing total
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = totalQuestions.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A) // slate-900
                        )
                        Text(
                            text = "Total",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B) // slate-500
                        )
                    }
                }
                
                // Legend
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data.forEach { item ->
                        LegendItem(
                            label = item.label,
                            value = item.value,
                            color = item.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedPieChart(
    data: List<PieChartData>,
    totalQuestions: Int,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }
    
    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2.5f
        val strokeWidth = 45f
        val center = Offset(size.width / 2, size.height / 2)
        
        var startAngle = -90f
        
        data.forEach { item ->
            if (item.value > 0) {
                val sweepAngle = (item.value.toFloat() / totalQuestions) * 360f * animationProgress.value
                
                // Draw arc
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )
                
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = CircleShape)
        )
        
        // Label and value
        Column {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A) // slate-900
            )
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A) // slate-900
            )
        }
    }
}

