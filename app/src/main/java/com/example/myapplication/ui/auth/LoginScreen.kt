package com.example.myapplication.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.DatabaseMode
import com.example.myapplication.ui.theme.DarkBg2
import com.example.myapplication.ui.theme.DarkBg3
import com.example.myapplication.ui.theme.DarkBorder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }
    var googleEmailInput by remember { mutableStateOf("amir.mohammad.salimi.helli@gmail.com") }
    var googleNameInput by remember { mutableStateOf("Amir Mohammad") }

    val currentDbMode by viewModel.databaseMode.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                if (account != null && !account.email.isNullOrBlank()) {
                    viewModel.loginWithGoogle(
                        idToken = account.idToken,
                        email = account.email!!,
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString(),
                        preferCloudDb = (currentDbMode == DatabaseMode.GOOGLE_CLOUD),
                        onSuccess = onLoginSuccess
                    )
                    return@rememberLauncherForActivityResult
                }
            } catch (e: Exception) {
                // Fallback to dialog helper
            }
        }
        showGoogleDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Icon Header
        Surface(
            modifier = Modifier.size(68.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isRegister) "Create Account" else "Log Server Pro",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "High-performance logging & cross-device inspection",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp)
        )

        // Database Mode Choice - Elegant Segmented Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBg2),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORAGE ENGINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (currentDbMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF3B82F6).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (currentDbMode == DatabaseMode.GOOGLE_CLOUD) "Cloud Active" else "Offline Ready",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentDbMode == DatabaseMode.GOOGLE_CLOUD) Color(0xFF34D399) else Color(0xFF60A5FA),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Local DB button
                    val isLocal = currentDbMode == DatabaseMode.LOCAL
                    Surface(
                        onClick = { viewModel.setDatabaseMode(DatabaseMode.LOCAL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLocal) Color(0xFF1E293B) else DarkBg3,
                        border = BorderStroke(
                            if (isLocal) 1.5.dp else 1.dp,
                            if (isLocal) Color(0xFF60A5FA) else DarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isLocal) Color(0xFF60A5FA) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Local SQLite",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLocal) Color.White else Color(0xFF94A3B8)
                                )
                                Text("Offline", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Google Cloud button
                    val isCloud = currentDbMode == DatabaseMode.GOOGLE_CLOUD
                    Surface(
                        onClick = { viewModel.setDatabaseMode(DatabaseMode.GOOGLE_CLOUD) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCloud) Color(0xFF064E3B).copy(alpha = 0.35f) else DarkBg3,
                        border = BorderStroke(
                            if (isCloud) 1.5.dp else 1.dp,
                            if (isCloud) Color(0xFF10B981) else DarkBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isCloud) Color(0xFF34D399) else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Firestore",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCloud) Color.White else Color(0xFF94A3B8)
                                )
                                Text("Cloud Sync", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Google Sign-In Button
        Button(
            onClick = {
                try {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                } catch (e: Exception) {
                    showGoogleDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1E293B)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            enabled = !loading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
            Text(
                text = "OR SIGN IN WITH USERNAME",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = DarkBorder,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = DarkBg2,
                focusedContainerColor = DarkBg2,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = DarkBorder,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = DarkBg2,
                focusedContainerColor = DarkBg2,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (error != null) {
            Surface(
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = error!!,
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                if (isRegister) {
                    viewModel.register(username, password, onLoginSuccess)
                } else {
                    viewModel.login(username, password, onLoginSuccess)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = username.isNotBlank() && password.length >= 4 && !loading
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
            } else {
                Text(
                    if (isRegister) "Create Account" else "Sign In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        TextButton(
            onClick = { isRegister = !isRegister },
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text(
                if (isRegister) "Already have an account? Sign in" else "Don't have an account? Register",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Google Sign-In Prompt / Fast Sign-In Dialog
    if (showGoogleDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleDialog = false },
            containerColor = DarkBg2,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF4285F4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign in with Google", color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sign in to activate Google Cloud Firestore storage for your logs.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = googleEmailInput,
                        onValueChange = { googleEmailInput = it },
                        label = { Text("Google Account Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedContainerColor = DarkBg3,
                            focusedContainerColor = DarkBg3
                        )
                    )
                    OutlinedTextField(
                        value = googleNameInput,
                        onValueChange = { googleNameInput = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedContainerColor = DarkBg3,
                            focusedContainerColor = DarkBg3
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoogleDialog = false
                        viewModel.loginWithGoogle(
                            idToken = null,
                            email = googleEmailInput.trim(),
                            displayName = googleNameInput.trim(),
                            photoUrl = null,
                            preferCloudDb = true,
                            onSuccess = onLoginSuccess
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    enabled = googleEmailInput.contains("@")
                ) {
                    Text("Connect Google Cloud", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

