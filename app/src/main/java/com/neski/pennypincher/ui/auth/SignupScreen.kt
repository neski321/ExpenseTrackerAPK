package com.neski.pennypincher.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import com.neski.pennypincher.data.repository.AuthRepository
import com.neski.pennypincher.data.repository.UserServiceRepository
import kotlinx.coroutines.launch
import com.neski.pennypincher.ui.components.LoadingSpinner
import com.neski.pennypincher.ui.theme.getTextColor

@Composable
fun SignupScreen(onSignupSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val bgGradient = if (isLight) {
        Brush.verticalGradient(listOf(Color(0xFFEAF6FF), Color(0xFFBFD7ED)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF061D33), Color(0xFF0A2640)))
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
            colors = CardDefaults.cardColors(containerColor = if (isLight) Color.White else Color(0xFF112B45))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.AttachMoney,
                    contentDescription = "PennyPincher Logo",
                    tint = Color(0xFF50A8FF),
                    modifier = Modifier
                        .size(56.dp)
                        .padding(bottom = 12.dp)
                )

                Text("Create Your Account", fontSize = 24.sp, color = getTextColor())
                Text(
                    "Join PennyPincher by Neski and start managing your finances today.",
                    fontSize = 14.sp,
                    color = getTextColor(),
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
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

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        scope.launch {
                            if (password != confirmPassword) {
                                val msg = "Passwords do not match"
                                errorMsg = msg
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            isLoading = true
                            try {
                                val result = AuthRepository.signUp(email, password)
                                result
                                    .onSuccess { user ->
                                        // Explicitly refresh the session after successful signup
                                        AuthRepository.refreshSessionAfterAuth()
                                        
                                        // Seed default data for the new user
                                        val seedResult = UserServiceRepository.seedDefaultUserData(user.uid)
                                        seedResult.onSuccess {
                                            Toast.makeText(context, "Signup successful! Default data has been set up.", Toast.LENGTH_SHORT).show()
                                            onSignupSuccess()
                                        }.onFailure { seedError ->
                                            // Still proceed with signup even if seeding fails
                                            Toast.makeText(context, "Signup successful! Some default data may not have been set up.", Toast.LENGTH_SHORT).show()
                                            onSignupSuccess()
                                        }
                                    }
                                    .onFailure {
                                        val msg = it.message ?: "Signup failed"
                                        errorMsg = msg
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF50A8FF)),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()
                ) {
                    if (isLoading) {
                        LoadingSpinner(
                            size = 20,
                            showText = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Setting up account...", color = getTextColor())
                    } else {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Up", color = getTextColor())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onNavigateToLogin) {
                    Text(text = "Already have an account?", color = getTextColor())
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Log in here", color = MaterialTheme.colorScheme.primary)
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
