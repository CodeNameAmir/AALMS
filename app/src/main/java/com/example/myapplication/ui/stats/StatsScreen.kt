package com.example.myapplication.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.CategoryCount
import com.example.myapplication.data.DayCount
import com.example.myapplication.data.LevelCount
import com.example.myapplication.ui.logs.parseColor
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val totalLogs by viewModel.totalLogs.collectAsState()
    val levelCounts by viewModel.logCountsByLevel.collectAsState()
    val categoryCounts by viewModel.logCountsByCategory.collectAsState()
    val databaseMode by viewModel.databaseMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (databaseMode == com.example.myapplication.data.model.DatabaseMode.GOOGLE_CLOUD) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (databaseMode == com.example.myapplication.data.model.DatabaseMode.GOOGLE_CLOUD) Color(0xFF10B981).copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = if (databaseMode == com.example.myapplication.data.model.DatabaseMode.GOOGLE_CLOUD) 
                                    "Google Cloud" 
                                else 
                                    "Local SQLite",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (databaseMode == com.example.myapplication.data.model.DatabaseMode.GOOGLE_CLOUD) 
                                    Color(0xFF34D399) 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Total Logs", totalLogs.toString(), Modifier.weight(1f))
                StatCard("Categories", categoryCounts.size.toString(), Modifier.weight(1f))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val errorCount = levelCounts.find { it.level == "ERROR" }?.count ?: 0
                val critCount = levelCounts.find { it.level == "CRITICAL" }?.count ?: 0
                StatCard("Errors", errorCount.toString(), Modifier.weight(1f), color = rememberLogLevelColor("ERROR"))
                StatCard("Critical", critCount.toString(), Modifier.weight(1f), color = rememberLogLevelColor("CRITICAL"))
            }

            // Level Stats
            ChartCard(title = "Logs by Level") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    levelCounts.forEach { lc ->
                        LevelStatRow(lc.level, lc.count, totalLogs)
                    }
                }
            }

            // Category Stats
            ChartCard(title = "Logs by Category") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categoryCounts.forEach { cc ->
                        CategoryStatRow(cc, totalLogs)
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val displayColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label.uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
            Text(
                text = value,
                color = displayColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            content()
        }
    }
}

@Composable
fun LevelStatRow(level: String, count: Int, total: Int) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    val color = rememberLogLevelColor(level)
    
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(level, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
            Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun CategoryStatRow(cc: CategoryCount, total: Int) {
    val progress = if (total > 0) cc.count.toFloat() / total else 0f
    val color = parseColor(cc.categoryColor)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(8.dp))
            Text(cc.categoryName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp), overflow = TextOverflow.Ellipsis, maxLines = 1)
            Text(cc.count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
