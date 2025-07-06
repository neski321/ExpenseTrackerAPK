package com.neski.pennypincher.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import com.neski.pennypincher.data.repository.AuthRepository
import kotlinx.coroutines.launch
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    val colorScheme = MaterialTheme.colorScheme
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgGradient = if (isLight) {
        Brush.verticalGradient(listOf(colorScheme.primary.copy(alpha = 0.1f), colorScheme.primaryContainer))
    } else {
        Brush.verticalGradient(listOf(colorScheme.surface, colorScheme.background))
    }

    errorMsg?.let {
        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        errorMsg = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = "PennyPincher Icon",
                    tint = colorScheme.primary,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 12.dp)
                )

                Text("Welcome Back!", fontSize = 24.sp, color = colorScheme.primary)
                Text(
                    "Sign in to manage your finances with PennyPincher by Neski.",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("you@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (isResetting) {
                        LoadingSpinner(size = 16, showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TextButton(onClick = {
                        if (email.isBlank()) {
                            Toast.makeText(context, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show()
                        } else {
                            isResetting = true
                            scope.launch {
                                val result = AuthRepository.sendPasswordResetEmail(email)
                                isResetting = false
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to send reset email.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }) {
                        Text("Forgot Password?", color = colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val result = AuthRepository.signIn(email, password)
                                result
                                    .onSuccess { 
                                        // Explicitly refresh the session after successful login
                                        AuthRepository.refreshSessionAfterAuth()
                                        onLoginSuccess() 
                                    }
                                    .onFailure { errorMsg = it.message ?: "Login failed" }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        LoadingSpinner(size = 20, showText = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logging in...", color = getTextColor())
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log In", color = getTextColor())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToSignup) {
                    Text("Don't have an account? ", color = colorScheme.onSurface)
                    Text("Sign up here", color = colorScheme.primary)
                }
            }
        }
    }
}
