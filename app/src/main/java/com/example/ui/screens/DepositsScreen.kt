package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Deposit
import com.example.data.model.Member
import com.example.ui.MessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositsScreen(
    viewModel: MessViewModel,
    onBack: () -> Unit
) {
    val deposits by viewModel.deposits.collectAsState()
    val members by viewModel.members.collectAsState()
    val isManager by viewModel.isManager.collectAsState()
    val totalDeposits = deposits.sumOf { it.amount }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mess Deposit Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isManager) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.testTag("add_deposit_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Deposit")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isManager) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "🔒 Read-only view: Only Mess Managers and Admins can record, correct, or delete member deposit entries.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            // Total Deposits Hero Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL POOLED ADVANCE DEPOSITS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalDeposits",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Deposit Contributions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (deposits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No deposits recorded yet. Tap + to add member funds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(deposits) { deposit ->
                        DepositCard(
                            deposit = deposit,
                            canDelete = isManager,
                            onDelete = { viewModel.deleteDeposit(deposit.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        if (showAddDialog) {
            AddDepositDialog(
                members = members,
                onDismiss = { showAddDialog = false },
                onAdd = { memberId, memberName, amount, method, notes ->
                    viewModel.addDeposit(memberId, memberName, amount, method, notes)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun DepositCard(
    deposit: Deposit,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = deposit.memberName.ifBlank { "Member" },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = deposit.paymentMethod,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = deposit.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (deposit.notes.isNotBlank()) {
                        Text(
                            text = deposit.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${deposit.amount}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDepositDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onAdd: (memberId: String, memberName: String, amount: Double, method: String, notes: String) -> Unit
) {
    var selectedMember by remember { mutableStateOf(members.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var notesText by remember { mutableStateOf("") }
    var expandedMemberDropdown by remember { mutableStateOf(false) }

    val methods = listOf("Cash", "bKash / Mobile", "Bank Transfer", "UPI")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Member Deposit", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Member Selector
                ExposedDropdownMenuBox(
                    expanded = expandedMemberDropdown,
                    onExpandedChange = { expandedMemberDropdown = !expandedMemberDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedMember?.displayName ?: "Select Member",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Member") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMemberDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMemberDropdown,
                        onDismissRequest = { expandedMemberDropdown = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.displayName) },
                                onClick = {
                                    selectedMember = m
                                    expandedMemberDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Deposit Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input"),
                    singleLine = true
                )

                Text("Payment Channel", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(methods) { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Reference (Optional)") },
                    placeholder = { Text("e.g. November Advance") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val member = selectedMember
                    if (amount > 0 && member != null) {
                        onAdd(member.id, member.displayName, amount, paymentMethod, notesText.trim())
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0 && selectedMember != null,
                modifier = Modifier.testTag("submit_deposit_button")
            ) {
                Text("Save Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
