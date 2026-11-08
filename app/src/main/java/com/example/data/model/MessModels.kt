package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val currentMessId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Mess(
    @DocumentId val id: String = "",
    val name: String = "",
    val managerUid: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val activeMonth: String = "", // e.g. "2026-11"
    val lunchBookingDeadline: String = "09:00", // HH:mm (24-hr) in Asia/Dhaka
    val nightBookingDeadline: String = "17:00", // HH:mm (24-hr) in Asia/Dhaka
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isManager(userId: String): Boolean {
        return managerUid == userId || adminUids.contains(userId)
    }
}

@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val room: String = "",
    val role: String = "MEMBER", // "MANAGER" or "MEMBER"
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

// Meal Booking state per meal: "NO_RESPONSE", "BOOKED", "CANCELLED"
@IgnoreExtraProperties
data class MealBookingEntry(
    val statusLunch: String = "NO_RESPONSE", // "BOOKED", "CANCELLED", "NO_RESPONSE"
    val statusNight: String = "NO_RESPONSE", // "BOOKED", "CANCELLED", "NO_RESPONSE"
    val guestLunch: Int = 0,
    val guestNight: Int = 0,
    val updatedBy: String = "",
    val managerOverrideReason: String = ""
)

// Confirmed Actual Attendance Entry
@IgnoreExtraProperties
data class MealAttendanceEntry(
    val attendedLunch: Boolean = false,
    val attendedNight: Boolean = false,
    val guestLunchAttended: Int = 0,
    val guestNightAttended: Int = 0,
    val calculatedUnits: Double = 0.0
)

@IgnoreExtraProperties
data class DailyMealRecord(
    @DocumentId val id: String = "", // date format YYYY-MM-DD
    val date: String = "",
    val isFriday: Boolean = false,
    val lunchServiceStatus: String = "ACTIVE", // "ACTIVE" or "CANCELLED"
    val nightServiceStatus: String = "ACTIVE", // "ACTIVE" or "CANCELLED"
    val cancellationReason: String = "",
    val bookings: Map<String, MealBookingEntry> = emptyMap(),
    val attendances: Map<String, MealAttendanceEntry> = emptyMap(),
    val totalDayUnits: Double = 0.0,
    val totalDayMeals: Double = 0.0, // backward compatibility
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val action: String = "", // e.g. "DEPOSIT_ADDED", "DEPOSIT_DELETED", "BOOKING_OVERRIDE", "ATTENDANCE_CONFIRMED", "MEAL_SERVICE_CANCELLED"
    val performedByUid: String = "",
    val performedByName: String = "",
    val targetMemberId: String = "",
    val targetMemberName: String = "",
    val details: String = "",
    val reason: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    val timestampFormatted: String
        get() {
            val t = timestamp ?: return "Just now"
            val sdf = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Dhaka")
            }
            return sdf.format(t.toDate())
        }
}

@IgnoreExtraProperties
data class BazaarExpense(
    @DocumentId val id: String = "",
    val buyerMemberId: String = "",
    val buyerName: String = "",
    val amount: Double = 0.0,
    val itemsDescription: String = "",
    val category: String = "Groceries", // Groceries, Meat & Fish, Vegetables, Spices, Misc
    val date: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Deposit(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val paymentMethod: String = "Cash", // Cash, bKash/Nagad, Bank, UPI
    val notes: String = "",
    val recordedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UtilityBill(
    @DocumentId val id: String = "",
    val title: String = "", // e.g. Cook Salary, Gas, WiFi, Electricity
    val amount: Double = 0.0,
    val month: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
