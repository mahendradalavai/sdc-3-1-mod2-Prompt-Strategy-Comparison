package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AggregateStrategyMetric
import com.example.data.model.ComplexityLevel
import com.example.data.model.PromptStrategy
import com.example.ui.theme.FewShotColor
import com.example.ui.theme.HighComplexityColor
import com.example.ui.theme.LowComplexityColor
import com.example.ui.theme.MediumComplexityColor
import com.example.ui.theme.StructuredColor
import com.example.ui.theme.ZeroShotColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * Grouped Bar Chart comparing Zero-Shot vs Few-Shot vs Structured across Complexity levels
 */
@Composable
fun ComplexityComparisonBarChart(
    metrics: List<AggregateStrategyMetric>,
    metricType: MetricChartType = MetricChartType.ACCURACY,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(metricType, metrics) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val complexities = ComplexityLevel.values().toList()
    val strategies = PromptStrategy.values().toList()

    val maxVal = if (metricType == MetricChartType.ACCURACY) 100f else {
        var highest = 1200f
        metrics.forEach { m ->
            m.latencyByComplexity.values.forEach { lat ->
                if (lat.toFloat() > highest) highest = lat.toFloat() * 1.15f
            }
        }
        highest
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("complexity_comparison_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (metricType == MetricChartType.ACCURACY) "Accuracy (%) by Complexity" else "Latency (ms) by Complexity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    strategies.forEach { strat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(strat.color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strat.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 30f // Leave space for bottom labels
                val leftAxisMargin = 40f
                val chartAreaWidth = canvasWidth - leftAxisMargin

                // Draw horizontal guide lines
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = canvasHeight * (1f - (i.toFloat() / gridSteps))
                    drawLine(
                        color = gridColor,
                        start = Offset(leftAxisMargin, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw bars
                val groupWidth = chartAreaWidth / complexities.size
                val barWidth = (groupWidth * 0.22f)
                val barSpacing = groupWidth * 0.05f

                complexities.forEachIndexed { groupIndex, comp ->
                    val groupStartX = leftAxisMargin + (groupIndex * groupWidth) + (groupWidth * 0.1f)

                    strategies.forEachIndexed { stratIndex, strat ->
                        val metric = metrics.firstOrNull { it.strategy == strat }
                        val rawValue = if (metricType == MetricChartType.ACCURACY) {
                            metric?.accuracyByComplexity?.get(comp) ?: (if (strat == PromptStrategy.STRUCTURED) 96f else if (strat == PromptStrategy.FEW_SHOT) 90f else 75f)
                        } else {
                            (metric?.latencyByComplexity?.get(comp) ?: (if (strat == PromptStrategy.STRUCTURED) 750L else if (strat == PromptStrategy.FEW_SHOT) 900L else 450L)).toFloat()
                        }

                        val barHeight = (rawValue / maxVal).coerceIn(0.05f, 1f) * canvasHeight * animProgress.value
                        val barX = groupStartX + (stratIndex * (barWidth + barSpacing))
                        val barY = canvasHeight - barHeight

                        drawRoundRect(
                            color = strat.color,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // X-Axis Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                complexities.forEach { comp ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = comp.title.replace(" Complexity", ""),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = comp.color
                        )
                    }
                }
            }
        }
    }
}

enum class MetricChartType {
    ACCURACY,
    LATENCY
}

/**
 * Metric summary card for each Strategy (Zero-shot, Few-shot, Structured)
 */
@Composable
fun StrategyMetricCard(
    metric: AggregateStrategyMetric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("strategy_card_${metric.strategy.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(metric.strategy.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = metric.strategy.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = metric.strategy.color.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, metric.strategy.color.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${metric.avgAccuracy.toInt()}% Avg Acc",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = metric.strategy.color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = metric.strategy.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Stat pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    icon = Icons.Default.Speed,
                    label = "Latency",
                    value = "${metric.avgLatencyMs} ms",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Default.DataArray,
                    label = "Schema",
                    value = "${metric.schemaComplianceRate.toInt()}% Valid",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = Icons.Default.Bolt,
                    label = "Tokens",
                    value = "${metric.avgInputTokens + metric.avgOutputTokens} avg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Radar / Multi-Dimension Spider Chart
 */
@Composable
fun StrategyRadarChart(
    metrics: List<AggregateStrategyMetric>,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Accuracy", "Speed", "Schema Validity", "Token Efficiency", "Complex Reasoning")
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("strategy_radar_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Multi-Dimensional Strategy Scorecard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Evaluates trade-offs across 5 core LLM operational dimensions",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width * 0.38f
                    val angleStep = (2 * Math.PI / categories.size).toFloat()

                    // Draw concentric rings
                    for (level in 1..4) {
                        val levelRadius = radius * (level / 4f)
                        val path = Path()
                        for (i in categories.indices) {
                            val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                            val x = center.x + levelRadius * cos(angle)
                            val y = center.y + levelRadius * sin(angle)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, color = gridColor, style = Stroke(width = 1.dp.toPx()))
                    }

                    // Draw radial spokes
                    for (i in categories.indices) {
                        val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                        val x = center.x + radius * cos(angle)
                        val y = center.y + radius * sin(angle)
                        drawLine(color = gridColor, start = center, end = Offset(x, y), strokeWidth = 1.dp.toPx())
                    }

                    // Draw polygons for each strategy
                    val strategies = listOf(PromptStrategy.ZERO_SHOT, PromptStrategy.FEW_SHOT, PromptStrategy.STRUCTURED)
                    strategies.forEach { strat ->
                        val (acc, spd, sch, eff, reas) = when (strat) {
                            PromptStrategy.ZERO_SHOT -> listOf(0.72f, 0.95f, 0.40f, 0.90f, 0.55f)
                            PromptStrategy.FEW_SHOT -> listOf(0.91f, 0.65f, 0.88f, 0.50f, 0.82f)
                            PromptStrategy.STRUCTURED -> listOf(0.98f, 0.72f, 0.99f, 0.75f, 0.96f)
                        }

                        val values = listOf(acc, spd, sch, eff, reas)
                        val polyPath = Path()

                        values.forEachIndexed { i, factor ->
                            val angle = (i * angleStep) - (Math.PI / 2).toFloat()
                            val r = radius * factor
                            val x = center.x + r * cos(angle)
                            val y = center.y + r * sin(angle)
                            if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
                        }
                        polyPath.close()

                        // Fill translucent
                        drawPath(polyPath, color = strat.color.copy(alpha = 0.16f))
                        // Stroke border
                        drawPath(polyPath, color = strat.color, style = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }

            // Legend below radar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PromptStrategy.values().forEach { strat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(strat.color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strat.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
