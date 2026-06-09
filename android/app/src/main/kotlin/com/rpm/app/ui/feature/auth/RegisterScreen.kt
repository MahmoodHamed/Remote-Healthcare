package com.rpm.app.ui.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onRegisterSuccess()
    }

    var fullName       by remember { mutableStateOf("") }
    var email          by remember { mutableStateOf("") }
    var phone          by remember { mutableStateOf("") }
    var password       by remember { mutableStateOf("") }
    var showPassword   by remember { mutableStateOf(false) }
    var licenseNumber  by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("General") }
    var expanded       by remember { mutableStateOf(false) }
    var role           by remember { mutableStateOf("Doctor") }
    val roles          = listOf("Doctor", "Patient", "Relative")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Personal Information ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader(text = "Personal Information", icon = Icons.Default.Person)

                    OutlinedTextField(
                        value         = fullName,
                        onValueChange = { fullName = it },
                        label         = { Text("Full Name") },
                        leadingIcon   = { Icon(Icons.Default.AccountCircle, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                    )

                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Email address") },
                        leadingIcon   = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                    )

                    OutlinedTextField(
                        value         = phone,
                        onValueChange = { phone = it },
                        label         = { Text("Phone number") },
                        leadingIcon   = { Icon(Icons.Default.Phone, null) },
                        placeholder   = { Text("+966501234567") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction    = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                    )

                    OutlinedTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = { Text("Password") },
                        leadingIcon   = { Icon(Icons.Default.Lock, null) },
                        trailingIcon  = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector        = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide" else "Show",
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        singleLine    = true,
                        supportingText = { Text("Minimum 6 characters") },
                    )
                }
            }

            // ── Account Type ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier            = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionHeader(text = "Account Type", icon = Icons.Default.Badge)

                    ExposedDropdownMenuBox(
                        expanded          = expanded,
                        onExpandedChange  = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value         = roleLabelFor(role),
                            onValueChange = {},
                            readOnly      = true,
                            label         = { Text("Role") },
                            leadingIcon   = { Icon(roleIconFor(role), null) },
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier      = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape         = RoundedCornerShape(12.dp),
                        )
                        ExposedDropdownMenu(
                            expanded         = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            roles.forEach { r ->
                                DropdownMenuItem(
                                    text    = { Text(roleLabelFor(r)) },
                                    onClick = { role = r; expanded = false },
                                    leadingIcon = { Icon(roleIconFor(r), null) },
                                )
                            }
                        }
                    }

                    if (role == "Doctor") {
                        OutlinedTextField(
                            value         = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label         = { Text("License Number") },
                            leadingIcon   = { Icon(Icons.Default.Badge, null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            singleLine    = true,
                        )
                        OutlinedTextField(
                            value         = specialization,
                            onValueChange = { specialization = it },
                            label         = { Text("Specialization") },
                            leadingIcon   = { Icon(Icons.Default.LocalHospital, null) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(12.dp),
                            singleLine    = true,
                        )
                    }
                }
            }

            // ── Server Error ──────────────────────────────────────────────
            uiState.error?.let { err ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Error,
                            null,
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.register(
                        email          = email.trim(),
                        password       = password,
                        fullName       = fullName.trim(),
                        phone          = phone.trim(),
                        role           = role,
                        licenseNumber  = licenseNumber.trim().ifBlank { null },
                        specialization = specialization.trim().ifBlank { null },
                    )
                },
                enabled  = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Create Account", style = MaterialTheme.typography.labelLarge)
                }
            }

            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Already have an account?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text  = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
}

private fun roleLabelFor(role: String) = when (role) {
    "Doctor"   -> "Doctor"
    "Patient"  -> "Patient"
    "Relative" -> "Relative / Family"
    else       -> role
}

private fun roleIconFor(role: String) = when (role) {
    "Doctor"   -> Icons.Default.LocalHospital
    "Patient"  -> Icons.Default.Person
    "Relative" -> Icons.Default.People
    else       -> Icons.Default.Person
}
