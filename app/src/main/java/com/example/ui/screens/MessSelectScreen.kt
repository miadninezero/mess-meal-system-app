package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MessViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessSelectScreen(
    viewModel: MessViewModel,
    onSignOut: () -> Unit,
    onReturnToDashboard: (() -> Unit)? = null
) {
    val defaultUserName = remember { Firebase.auth.currentUser?.displayName ?: "" }
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val selectedMessId by viewModel.selectedMessId.collectAsState()
    val currentMess by viewModel.currentMess.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Create, 1: Join

    // Create state
    var messName by remember { mutableStateOf("") }
    var managerName by remember { mutableStateOf(defaultUserName) }

    // Join state
    var joinMessId by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf(defaultUserName) }
    var roomNumber by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mess Setup", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Mess Meal",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Create a mess as manager or join an existing flatmate mess group.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // If already associated with a mess, offer quick return
            if (selectedMessId.isNotBlank() && onReturnToDashboard != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Mess: ${currentMess?.name ?: selectedMessId.take(8)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        OutlinedButton(
                            onClick = onReturnToDashboard,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Go to Dashboard")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Create New Mess") },
                    icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Join Existing") },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // Create Mess Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "New Mess Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = messName,
                            onValueChange = { messName = it },
                            label = { Text("Mess / Flat Name") },
                            placeholder = { Text("e.g. Green View Bachelor Mess") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mess_name_input"),
                            singleLine = true,
                            enabled = !isSubmitting
                        )

                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Your Display Name (Manager)") },
                            placeholder = { Text("e.g. Tanvir (Room 302)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manager_name_input"),
                            singleLine = true,
                            enabled = !isSubmitting
                        )

                        Button(
                            onClick = {
                                if (messName.isNotBlank() && managerName.isNotBlank() && !isSubmitting) {
                                    viewModel.createMess(messName.trim(), managerName.trim())
                                }
                            },
                            enabled = messName.isNotBlank() && managerName.isNotBlank() && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("create_mess_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text("Create & Start Mess", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Join Mess Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Join with Mess Code",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = joinMessId,
                            onValueChange = { joinMessId = it },
                            label = { Text("Mess ID / Code") },
                            placeholder = { Text("Paste Mess ID from Manager") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("join_mess_id_input"),
                            singleLine = true,
                            enabled = !isSubmitting
                        )

                        OutlinedTextField(
                            value = memberName,
                            onValueChange = { memberName = it },
                            label = { Text("Your Full Name") },
                            placeholder = { Text("e.g. Rahim Ahmed") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_name_input"),
                            singleLine = true,
                            enabled = !isSubmitting
                        )

                        OutlinedTextField(
                            value = roomNumber,
                            onValueChange = { roomNumber = it },
                            label = { Text("Room / Bed Number (Optional)") },
                            placeholder = { Text("e.g. Room 4B") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("room_number_input"),
                            singleLine = true,
                            enabled = !isSubmitting
                        )

                        Button(
                            onClick = {
                                if (joinMessId.isNotBlank() && memberName.isNotBlank() && !isSubmitting) {
                                    viewModel.joinMess(joinMessId.trim(), memberName.trim(), roomNumber.trim())
                                }
                            },
                            enabled = joinMessId.isNotBlank() && memberName.isNotBlank() && !isSubmitting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("join_mess_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text("Join Mess", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
