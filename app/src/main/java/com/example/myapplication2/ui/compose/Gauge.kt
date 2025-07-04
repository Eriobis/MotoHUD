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
        modifier = modifier.size(120.dp),
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${animatedValue.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val strokeWidth = 16.dp.toPx()
    val center = size.center
    val radius = (size.minDimension / 2) - strokeWidth
    
    // Draw background arc
    drawArc(
        color = Color.Gray.copy(alpha = 0.5f),
        startAngle = 180f,
        sweepAngle = 150f,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )
    
    // Draw value arc
    val valueAngle = ((value - minValue) / (maxValue - minValue)) * 270f
    val valueColor = when {
        redlineValue != null && value >= redlineValue -> Color.Red
        value > maxValue * 0.8f -> Color.Yellow
        else -> Color.Green
    }
    
    drawArc(
        color = valueColor,
        startAngle = 135f,
        sweepAngle = valueAngle,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        topLeft = Offset(center.x - radius, center.y - radius)
    )
    
//    // Draw needle
//    val needleAngle = 135f + valueAngle
//    val needleLength = radius * 0.8f
//
//    rotate(degrees = needleAngle, pivot = center) {
//        drawLine(
//            color = Color.White,
//            start = center,
//            end = Offset(center.x, center.y - needleLength),
//            strokeWidth = 3.dp.toPx(),
//            cap = StrokeCap.Round
//        )
//    }
//
//    // Draw center circle
//    drawCircle(
//        color = Color.White,
//        radius = 8.dp.toPx(),
//        center = center
//    )
}