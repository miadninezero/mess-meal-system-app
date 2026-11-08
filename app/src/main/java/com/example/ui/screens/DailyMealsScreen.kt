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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calculation.MessCalculationEngine
import com.example.data.model.MealAttendanceEntry
import com.example.data.model.MealBookingEntry
import com.example.data.model.Member
import com.example.ui.MessViewModel
import com.example.util.DhakaTimeHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMealsScreen(
    viewModel: MessViewModel,
    onBack: () -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val members by viewModel.members.collectAsState()
    val dailyMeals by viewModel.dailyMeals.collectAsState()
    val currentMess by viewModel.currentMess.collectAsState()
    val isManager by viewModel.isManager.collectAsState()
    val kitchenSummary by viewModel.kitchenSummary.collectAsState()

    val currentRecord = dailyMeals.firstOrNull { it.id == selectedDate || it.date == selectedDate }
    val bookingsMap = currentRecord?.bookings ?: emptyMap()
    val attendancesMap = currentRecord?.attendances ?: emptyMap()

    val lunchDeadline = currentMess?.lunchBookingDeadline ?: "09:00"
    val nightDeadline = currentMess?.nightBookingDeadline ?: "17:00"

    val isFriday = currentRecord?.isFriday ?: DhakaTimeHelper.isFriday(selectedDate)
    val isLunchDeadlinePassed = DhakaTimeHelper.isDeadlinePassed(selectedDate, lunchDeadline)
    val isNightDeadlinePassed = if (isFriday) true else DhakaTimeHelper.isDeadlinePassed(selectedDate, nightDeadline)

    val currentMember = members.firstOrNull { it.userId == viewModel.currentUserId }
    val myMemberId = currentMember?.id ?: viewModel.currentUserId
    val myBooking = bookingsMap[myMemberId] ?: MealBookingEntry()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Booking, 1: Kitchen Orders, 2: Actual Attendance
    var showDeadlineDialog by remember { mutableStateOf(false) }
    var showServiceStatusDialog by remember { mutableStateOf(false) }
    var overrideMemberTarget by remember { mutableStateOf<Member?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Operations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isManager) {
                        IconButton(onClick = { showDeadlineDialog = true }) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Configure Deadlines")
                        }
                        IconButton(onClick = { showServiceStatusDialog = true }) {
                            Icon(Icons.Default.RoomService, contentDescription = "Service Status")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Date Switcher Header
            DateSwitcher(
                currentDate = selectedDate,
                isFriday = isFriday,
                onDateChanged = { newDate -> viewModel.setSelectedDate(newDate) },
                onTodayClicked = { viewModel.setSelectedDate(viewModel.todayDhakaDate) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Service Notice or Friday Notice
            if (isFriday || !kitchenSummary.lunchActive || !kitchenSummary.nightActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!kitchenSummary.lunchActive || !kitchenSummary.nightActive)
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (!kitchenSummary.lunchActive || !kitchenSummary.nightActive) Icons.Default.Warning else Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = if (!kitchenSummary.lunchActive || !kitchenSummary.nightActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !kitchenSummary.lunchActive && !kitchenSummary.nightActive -> "All meals cancelled for today: ${kitchenSummary.cancellationReason}"
                                !kitchenSummary.lunchActive -> "Lunch cancelled: ${kitchenSummary.cancellationReason} (Night = 1 unit)"
                                !kitchenSummary.nightActive && !isFriday -> "Night meal cancelled: ${kitchenSummary.cancellationReason} (Lunch = 1 unit)"
                                isFriday -> "Friday Schedule: Only Lunch served (Counts as 1 unit). Night meal closed."
                                else -> "Normal Day: Lunch & Night served."
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (!kitchenSummary.lunchActive || !kitchenSummary.nightActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tab Navigation: My Booking | Kitchen Orders | Actual Attendance
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("My Booking", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Kitchen Orders", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: Member's Own Booking
                    MyBookingTab(
                        date = selectedDate,
                        booking = myBooking,
                        lunchActive = kitchenSummary.lunchActive,
                        nightActive = kitchenSummary.nightActive,
                        isFriday = isFriday,
                        lunchDeadline = lunchDeadline,
                        nightDeadline = nightDeadline,
                        isLunchPassed = isLunchDeadlinePassed,
                        isNightPassed = isNightDeadlinePassed,
                        onSaveBooking = { statusLunch, statusNight, guestL, guestN ->
                            viewModel.saveMyBooking(selectedDate, statusLunch, statusNight, guestL, guestN)
                        }
                    )
                }
                1 -> {
                    // TAB 1: Kitchen Orders & All Member Bookings
                    KitchenOrdersTab(
                        summary = kitchenSummary,
                        members = members,
                        bookings = bookingsMap,
                        isManager = isManager,
                        onOverride = { member -> overrideMemberTarget = member }
                    )
                }
                2 -> {
                    // TAB 2: Actual Confirmed Attendance (Drives Units & Financial Billing)
                    AttendanceTab(
                        members = members,
                        attendances = attendancesMap,
                        isFriday = isFriday,
                        lunchActive = kitchenSummary.lunchActive,
                        nightActive = kitchenSummary.nightActive,
                        isManager = isManager,
                        onSaveAttendance = { memberId, memberName, attendedL, attendedN, guestL, guestN ->
                            viewModel.saveConfirmedAttendance(
                                selectedDate,
                                memberId,
                                memberName,
                                attendedL,
                                attendedN,
                                guestL,
                                guestN
                            )
                        }
                    )
                }
            }
        }

        if (showDeadlineDialog && isManager) {
            ConfigureDeadlineDialog(
                currentLunch = lunchDeadline,
                currentNight = nightDeadline,
                onDismiss = { showDeadlineDialog = false },
                onSave = { lunch, night ->
                    viewModel.configureDeadlines(lunch, night)
                    showDeadlineDialog = false
                }
            )
        }

        if (showServiceStatusDialog && isManager) {
            ServiceStatusDialog(
                date = selectedDate,
                lunchActive = kitchenSummary.lunchActive,
                nightActive = kitchenSummary.nightActive,
                onDismiss = { showServiceStatusDialog = false },
                onSave = { lunchStatus, nightStatus, reason ->
                    viewModel.updateMealServiceStatus(selectedDate, lunchStatus, nightStatus, reason)
                    showServiceStatusDialog = false
                }
            )
        }

        overrideMemberTarget?.let { member ->
            val prev = bookingsMap[member.id] ?: MealBookingEntry()
            ManagerOverrideBookingDialog(
                member = member,
                date = selectedDate,
                prevBooking = prev,
                isFriday = isFriday,
                onDismiss = { overrideMemberTarget = null },
                onOverride = { statusL, statusN, guestL, guestN, reason ->
                    viewModel.managerOverrideBooking(
                        selectedDate,
                        member.id,
                        member.displayName,
                        statusL,
                        statusN,
                        guestL,
                        guestN,
                        reason
                    )
                    overrideMemberTarget = null
                }
            )
        }
    }
}

@Composable
private fun MyBookingTab(
    date: String,
    booking: MealBookingEntry,
    lunchActive: Boolean,
    nightActive: Boolean,
    isFriday: Boolean,
    lunchDeadline: String,
    nightDeadline: String,
    isLunchPassed: Boolean,
    isNightPassed: Boolean,
    onSaveBooking: (statusLunch: String, statusNight: String, guestLunch: Int, guestNight: Int) -> Unit
) {
    var statusLunch by remember(booking) { mutableStateOf(booking.statusLunch) }
    var statusNight by remember(booking) { mutableStateOf(booking.statusNight) }
    var guestLunch by remember(booking) { mutableIntStateOf(booking.guestLunch) }
    var guestNight by remember(booking) { mutableIntStateOf(booking.guestNight) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Your Meal Booking ($date)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Separate statuses: Booked, Cancelled, or No Response. Attendance is confirmed after meals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lunch Booking Card
                    MealBookingCard(
                        mealName = "Lunch (Day)",
                        deadlineTime = DhakaTimeHelper.format12Hour(lunchDeadline),
                        isPassed = isLunchPassed,
                        isActive = lunchActive,
                        status = statusLunch,
                        guestCount = guestLunch,
                        onStatusChanged = { statusLunch = it },
                        onGuestChanged = { guestLunch = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Night Booking Card
                    if (isFriday) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Night Meal: Friday Closed", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Night booking is disabled on Fridays as per mess rules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    } else {
                        MealBookingCard(
                            mealName = "Night (Dinner)",
                            deadlineTime = DhakaTimeHelper.format12Hour(nightDeadline),
                            isPassed = isNightPassed,
                            isActive = nightActive,
                            status = statusNight,
                            guestCount = guestNight,
                            onStatusChanged = { statusNight = it },
                            onGuestChanged = { guestNight = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onSaveBooking(statusLunch, if (isFriday) "CANCELLED" else statusNight, guestLunch, if (isFriday) 0 else guestNight)
                        },
                        enabled = (!isLunchPassed && lunchActive) || (!isNightPassed && nightActive && !isFriday),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm / Update My Booking", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MealBookingCard(
    mealName: String,
    deadlineTime: String,
    isPassed: Boolean,
    isActive: Boolean,
    status: String,
    guestCount: Int,
    onStatusChanged: (String) -> Unit,
    onGuestChanged: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (!isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(mealName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (!isActive) "CANCELLED BY SERVICE" else "Cutoff: $deadlineTime (Asia/Dhaka)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        !isActive -> MaterialTheme.colorScheme.errorContainer
                        isPassed -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPassed || !isActive) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (!isActive) "Service Cancelled" else if (isPassed) "Locked" else "Open",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (!isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Way Choice: BOOKED | CANCELLED | NO_RESPONSE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Booked",
                    isSelected = status == "BOOKED",
                    enabled = !isPassed && isActive,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onStatusChanged("BOOKED") }
                )
                StatusButton(
                    modifier = Modifier.weight(1f),
                    label = "Cancelled",
                    isSelected = status == "CANCELLED",
                    enabled = !isPassed && isActive,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onStatusChanged("CANCELLED") }
                )
                StatusButton(
                    modifier = Modifier.weight(1f),
                    label = "No Response",
                    isSelected = status == "NO_RESPONSE",
                    enabled = !isPassed && isActive,
                    color = MaterialTheme.colorScheme.outline,
                    onClick = { onStatusChanged("NO_RESPONSE") }
                )
            }

            if (status == "BOOKED" && isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Guest Meals for $mealName:", style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (guestCount > 0) onGuestChanged(guestCount - 1) },
                            enabled = !isPassed,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "$guestCount",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { onGuestChanged(guestCount + 1) },
                            enabled = !isPassed,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusButton(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.height(38.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(label, fontSize = 11.sp)
        }
    }
}

@Composable
private fun KitchenOrdersTab(
    summary: com.example.ui.KitchenOrderSummary,
    members: List<Member>,
    bookings: Map<String, MealBookingEntry>,
    isManager: Boolean,
    onOverride: (Member) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Kitchen Order Totals Hero
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "KITCHEN PREPARATION ORDERS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${summary.bookedLunchCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Lunch Orders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (summary.isFriday) "CLOSED" else "${summary.bookedNightCount}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text("Night Orders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Member Booking Statuses",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(members) { member ->
            val booking = bookings[member.id] ?: MealBookingEntry()
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
                        Text(member.displayName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BookingStatusBadge("Lunch", booking.statusLunch, booking.guestLunch)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (!summary.isFriday) {
                                BookingStatusBadge("Night", booking.statusNight, booking.guestNight)
                            }
                        }
                        if (booking.managerOverrideReason.isNotBlank()) {
                            Text(
                                text = "Override: ${booking.managerOverrideReason}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    if (isManager) {
                        OutlinedButton(
                            onClick = { onOverride(member) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Override", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun BookingStatusBadge(meal: String, status: String, guest: Int) {
    val (bgColor, textColor) = when (status) {
        "BOOKED" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Text(
            text = "$meal: $status" + (if (guest > 0) " (+$guest G)" else ""),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AttendanceTab(
    members: List<Member>,
    attendances: Map<String, MealAttendanceEntry>,
    isFriday: Boolean,
    lunchActive: Boolean,
    nightActive: Boolean,
    isManager: Boolean,
    onSaveAttendance: (memberId: String, memberName: String, attendedL: Boolean, attendedN: Boolean, guestL: Int, guestN: Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Actual Confirmed Attendance",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Attendance determines actual meal units and financial accounts. Unit formula: Both meals = 2 units, Only Lunch = 1.5 units, Only Night = 1 unit. Friday/Single-meal day = 1 unit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    if (!isManager) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(View-only. Only Managers have permission to confirm attendance).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        items(members) { member ->
            val attendance = attendances[member.id] ?: MealAttendanceEntry()
            MemberAttendanceCard(
                member = member,
                attendance = attendance,
                isFriday = isFriday,
                lunchActive = lunchActive,
                nightActive = nightActive,
                isManager = isManager,
                onSave = { attendedL, attendedN, guestL, guestN ->
                    onSaveAttendance(member.id, member.displayName, attendedL, attendedN, guestL, guestN)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
private fun MemberAttendanceCard(
    member: Member,
    attendance: MealAttendanceEntry,
    isFriday: Boolean,
    lunchActive: Boolean,
    nightActive: Boolean,
    isManager: Boolean,
    onSave: (attendedL: Boolean, attendedN: Boolean, guestL: Int, guestN: Int) -> Unit
) {
    var attendedLunch by remember(attendance) { mutableStateOf(attendance.attendedLunch) }
    var attendedNight by remember(attendance) { mutableStateOf(attendance.attendedNight) }
    var guestLunch by remember(attendance) { mutableIntStateOf(attendance.guestLunchAttended) }
    var guestNight by remember(attendance) { mutableIntStateOf(attendance.guestNightAttended) }

    val calculatedUnits = MessCalculationEngine.calculateAttendanceUnits(
        isFriday = isFriday,
        lunchServiceStatus = if (lunchActive) "ACTIVE" else "CANCELLED",
        nightServiceStatus = if (nightActive) "ACTIVE" else "CANCELLED",
        attendance = MealAttendanceEntry(attendedLunch, attendedNight, guestLunch, guestNight)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(member.displayName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$calculatedUnits units",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Checkboxes for Lunch & Night
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = attendedLunch,
                        onCheckedChange = {
                            if (isManager && lunchActive) {
                                attendedLunch = it
                                onSave(it, attendedNight, guestLunch, guestNight)
                            }
                        },
                        enabled = isManager && lunchActive
                    )
                    Text(
                        text = if (lunchActive) "Attended Lunch" else "Lunch Cancelled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (!isFriday) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = attendedNight,
                            onCheckedChange = {
                                if (isManager && nightActive) {
                                    attendedNight = it
                                    onSave(attendedLunch, it, guestLunch, guestNight)
                                }
                            },
                            enabled = isManager && nightActive
                        )
                        Text(
                            text = if (nightActive) "Attended Night" else "Night Cancelled",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Guests section
            if (isManager) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Guest Attended: Lunch $guestLunch | Night $guestNight", style = MaterialTheme.typography.labelSmall)
                    Row {
                        TextButton(
                            onClick = {
                                guestLunch += 1
                                onSave(attendedLunch, attendedNight, guestLunch, guestNight)
                            }
                        ) {
                            Text("+Guest Lunch", fontSize = 11.sp)
                        }
                        if (!isFriday) {
                            TextButton(
                                onClick = {
                                    guestNight += 1
                                    onSave(attendedLunch, attendedNight, guestLunch, guestNight)
                                }
                            ) {
                                Text("+Guest Night", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSwitcher(
    currentDate: String,
    isFriday: Boolean,
    onDateChanged: (String) -> Unit,
    onTodayClicked: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = DhakaTimeHelper.DHAKA_ZONE
    }

    fun shiftDays(days: Int) {
        try {
            val date = sdf.parse(currentDate) ?: Date()
            val cal = Calendar.getInstance(DhakaTimeHelper.DHAKA_ZONE).apply {
                time = date
                add(Calendar.DAY_OF_YEAR, days)
            }
            onDateChanged(sdf.format(cal.time))
        } catch (_: Exception) {}
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { shiftDays(-1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Day")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentDate + (if (isFriday) " (Friday)" else ""),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row {
                IconButton(onClick = onTodayClicked) {
                    Icon(Icons.Default.Today, contentDescription = "Today", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { shiftDays(1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day")
                }
            }
        }
    }
}

@Composable
private fun ConfigureDeadlineDialog(
    currentLunch: String,
    currentNight: String,
    onDismiss: () -> Unit,
    onSave: (lunch: String, night: String) -> Unit
) {
    var lunchDeadline by remember { mutableStateOf(currentLunch) }
    var nightDeadline by remember { mutableStateOf(currentNight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Booking Deadlines", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Times must be entered in 24-hr format (HH:mm) under Asia/Dhaka timezone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = lunchDeadline,
                    onValueChange = { lunchDeadline = it },
                    label = { Text("Lunch Deadline (Asia/Dhaka)") },
                    placeholder = { Text("e.g. 09:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nightDeadline,
                    onValueChange = { nightDeadline = it },
                    label = { Text("Night Deadline (Asia/Dhaka)") },
                    placeholder = { Text("e.g. 17:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(lunchDeadline.trim(), nightDeadline.trim()) },
                enabled = lunchDeadline.isNotBlank() && nightDeadline.isNotBlank()
            ) {
                Text("Save Deadlines")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ServiceStatusDialog(
    date: String,
    lunchActive: Boolean,
    nightActive: Boolean,
    onDismiss: () -> Unit,
    onSave: (lunchStatus: String, nightStatus: String, reason: String) -> Unit
) {
    var isLunchActive by remember { mutableStateOf(lunchActive) }
    var isNightActive by remember { mutableStateOf(nightActive) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Meal Service Status", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Date: $date", fontWeight = FontWeight.Bold)
                Text(
                    text = "If a meal is unexpectedly cancelled due to external issues (e.g. cook absent, gas shortage), update here. The remaining meal will count as 1 unit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isLunchActive, onCheckedChange = { isLunchActive = it })
                    Text("Lunch Service Active")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isNightActive, onCheckedChange = { isNightActive = it })
                    Text("Night Service Active")
                }

                if (!isLunchActive || !isNightActive) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Cancellation Reason") },
                        placeholder = { Text("e.g. Gas cylinder shortage") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        if (isLunchActive) "ACTIVE" else "CANCELLED",
                        if (isNightActive) "ACTIVE" else "CANCELLED",
                        reason.trim()
                    )
                }
            ) {
                Text("Update Service Status")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ManagerOverrideBookingDialog(
    member: Member,
    date: String,
    prevBooking: MealBookingEntry,
    isFriday: Boolean,
    onDismiss: () -> Unit,
    onOverride: (statusL: String, statusN: String, guestL: Int, guestN: Int, reason: String) -> Unit
) {
    var statusLunch by remember { mutableStateOf(prevBooking.statusLunch) }
    var statusNight by remember { mutableStateOf(prevBooking.statusNight) }
    var guestLunch by remember { mutableIntStateOf(prevBooking.guestLunch) }
    var guestNight by remember { mutableIntStateOf(prevBooking.guestNight) }
    var overrideReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manager Override Booking", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Member: ${member.displayName} ($date)", fontWeight = FontWeight.SemiBold)
                Text("Managers can override bookings after deadline with a recorded audit reason.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                Text("Lunch Status:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BOOKED", "CANCELLED", "NO_RESPONSE").forEach { st ->
                        FilterChip(selected = statusLunch == st, onClick = { statusLunch = st }, label = { Text(st, fontSize = 10.sp) })
                    }
                }

                if (!isFriday) {
                    Text("Night Status:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BOOKED", "CANCELLED", "NO_RESPONSE").forEach { st ->
                            FilterChip(selected = statusNight == st, onClick = { statusNight = st }, label = { Text(st, fontSize = 10.sp) })
                        }
                    }
                }

                OutlinedTextField(
                    value = overrideReason,
                    onValueChange = { overrideReason = it },
                    label = { Text("Reason for Override (Required)") },
                    placeholder = { Text("e.g. Member called manager after deadline") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (overrideReason.isNotBlank()) {
                        onOverride(statusLunch, if (isFriday) "CANCELLED" else statusNight, guestLunch, if (isFriday) 0 else guestNight, overrideReason.trim())
                    }
                },
                enabled = overrideReason.isNotBlank()
            ) {
                Text("Save Override")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
