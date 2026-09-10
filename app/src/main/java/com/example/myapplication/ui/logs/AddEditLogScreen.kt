package com.example.myapplication.ui.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Delete
import com.example.myapplication.data.model.LogEntry
import com.example.myapplication.data.model.LogLevel
import com.example.myapplication.ui.theme.DarkBg
import com.example.myapplication.ui.theme.DarkBg2
import com.example.myapplication.ui.theme.DarkBg3
import com.example.myapplication.ui.theme.DarkBorder
import com.example.myapplication.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLogScreen(
    viewModel: LogViewModel,
    logId: Long? = null,
    onBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val logs by viewModel.filteredLogs.collectAsState()
    
    val existingLog = remember(logId, logs) {
        if (logId != null) logs.find { it.log.id == logId }?.log else null
    }

    var message by remember(existingLog) { mutableStateOf(existingLog?.message ?: "") }
    var level by remember(existingLog) { mutableStateOf(existingLog?.level ?: LogLevel.INFO) }
    
    // Use a regular mutableStateOf for the ID to ensure proper reactivity
    var selectedCategoryId by remember(existingLog) {
        mutableLongStateOf(existingLog?.categoryId ?: 0L)
    }

    // Set initial category once they are available, but only if not already set by user
    val categoriesLoaded = categories.isNotEmpty()
    LaunchedEffect(categoriesLoaded) {
        if (selectedCategoryId == 0L && categories.isNotEmpty()) {
            val generalCat = categories.find { it.name.equals("General", ignoreCase = true) }
            selectedCategoryId = generalCat?.id ?: categories.first().id
        }
    }
    
    var pinned by remember(existingLog) { mutableStateOf(existingLog?.pinned ?: false) }
    var attachmentPath by remember(existingLog) { mutableStateOf(existingLog?.attachmentPath) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val userId by viewModel.userId.collectAsState()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            attachmentPath = FileUtils.saveFileToInternal(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (logId == null) "New Log" else "Edit Log", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.LightGray
                )
            )
        },
        containerColor = DarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("MESSAGE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("Enter log message…", color = Color(0xFF64748B)) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DarkBorder,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = DarkBg3,
                    focusedContainerColor = DarkBg3,
                    unfocusedTextColor = Color.White,
                    focusedTextColor = Color.White,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("LEVEL", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    var levelMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { levelMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = DarkBg3,
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(level, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                        DropdownMenu(
                            expanded = levelMenuExpanded,
                            onDismissRequest = { levelMenuExpanded = false },
                            modifier = Modifier.background(DarkBg2)
                        ) {
                            LogLevel.all.forEach { lvl ->
                                DropdownMenuItem(
                                    text = { Text(lvl, color = Color.White) },
                                    onClick = { level = lvl; levelMenuExpanded = false }
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("CATEGORY", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    var categoryMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { categoryMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = DarkBg3,
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentCat = categories.find { it.id == selectedCategoryId }
                                Text(currentCat?.name ?: "Select", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF94A3B8))
                            }
                        }
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false },
                            modifier = Modifier.background(DarkBg2)
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name, color = Color.White) },
                                    onClick = { 
                                        selectedCategoryId = cat.id
                                        categoryMenuExpanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkBg3,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pin this log to top", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Attachment Field
            Text("ATTACHMENT", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkBg3)
                ) {
                    Text(if (attachmentPath != null) "Change File" else "Attach File", color = Color.White)
                }
                
                if (attachmentPath != null) {
                    IconButton(
                        onClick = { attachmentPath = null },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Remove File")
                    }
                }
            }

            if (attachmentPath != null) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "File: ${attachmentPath?.substringAfterLast("/")}",
                        color = Color(0xFF86EFAC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val log = LogEntry(
                        id = logId ?: 0L,
                        message = message,
                        level = level,
                        categoryId = selectedCategoryId,
                        userId = userId ?: 0L,
                        pinned = pinned,
                        attachmentPath = attachmentPath,
                        updatedAt = System.currentTimeMillis()
                    )
                    if (logId == null) viewModel.insertLog(log) else viewModel.updateLog(log)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = message.isNotBlank() && selectedCategoryId != 0L
            ) {
                Text(if (logId == null) "Create Log" else "Save Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
