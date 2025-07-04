package com.example.myapplication2.ui.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun Gauge(
    modifier: Modifier = Modifier,
    value: Float,
    unit: String,
    max: Float = 180f,
    minValue: Float = 0f,
    redlineValue: Float? = null
) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 500),
        label = "gauge_animation"
    )
    
    Box(
        modifier = modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawGauge(
                value = animatedValue,
                maxValue = max,
                minValue = minValue,
                redlineValue = redlineValue
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = 20.dp),

        ) {
            Text(
                text = "${animatedValue.toInt()}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = unit,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

private fun DrawScope.drawGauge(
    value: Float,
    maxValue: Float,
    minValue: Float,
    redlineValue: Float?
) {
    val strokeWidth = 12.dp.toPx()
    val center = Offset(size.width / 2, size.height * 0.75f) // Lower center for quarter circle
    val radius = size.width * 0.55f
    
    // Quarter circle gauge (180 degrees from left to right)
    val startAngle = 180f
    val sweepAngle = 145f

    // Calculate zone angles
    val normalZone = 0.6f // 60% is normal (green)
    val warningZone = 0.8f // 80% is warning (yellow)
    val redlineZone = redlineValue?.let { it / maxValue } ?: 1.0f
    
    val normalAngle = normalZone * sweepAngle
    val warningAngle = warningZone * sweepAngle
    val redlineAngle = redlineZone * sweepAngle
    
    // Draw background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.5f),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth + 4.dp.toPx(), cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )
    
    // Green zone (normal operation)
    drawArc(
        color = Color(0xFF00AA00),
        startAngle = startAngle,
        sweepAngle = normalAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )
    
    // Yellow zone (warning)
    drawArc(
        color = Color(0xFFFFAA00),
        startAngle = startAngle + normalAngle,
        sweepAngle = warningAngle - normalAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )
    
    // Red zone (redline)
    drawArc(
        color = Color(0xFFFF3333),
        startAngle = startAngle + warningAngle,
        sweepAngle = sweepAngle - warningAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )

    // Draw tick marks first
    drawTickMarks(center, radius, maxValue, minValue)

    // Draw needle
    val valueAngle = ((value - minValue) / (maxValue - minValue)) * sweepAngle
    val needleAngle = startAngle + valueAngle
    val needleLength = radius

    val needleEnd = Offset(
        center.x + needleLength * cos(Math.toRadians(needleAngle.toDouble())).toFloat(),
        center.y + needleLength * sin(Math.toRadians(needleAngle.toDouble())).toFloat()
    )

    // Needle shadow fo
    val needleStart = Offset(
        center.x + (radius * 0.75f) * cos(Math.toRadians(needleAngle.toDouble())).toFloat(),
        center.y + (radius * 0.75f) * sin(Math.toRadians(needleAngle.toDouble())).toFloat()
    )
    
    // Needle shadow for depth
    drawLine(
        color = Color.Black.copy(alpha = 0.4f),
        start = center,
        end = Offset(needleEnd.x + 2, needleEnd.y + 2),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round
    )
    
    // Main needle
    drawLine(
        color = Color.White,
        start = needleStart,
        end = needleEnd,
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = Color.White,
        radius = 6.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawTickMarks(
    center: Offset,
    radius: Float,
    maxValue: Float,
    minValue: Float
) {
    val majorTickCount = 6 // Major ticks at 0, 20%, 40%, 60%, 80%, 100%
    val minorTickCount = 18 // Total minor ticks
    val sweepAngle = 180f
    val startAngle = 180f
    
    // Draw minor ticks
    for (i in 0..minorTickCount) {
        val angle = startAngle + (i * sweepAngle / minorTickCount)
        val tickRadius = radius * 0.88f
        val tickEndRadius = radius * 0.95f
        
        val startPos = Offset(
            center.x + tickRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            center.y + tickRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        
        val endPos = Offset(
            center.x + tickEndRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            center.y + tickEndRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = startPos,
            end = endPos,
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    
    // Draw major ticks
    for (i in 0..majorTickCount) {
        val angle = startAngle + (i * sweepAngle / majorTickCount)
        val tickRadius = radius * 0.82f
        val tickEndRadius = radius
        
        val startPos = Offset(
            center.x + tickRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            center.y + tickRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        
        val endPos = Offset(
            center.x + tickEndRadius * cos(Math.toRadians(angle.toDouble())).toFloat(),
            center.y + tickEndRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        )
        
        drawLine(
            color = Color.White,
            start = startPos,
            end = endPos,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}