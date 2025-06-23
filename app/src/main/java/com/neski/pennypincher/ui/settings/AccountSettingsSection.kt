package com.neski.pennypincher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.neski.pennypincher.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun AccountSettingsSection() {
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val currentEmail = currentUser?.email ?: "No Email"

    var showForm by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var confirmEmail by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SnackbarHost(hostState = snackbarHostState)

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Account Settings", style = MaterialTheme.typography.titleMedium)
                Text("Manage your account details.")
                Text("Current Email: $currentEmail")

                if (showForm) {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmEmail,
                        onValueChange = { confirmEmail = it },
                        label = { Text("Confirm New Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newEmail != confirmEmail) {
                                message = "Emails do not match."
                                return@Button
                            }
                            scope.launch {
                                if (newEmail != confirmEmail) {
                                    snackbarHostState.showSnackbar("Emails do not match.")
                                    return@launch
                                }

                                val reauth = AuthRepository.reauthenticateUser(currentEmail, currentPassword)
                                if (reauth.isFailure) {
                                    snackbarHostState.showSnackbar("Reauthentication failed: ${reauth.exceptionOrNull()?.message}")
                                    return@launch
                                }

                                val update = AuthRepository.updateEmailAddress(newEmail)
                                if (update.isFailure) {
                                    snackbarHostState.showSnackbar("Update failed: ${update.exceptionOrNull()?.message}")
                                } else {
                                    snackbarHostState.showSnackbar("Email updated successfully!")
                                    showForm = false
                                    newEmail = ""
                                    confirmEmail = ""
                                    currentPassword = ""
                                }
                            }


                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newEmail.isNotBlank() && confirmEmail.isNotBlank() && currentPassword.isNotBlank()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Update")
                        Spacer(Modifier.width(8.dp))
                        Text("Update Email Address")
                    }
                } else {
                    Button(onClick = { showForm = true }) {
                        Icon(Icons.Default.Email, contentDescription = "Update")
                        Spacer(Modifier.width(8.dp))
                        Text("Update Email Address")
                    }
                }

                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
