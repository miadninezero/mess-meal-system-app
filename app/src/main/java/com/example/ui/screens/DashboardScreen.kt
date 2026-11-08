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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BazaarExpense
import com.example.ui.MessViewModel
import com.example.util.DhakaTimeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MessViewModel,
    onNavigateToMeals: () -> Unit,
    onNavigateToBazaar: () -> Unit,
    onNavigateToDeposits: () -> Unit,
    onNavigateToSettlement: () -> Unit,
    onChangeMess: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val mess by viewModel.currentMess.collectAsState()
    val members by viewModel.members.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val dailyMeals by viewModel.dailyMeals.collectAsState()
    val bazaarExpenses by viewModel.bazaarExpenses.collectAsState()

    val currentMember = members.firstOrNull { it.userId == viewModel.currentUserId }
    val memberId = currentMember?.id ?: viewModel.currentUserId
    val todayRecord = dailyMeals.firstOrNull { it.id == viewModel.todayDhakaDate || it.date == viewModel.todayDhakaDate }
    val myBooking = todayRecord?.bookings?.get(memberId)
    val kitchenSummary by viewModel.kitchenSummary.collectAsState()
    val isFriday = todayRecord?.isFriday ?: DhakaTimeHelper.isFriday(viewModel.todayDhakaDate)

    var showMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mess?.name?.ifBlank { "Mess Meal" } ?: "Mess Meal",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mess ID", mess?.id ?: "")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Mess Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Code: ${mess?.id?.take(8) ?: "..."}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copy Code",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Switch / Join Mess") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onChangeMess()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out") },
                            leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSignOut()
                            }
                        )
                    }
                }
            }
        }

        // Live Meal Rate Hero Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT MEAL RATE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = String.format("%.2f", summary.mealRate),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = " / meal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Formula: Total Bazaar (${summary.totalBazaarCost}) ÷ Attendance Units (${summary.totalMeals})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 4-Grid Key Financial Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Attendance Units",
                    value = "${summary.totalMeals}",
                    icon = Icons.Default.Restaurant,
                    tint = MaterialTheme.colorScheme.primary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Bazaar",
                    value = "${summary.totalBazaarCost}",
                    icon = Icons.Default.ShoppingBag,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Deposits",
                    value = "${summary.totalDeposits}",
                    icon = Icons.Default.Payments,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Reserve Fund",
                    value = "${summary.remainingFund}",
                    icon = Icons.Default.AccountBalanceWallet,
                    tint = if (summary.remainingFund >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }

        // Today's Booking & Kitchen Orders Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Meals (${viewModel.todayDhakaDate})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Asia/Dhaka Timezone • Strict Cutoffs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (isFriday) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Friday (Lunch Only)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // My Booking Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val lunchStatus = myBooking?.statusLunch ?: "NO_RESPONSE"
                        val nightStatus = if (isFriday) "DISABLED" else (myBooking?.statusNight ?: "NO_RESPONSE")
                        val totalGuests = (myBooking?.guestLunch ?: 0) + (myBooking?.guestNight ?: 0)

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("My Lunch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lunchStatus.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (lunchStatus == "BOOKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("My Night", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = nightStatus.replace("_", " "),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isFriday) MaterialTheme.colorScheme.outline else if (nightStatus == "BOOKED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Guest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalGuests",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kitchen Order Tally
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🍳 Kitchen Summary:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Lunch: ${kitchenSummary.bookedLunchCount} | Night: ${kitchenSummary.bookedNightCount}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToMeals,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Bookings & Attendance")
                    }
                }
            }
        }

        // Quick Navigation Section
        item {
            Text(
                text = "Mess Operations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    onClick = onNavigateToMeals,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Daily Meals", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                OutlinedCard(
                    onClick = onNavigateToBazaar,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bazaar Log", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                OutlinedCard(
                    onClick = onNavigateToDeposits,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Deposits", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }

                OutlinedCard(
                    onClick = onNavigateToSettlement,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Balance", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        // Recent Market Expenses
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bazaar Purchases",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedButton(
                    onClick = onNavigateToBazaar,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View All", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (bazaarExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "No bazaar records yet this month. Tap 'Bazaar Log' to add grocery purchases.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(bazaarExpenses.take(3)) { expense ->
                BazaarMiniCard(expense = expense)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MealStepper(
    label: String,
    count: Double,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                text = if (count % 1.0 == 0.0) count.toInt().toString() else count.toString(),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = onIncrement,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun BazaarMiniCard(expense: BazaarExpense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    text = expense.itemsDescription.ifBlank { "Bazaar Purchase" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${expense.buyerName} • ${expense.date} • ${expense.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${expense.amount}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
