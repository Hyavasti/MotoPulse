package com.example.motorapp.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motorapp.R

// High-contrast, production palette
private val LeafGreen = Color(0xFF16A34A)
private val DarkGreenBg = Color(0xFF0F5A28)
private val LightCardBg = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF0F172A)
private val TextSubtle = Color(0xFF475569)
private val InputBorderColor = Color(0xFFCBD5E1)
private val ErrorRed = Color(0xFFDC2626)

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val state by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var isSignUp by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Remember Me state
    var rememberMe by remember { mutableStateOf(authViewModel.isRememberMeEnabled()) }

    // Inline errors
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var showVerificationModal by remember { mutableStateOf(false) }
    var verificationTargetEmail by remember { mutableStateOf("") }

    // Auto-populate saved username if Remember Me is active
    LaunchedEffect(Unit) {
        if (rememberMe) {
            username = authViewModel.getSavedUsername()
        }
    }

    // Handle authentication state changes & route errors to specific fields
    LaunchedEffect(state) {
        when (state) {
            is AuthUiState.Success -> onAuthSuccess()
            is AuthUiState.EmailVerificationRequired -> {
                verificationTargetEmail = (state as AuthUiState.EmailVerificationRequired).email
                showVerificationModal = true
            }
            is AuthUiState.Error -> {
                val errorMsg = (state as AuthUiState.Error).message
                when {
                    errorMsg.contains("username", ignoreCase = true) -> {
                        usernameError = errorMsg
                        generalError = null
                    }
                    errorMsg.contains("email", ignoreCase = true) -> {
                        emailError = errorMsg
                        generalError = null
                    }
                    errorMsg.contains("password", ignoreCase = true) -> {
                        passwordError = errorMsg
                        generalError = null
                    }
                    else -> {
                        generalError = errorMsg
                    }
                }
            }
            else -> {
                generalError = null
            }
        }
    }

    // Reset errors and fields when switching between Sign Up and Log In
    LaunchedEffect(isSignUp) {
        usernameError = null
        emailError = null
        phoneError = null
        passwordError = null
        confirmPasswordError = null
        generalError = null
        authViewModel.resetState()
    }

    fun submitForm() {
        focusManager.clearFocus()
        generalError = null

        if (isSignUp) {
            var isValid = true

            val cleanUsername = username.trim()
            if (cleanUsername.length < 3) {
                usernameError = "Username must be at least 3 characters."
                isValid = false
            } else {
                usernameError = null
            }

            val cleanEmail = email.trim()
            if (cleanEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                emailError = "Please enter a valid email address."
                isValid = false
            } else {
                emailError = null
            }

            val cleanPhone = contactNumber.trim()
            if (cleanPhone.length != 11 || !cleanPhone.startsWith("09")) {
                phoneError = "Must be an 11-digit number starting with 09."
                isValid = false
            } else {
                phoneError = null
            }

            val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[0-9])(?=.*[@$!%*?&#^()_+\\-=\\[\\]{}|;:,.<>/~`]).{8,16}$")
            if (!password.matches(passwordRegex)) {
                passwordError = "8-16 chars, 1 uppercase, 1 digit & 1 symbol required."
                isValid = false
            } else {
                passwordError = null
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordError = "Please confirm your password."
                isValid = false
            } else if (confirmPassword != password) {
                confirmPasswordError = "Passwords do not match."
                isValid = false
            } else {
                confirmPasswordError = null
            }

            if (isValid) {
                authViewModel.signUp(
                    username = cleanUsername,
                    email = cleanEmail,
                    phoneNumber = cleanPhone,
                    pass = password
                )
            }
        } else {
            var isValid = true
            val cleanUsername = username.trim()
            if (cleanUsername.isBlank()) {
                usernameError = "Username is required."
                isValid = false
            } else {
                usernameError = null
            }

            if (password.isBlank()) {
                passwordError = "Password is required."
                isValid = false
            } else {
                passwordError = null
            }

            if (isValid) {
                authViewModel.signIn(
                    username = cleanUsername,
                    pass = password,
                    rememberMe = rememberMe
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSignUp) DarkGreenBg else LeafGreen)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // LEFT TAB RAIL
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                VerticalTabItem(
                    text = "Log in",
                    isSelected = !isSignUp,
                    onClick = { isSignUp = false }
                )

                Spacer(modifier = Modifier.height(48.dp))

                VerticalTabItem(
                    text = "Sign Up",
                    isSelected = isSignUp,
                    onClick = { isSignUp = true }
                )
            }

            // RIGHT MAIN SHEET
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)),
                color = LightCardBg
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Pinned Close / Clear Focus button
                    IconButton(
                        onClick = { focusManager.clearFocus() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark)
                    }

                    // Content Container
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = if (isSignUp) Arrangement.Top else Arrangement.Center
                    ) {
                        if (isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // App Logo
                        Image(
                            painter = painterResource(id = R.drawable.motopulse),
                            contentDescription = "MotoPulse Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(130.dp)
                                .padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Title
                        Text(
                            text = if (isSignUp) "Create Account" else "Welcome Back",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // ================= FORM INPUTS =================

                        // 1. Username
                        ReferenceDividedInput(
                            value = username,
                            onValueChange = {
                                username = it
                                usernameError = null
                                generalError = null
                            },
                            placeholder = "Username",
                            icon = Icons.Default.Person,
                            imeAction = ImeAction.Next,
                            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                            isError = usernameError != null
                        )
                        InlineErrorText(error = usernameError)

                        // 2. Email (Sign Up only)
                        if (isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ReferenceDividedInput(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = null
                                    generalError = null
                                },
                                placeholder = "Email",
                                icon = Icons.Default.Email,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                                isError = emailError != null
                            )
                            InlineErrorText(error = emailError)
                        }

                        // 3. Contact Number (Sign Up only)
                        if (isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ReferenceDividedInput(
                                value = contactNumber,
                                onValueChange = { input ->
                                    if (input.length <= 11 && input.all { it.isDigit() }) {
                                        contactNumber = input
                                        phoneError = null
                                        generalError = null
                                    }
                                },
                                placeholder = "Phone number (e.g. 09123456789)",
                                icon = Icons.Default.Phone,
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next,
                                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                                isError = phoneError != null
                            )
                            InlineErrorText(error = phoneError)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Password
                        ReferenceDividedInput(
                            value = password,
                            onValueChange = {
                                password = it
                                passwordError = null
                                generalError = null
                            },
                            placeholder = "Password",
                            icon = Icons.Default.Lock,
                            keyboardType = KeyboardType.Password,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done,
                            onImeAction = {
                                if (isSignUp) {
                                    focusManager.moveFocus(FocusDirection.Down)
                                } else {
                                    submitForm()
                                }
                            },
                            isError = passwordError != null
                        )
                        if (isSignUp && passwordError == null) {
                            Text(
                                text = "8-16 chars, 1 uppercase, 1 digit & 1 symbol",
                                fontSize = 11.sp,
                                color = TextSubtle,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                        InlineErrorText(error = passwordError)

                        // 5. Confirm Password (Sign Up only)
                        if (isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ReferenceDividedInput(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    confirmPasswordError = null
                                    generalError = null
                                },
                                placeholder = "Confirm Password",
                                icon = Icons.Default.Lock,
                                keyboardType = KeyboardType.Password,
                                isPassword = true,
                                passwordVisible = confirmPasswordVisible,
                                onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible },
                                imeAction = ImeAction.Done,
                                onImeAction = { submitForm() },
                                isError = confirmPasswordError != null
                            )
                            InlineErrorText(error = confirmPasswordError)
                        }

                        // 6. Remember Me Checkbox (Log In only)
                        if (!isSignUp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { rememberMe = !rememberMe }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LeafGreen,
                                        uncheckedColor = InputBorderColor,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remember me",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                            }
                        }

                        // General System Error (fallback when error is not tied to a single field)
                        AnimatedVisibility(
                            visible = generalError != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            generalError?.let { msg ->
                                Text(
                                    text = msg,
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action Button
                        Button(
                            onClick = { submitForm() },
                            enabled = state !is AuthUiState.Loading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LeafGreen,
                                disabledContainerColor = LeafGreen.copy(alpha = 0.85f)
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(50.dp)
                        ) {
                            if (state is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = if (isSignUp) "SIGN UP" else "SIGN IN",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom toggle link
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                                fontSize = 13.sp,
                                color = TextSubtle
                            )
                            Text(
                                text = if (isSignUp) "Log in" else "Sign up",
                                fontSize = 13.sp,
                                color = LeafGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { isSignUp = !isSignUp }
                            )
                        }
                    }
                }
            }
        }
    }

    // EMAIL VERIFICATION MODAL
    if (showVerificationModal) {
        AlertDialog(
            onDismissRequest = {
                showVerificationModal = false
                authViewModel.resetState()
                isSignUp = false
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LeafGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Verification",
                        tint = LeafGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Verify Your Email",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "A confirmation link has been sent to:\n$verificationTargetEmail\n\nPlease check your inbox and verify your email before logging in.",
                    color = TextSubtle,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showVerificationModal = false
                        authViewModel.resetState()
                        isSignUp = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Proceed to Log In", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun InlineErrorText(error: String?) {
    AnimatedVisibility(
        visible = error != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 3.dp)
            )
        }
    }
}

@Composable
private fun VerticalTabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.height, placeable.width) {
                    placeable.placeRelativeWithLayer(
                        x = (placeable.height - placeable.width) / 2,
                        y = (placeable.width - placeable.height) / 2
                    ) {
                        rotationZ = -90f
                    }
                }
            }
        )
    }
}

@Composable
private fun ReferenceDividedInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    isError: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextSubtle, fontSize = 14.sp) },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) ErrorRed else TextDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.5.dp)
                        .background(if (isError) ErrorRed else InputBorderColor)
                )
            }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Password",
                        tint = if (isError) ErrorRed else TextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = { onImeAction() },
            onDone = { onImeAction() }
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextDark,
            unfocusedTextColor = TextDark,
            focusedBorderColor = if (isError) ErrorRed else LeafGreen,
            unfocusedBorderColor = if (isError) ErrorRed else InputBorderColor,
            errorBorderColor = ErrorRed,
            cursorColor = LeafGreen,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}