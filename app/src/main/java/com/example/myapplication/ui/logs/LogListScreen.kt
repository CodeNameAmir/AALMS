package com.example.myapplication.ui.logs

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.data.LogWithCategory
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.DatabaseMode
import com.example.myapplication.data.model.LogLevel
import com.example.myapplication.service.LogServerService
import com.example.myapplication.ui.theme.DarkBg
import com.example.myapplication.ui.theme.DarkBg2
import com.example.myapplication.ui.theme.DarkBg3
import com.example.myapplication.ui.theme.DarkBorder
import com.example.myapplication.ui.theme.getLogLevelColor
import com.example.myapplication.util.ImportExportUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogListScreen(
    viewModel: LogViewModel,
    onAddLogClick: () -> Unit,
    onLogClick: (LogWithCategory) -> Unit,
    onStatsClick: () -> Unit
) {
    val logs by viewModel.filteredLogs.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val filterPinned by viewModel.filterPinned.collectAsState()
    val filterHasFile by viewModel.filterHasFile.collectAsState()

    val context = LocalContext.current
    val databaseMode by viewModel.databaseMode.collectAsState()
    val googleUser by viewModel.googleUser.collectAsState()
    var showDatabaseDialog by remember { mutableStateOf(false) }

    var isServerRunning by remember { mutableStateOf(LogServerService.isRunning) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<com.example.myapplication.data.model.LogEntry?>(null) }

    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportLogs(context, it, "json") }
    }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportLogs(context, it, "csv") }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importLogs(context, it, "json") }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importLogs(context, it, "csv") }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startServer(context) { isServerRunning = it }
        }
    }

    fun toggleServer() {
        if (isServerRunning) {
            stopServer(context) { isServerRunning = false }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return
                }
            }
            startServer(context) { isServerRunning = it }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, color ->
                viewModel.insertCategory(Category(name = name, color = color))
                showAddCategoryDialog = false
            }
        )
    }

    if (showDeleteCategoryDialog) {
        DeleteCategoryDialog(
            categories = categories,
            onDismiss = { showDeleteCategoryDialog = false },
            onConfirm = { category ->
                viewModel.deleteCategory(category)
                showDeleteCategoryDialog = false
            }
        )
    }

    if (logToDelete != null) {
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Log") },
            text = { Text("Are you sure you want to delete this log entry? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        logToDelete?.let { viewModel.deleteLog(it) }
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkBg2,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showDatabaseDialog) {
        AlertDialog(
            onDismissRequest = { showDatabaseDialog = false },
            containerColor = DarkBg2,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF60A5FA) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Database Source", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Choose whether to store and query logs in local SQLite or Google Cloud Firestore.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    // Option 1: Local
                    Card(
                        onClick = {
                            viewModel.switchDatabaseMode(DatabaseMode.LOCAL)
                            showDatabaseDialog = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (databaseMode == DatabaseMode.LOCAL)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else DarkBg3
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (databaseMode == DatabaseMode.LOCAL) MaterialTheme.colorScheme.primary else DarkBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = databaseMode == DatabaseMode.LOCAL,
                                onClick = {
                                    viewModel.switchDatabaseMode(DatabaseMode.LOCAL)
                                    showDatabaseDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Local Database (SQLite)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("Stored on device (Room) • Offline", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Option 2: Google Cloud
                    Card(
                        onClick = {
                            viewModel.switchDatabaseMode(DatabaseMode.GOOGLE_CLOUD)
                            showDatabaseDialog = false
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (databaseMode == DatabaseMode.GOOGLE_CLOUD)
                                Color(0xFF1E3A8A).copy(alpha = 0.35f)
                            else DarkBg3
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF60A5FA) else DarkBorder
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = databaseMode == DatabaseMode.GOOGLE_CLOUD,
                                onClick = {
                                    viewModel.switchDatabaseMode(DatabaseMode.GOOGLE_CLOUD)
                                    showDatabaseDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Google Cloud (Firestore)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                val desc = if (googleUser != null) "Account: ${googleUser?.email}" else "Real-time Google Cloud Sync"
                                Text(desc, fontSize = 11.sp, color = Color(0xFF93C5FD))
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    Text("Database Sync Actions:", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.syncLocalToCloud(context)
                                showDatabaseDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                            border = BorderStroke(1.dp, Color(0xFF2563EB))
                        ) {
                            Text("Local ➔ Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.syncCloudToLocal(context)
                                showDatabaseDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Text("Cloud ➔ Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatabaseDialog = false }) {
                    Text("Done", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "LOG SERVER", 
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                fontSize = 16.sp
                            )
                            if (isServerRunning) {
                                val ip = LogServerService.getLocalIpAddress() ?: "0.0.0.0"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$ip:8080",
                                        fontSize = 11.sp,
                                        color = Color(0xFF86EFAC),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Database Mode Switcher Chip
                    Surface(
                        onClick = { showDatabaseDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF064E3B).copy(alpha = 0.4f) else DarkBg3,
                        border = BorderStroke(1.dp, if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF10B981) else DarkBorder),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                                contentDescription = "Database",
                                modifier = Modifier.size(14.dp),
                                tint = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF34D399) else Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) "Cloud" else "Local",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF34D399) else Color(0xFFE2E8F0)
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = { toggleServer() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isServerRunning) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF22C55E).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Server",
                            tint = if (isServerRunning) Color(0xFFF87171) else Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.LightGray)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(DarkBg2)
                        ) {
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF60A5FA) else Color.LightGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (databaseMode == DatabaseMode.GOOGLE_CLOUD) "Database: Google Cloud" else "Database: Local")
                                    }
                                },
                                onClick = { 
                                    menuExpanded = false
                                    showDatabaseDialog = true
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text("Statistics") },
                                onClick = { 
                                    menuExpanded = false
                                    onStatsClick()
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text(if (isServerRunning) "Stop Server" else "Start Server") },
                                onClick = { 
                                    menuExpanded = false
                                    toggleServer()
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text("Add Category") },
                                onClick = { 
                                    menuExpanded = false 
                                    showAddCategoryDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Category") },
                                onClick = { 
                                    menuExpanded = false
                                    showDeleteCategoryDialog = true
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text("Import JSON") },
                                onClick = { 
                                    menuExpanded = false
                                    importJsonLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import CSV") },
                                onClick = { 
                                    menuExpanded = false
                                    importCsvLauncher.launch(arrayOf(
                                        "text/csv", 
                                        "text/comma-separated-values", 
                                        "application/csv", 
                                        "application/vnd.ms-excel",
                                        "application/octet-stream",
                                        "*/*"
                                    ))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON") },
                                onClick = { 
                                    menuExpanded = false
                                    exportJsonLauncher.launch("logs_${System.currentTimeMillis()}.json")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = { 
                                    menuExpanded = false
                                    exportCsvLauncher.launch("logs_${System.currentTimeMillis()}.csv")
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text("Logout") },
                                onClick = { 
                                    menuExpanded = false
                                    viewModel.logout()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.LightGray
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddLogClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Log")
            }
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Active Database Banner
            Surface(
                color = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF064E3B).copy(alpha = 0.25f) else DarkBg2,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatabaseDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (databaseMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF34D399) else Color(0xFF60A5FA),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (databaseMode == DatabaseMode.GOOGLE_CLOUD)
                                    "Firestore Cloud Database"
                                else
                                    "Local SQLite Database",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (databaseMode == DatabaseMode.GOOGLE_CLOUD)
                                    (googleUser?.email ?: "Synced with Google Cloud")
                                else
                                    "Offline storage on this device",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Switch",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

            FilterSection(
                searchQuery = searchQuery,
                onSearchChange = viewModel::onSearchQueryChanged,
                selectedLevel = selectedLevel,
                onLevelChange = viewModel::onLevelSelected,
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategoryChange = viewModel::onCategorySelected,
                filterPinned = filterPinned,
                onFilterPinnedChange = viewModel::onFilterPinnedChanged,
                filterHasFile = filterHasFile,
                onFilterHasFileChange = viewModel::onFilterHasFileChanged,
                onClearFilters = viewModel::clearFilters
            )

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = DarkBg2,
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FilterListOff,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedLevel.isNotEmpty() || selectedCategoryId != 0L)
                                "No matching logs found"
                            else
                                "No logs in this database yet",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedLevel.isNotEmpty() || selectedCategoryId != 0L)
                                "Try resetting your search query or level filters"
                            else
                                "Tap the + button below or send logs via the web server",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(logs, key = { it.log.id }) { logWithCat ->
                        LogItem(
                            logWithCat = logWithCat,
                            onLogClick = { onLogClick(logWithCat) },
                            onPinClick = { viewModel.togglePin(logWithCat.log.id) },
                            onEditClick = { onLogClick(logWithCat) },
                            onDeleteClick = { logToDelete = logWithCat.log },
                            onDownloadClick = { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    try {
                                        // Extract original name from attach_timestamp_filename.ext
                                        val name = file.name
                                        val originalName = try {
                                            val parts = name.split("_")
                                            if (parts.size >= 3) {
                                                name.substringAfter(parts[1] + "_")
                                            } else name
                                        } catch (e: Exception) { name }

                                        val resolver = context.contentResolver
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, originalName)
                                            put(MediaStore.MediaColumns.MIME_TYPE, resolver.getType(android.net.Uri.fromFile(file)) ?: "application/octet-stream")
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                            }
                                        }
                                        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                        } else {
                                            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            val destFile = File(downloadDir, originalName)
                                            file.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                                            null
                                        }
                                        
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                                            resolver.openOutputStream(uri)?.use { output ->
                                                file.inputStream().use { input -> input.copyTo(output) }
                                            }
                                        }
                                        Toast.makeText(context, "Saved as $originalName to Downloads", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

private fun startServer(context: android.content.Context, onUpdate: (Boolean) -> Unit) {
    val intent = Intent(context, LogServerService::class.java)
    intent.action = LogServerService.ACTION_START
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    onUpdate(true)
}

private fun stopServer(context: android.content.Context, onUpdate: (Boolean) -> Unit) {
    val intent = Intent(context, LogServerService::class.java)
    intent.action = LogServerService.ACTION_STOP
    context.stopService(intent)
    onUpdate(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedLevel: String,
    onLevelChange: (String) -> Unit,
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategoryChange: (Long) -> Unit,
    filterPinned: Boolean,
    onFilterPinnedChange: (Boolean) -> Unit,
    filterHasFile: Boolean,
    onFilterHasFileChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg2)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Search (Full Width)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search logs by message…", fontSize = 13.sp, color = Color(0xFF64748B)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = DarkBg3,
                focusedContainerColor = DarkBg3,
                unfocusedBorderColor = DarkBorder,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary,
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White
            )
        )

        // Row 2: Level + Category Selectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Level Selector
            var levelMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { levelMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBg3,
                    border = BorderStroke(1.dp, if (selectedLevel.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (selectedLevel.isEmpty()) Color(0xFF64748B) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedLevel.isEmpty()) "All levels" else selectedLevel,
                                fontSize = 12.sp,
                                color = if (selectedLevel.isEmpty()) Color(0xFF94A3B8) else Color.White,
                                fontWeight = if (selectedLevel.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(
                    expanded = levelMenuExpanded,
                    onDismissRequest = { levelMenuExpanded = false },
                    modifier = Modifier.background(DarkBg2)
                ) {
                    DropdownMenuItem(
                        text = { Text("All levels") },
                        onClick = { onLevelChange(""); levelMenuExpanded = false }
                    )
                    LogLevel.all.forEach { level ->
                        val col = getLogLevelColor(level)
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(level, color = Color.White)
                                }
                            },
                            onClick = { onLevelChange(level); levelMenuExpanded = false }
                        )
                    }
                }
            }

            // Category Selector
            var categoryMenuExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    onClick = { categoryMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = DarkBg3,
                    border = BorderStroke(1.dp, if (selectedCategoryId != 0L) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val cat = categories.find { it.id == selectedCategoryId }
                        val catName = if (selectedCategoryId == 0L) "All categories" else cat?.name ?: "Unknown"
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = if (selectedCategoryId == 0L) Color(0xFF64748B) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = catName,
                                fontSize = 12.sp,
                                color = if (selectedCategoryId == 0L) Color(0xFF94A3B8) else Color.White,
                                fontWeight = if (selectedCategoryId == 0L) FontWeight.Normal else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.background(DarkBg2)
                ) {
                    DropdownMenuItem(
                        text = { Text("All categories") },
                        onClick = { onCategoryChange(0L); categoryMenuExpanded = false }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(parseColor(category.color)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category.name, color = Color.White)
                                }
                            },
                            onClick = { onCategoryChange(category.id); categoryMenuExpanded = false }
                        )
                    }
                }
            }
        }

        // Row 3: Filter Chips & Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pinned Chip
                FilterChip(
                    selected = filterPinned,
                    onClick = { onFilterPinnedChange(!filterPinned) },
                    label = { Text("Pinned", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (filterPinned) Color(0xFFFACC15) else Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF713F12).copy(alpha = 0.35f),
                        selectedLabelColor = Color(0xFFFDE047),
                        containerColor = DarkBg3,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = DarkBorder,
                        selectedBorderColor = Color(0xFFEAB308).copy(alpha = 0.6f),
                        enabled = true,
                        selected = filterPinned
                    )
                )

                // Has File Chip
                FilterChip(
                    selected = filterHasFile,
                    onClick = { onFilterHasFileChange(!filterHasFile) },
                    label = { Text("Has Attachment", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (filterHasFile) Color(0xFF60A5FA) else Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                        selectedLabelColor = Color(0xFF93C5FD),
                        containerColor = DarkBg3,
                        labelColor = Color(0xFF94A3B8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = DarkBorder,
                        selectedBorderColor = Color(0xFF3B82F6).copy(alpha = 0.6f),
                        enabled = true,
                        selected = filterHasFile
                    )
                )
            }

            if (searchQuery.isNotEmpty() || selectedLevel.isNotEmpty() || selectedCategoryId != 0L || filterPinned || filterHasFile) {
                TextButton(
                    onClick = onClearFilters,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.FilterAltOff, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LogItem(
    logWithCat: LogWithCategory,
    onLogClick: () -> Unit,
    onPinClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    val log = logWithCat.log
    val levelColor = getLogLevelColor(log.level)
    val catColor = parseColor(logWithCat.categoryColor)
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable(onClick = onLogClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (log.pinned) Color(0xFFEAB308).copy(alpha = 0.4f) else DarkBorder),
        colors = CardDefaults.cardColors(
            containerColor = if (log.pinned) Color(0xFF1E1C11) else DarkBg2
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Accent Border
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (log.pinned) Color(0xFFEAB308) else levelColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Header: Level and Category Badges + Actions Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Level Badge
                    Surface(
                        color = levelColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, levelColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = log.level,
                            color = levelColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Category Badge
                    Surface(
                        color = catColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = logWithCat.categoryName,
                            color = catColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Actions Menu (3 dots)
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Actions", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(DarkBg2)
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (log.pinned) "Unpin" else "Pin") },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    onPinClick()
                                    menuExpanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    onEditClick()
                                    menuExpanded = false 
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Log Message", log.message)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    menuExpanded = false 
                                }
                            )
                            HorizontalDivider(color = DarkBorder)
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp)) },
                                onClick = { 
                                    onDeleteClick()
                                    menuExpanded = false 
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message Area
                Surface(
                    color = DarkBg3,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, DarkBorder.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            if (log.pinned) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = log.message,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Footer: Time & Attachment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (log.attachmentPath != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { onDownloadClick(log.attachmentPath) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Attachment", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = fmtTimeRelative(log.createdAt),
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private fun fmtTimeRelative(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#3b82f6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("Color (Hex)") },
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Preview: ", fontSize = 12.sp)
                    Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(parseColor(color)))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, color) },
                enabled = name.isNotBlank() && color.startsWith("#")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkBg2,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun DeleteCategoryDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select a category to delete. Warning: Logs with this category may become inaccessible.")
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedCategory?.name ?: "Select Category")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(DarkBg2)
                    ) {
                        categories.filter { it.userId != 0L }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedCategory?.let { onConfirm(it) } },
                enabled = selectedCategory != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkBg2,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (_: Exception) {
        Color.Gray
    }
}
