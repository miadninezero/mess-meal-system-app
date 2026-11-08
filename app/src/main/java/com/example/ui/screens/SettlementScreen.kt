package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.MemberSettlement
import com.example.ui.MessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementScreen(
    viewModel: MessViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val mess by viewModel.currentMess.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val utilityBills by viewModel.utilityBills.collectAsState()
    val isManager by viewModel.isManager.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAuditSection by remember { mutableStateOf(false) }

    fun copyReportToClipboard() {
        val report = buildString {
            appendLine("════════════════════════════════")
            appendLine("📋 ${mess?.name ?: "MESS MEAL"} - FINAL SETTLEMENT")
            appendLine("Month: ${mess?.activeMonth ?: "Current"}")
            appendLine("════════════════════════════════")
            appendLine("• Total Bazaar: ${summary.totalBazaarCost}")
            appendLine("• Total Attendance Units: ${summary.totalMeals} units")
            appendLine("• Final Meal Rate: ${summary.mealRate} / unit")
            appendLine("• Total Utility Bills: ${summary.totalUtilities}")
            appendLine("• Total Member Deposits: ${summary.totalDeposits}")
            appendLine("• Mess Reserve Balance: ${summary.remainingFund}")
            appendLine("────────────────────────────────")
            appendLine("MEMBER RECONCILIATION:")
            summary.memberSettlements.forEach { s ->
                val status = if (s.netBalance >= 0) "REFUND: +${s.netBalance}" else "DUE: ${s.netBalance}"
                appendLine("👤 ${s.memberName}")
                appendLine("   Attendance Units: ${s.totalMeals} | Meal Cost: ${s.mealCost}")
                appendLine("   Fixed Utilities: ${s.utilityCost} | Total Expense: ${s.totalCost}")
                appendLine("   Total Deposit: ${s.totalDeposit}")
                appendLine("   Status: $status")
                appendLine("")
            }
            appendLine("════════════════════════════════")
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mess Settlement Report", report)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Settlement report copied to clipboard!", Toast.LENGTH_LONG).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Month-End Settlement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { copyReportToClipboard() },
                        modifier = Modifier.testTag("share_settlement_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Report")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Metrics Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "MONTHLY MESS BALANCE SHEET",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Final Meal Rate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text("${summary.mealRate}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Total Expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text("${summary.totalBazaarCost + summary.totalUtilities}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Attendance Units:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${summary.totalMeals} units", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = "Rules: Normal day (Both=2, Lunch=1.5, Night=1) • Friday (Lunch=1) • Single served=1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Bazaar:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${summary.totalBazaarCost}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fixed Utilities (Cook/Bills):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${summary.totalUtilities}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Member Deposits:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${summary.totalDeposits}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Cash in Hand / Reserve:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "${summary.remainingFund}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (summary.remainingFund >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Shared Utility Bills Header & Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Shared Utilities (Split Equally)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (!isManager) {
                            Text(
                                text = "Only Managers can record utility bills",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (isManager) {
                        OutlinedButton(
                            onClick = { showAddBillDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Bill", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (utilityBills.isNotEmpty()) {
                items(utilityBills) { bill ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(bill.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(bill.month, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${bill.amount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                if (isManager) {
                                    IconButton(onClick = { viewModel.deleteUtilityBill(bill.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Individual Member Final Accounts
            item {
                Text(
                    text = "Individual Member Balances",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(summary.memberSettlements) { settlement ->
                MemberSettlementCard(settlement = settlement)
            }

            // Audit Trail Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Audit History & Governance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Immutable ledger of financial & attendance overrides",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(onClick = { showAuditSection = !showAuditSection }) {
                                Text(if (showAuditSection) "Hide (${auditLogs.size})" else "Show (${auditLogs.size})")
                            }
                        }

                        if (showAuditSection) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (auditLogs.isEmpty()) {
                                Text(
                                    text = "No audit entries recorded yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    auditLogs.take(15).forEach { log ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = log.action.replace("_", " "),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = log.timestampFormatted,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = log.details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "By: ${log.performedByName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Export Action Button
            item {
                Button(
                    onClick = { copyReportToClipboard() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy & Export Statement to Clipboard", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        if (showAddBillDialog) {
            AddUtilityBillDialog(
                onDismiss = { showAddBillDialog = false },
                onAdd = { title, amount ->
                    viewModel.addUtilityBill(title, amount)
                    showAddBillDialog = false
                }
            )
        }
    }
}

@Composable
private fun MemberSettlementCard(settlement: MemberSettlement) {
    val isRefund = settlement.isRefund
    val statusColor = if (isRefund) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val statusBadgeBg = if (isRefund) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
    val statusBadgeText = if (isRefund) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = settlement.memberName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBadgeBg
                ) {
                    Text(
                        text = if (isRefund) "REFUND: +${settlement.netBalance}" else "DUE: ${settlement.netBalance}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusBadgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Attendance Units:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${settlement.totalMeals} units", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Meal Cost (${settlement.totalMeals} × rate):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${settlement.mealCost}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Shared Utility Share:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${settlement.utilityCost}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Member Expense:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("${settlement.totalCost}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Deposit Paid:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Text("${settlement.totalDeposit}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun AddUtilityBillDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, amount: Double) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shared Utility Bill", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Bill Title") },
                    placeholder = { Text("e.g. Cook Salary, Gas, WiFi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Total Bill Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (titleText.isNotBlank() && amount > 0) {
                        onAdd(titleText.trim(), amount)
                    }
                },
                enabled = titleText.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Add Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
