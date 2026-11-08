package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.calculation.MessCalculationEngine
import com.example.data.model.AuditLog
import com.example.data.model.BazaarExpense
import com.example.data.model.DailyMealRecord
import com.example.data.model.Deposit
import com.example.data.model.MealAttendanceEntry
import com.example.data.model.MealBookingEntry
import com.example.data.model.Member
import com.example.data.model.Mess
import com.example.data.model.UserProfile
import com.example.data.model.UtilityBill
import com.example.util.DhakaTimeHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MessRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(UserProfile::class.java))
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val payload = mapOf(
            "displayName" to profile.displayName,
            "email" to profile.email,
            "currentMessId" to profile.currentMessId,
            "createdAt" to (profile.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Mess Observation ---
    fun observeMess(messId: String): Flow<Mess?> = callbackFlow {
        if (messId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("messes").document(messId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Mess::class.java))
        }
        awaitClose { listener.remove() }
    }

    fun observeMembers(messId: String): Flow<List<Member>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/members"
        emitAll(
            db.collection("messes").document(messId).collection("members")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Member::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    fun observeDailyMeals(messId: String): Flow<List<DailyMealRecord>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/daily_meals"
        emitAll(
            db.collection("messes").document(messId).collection("daily_meals")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(DailyMealRecord::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    fun observeBazaarExpenses(messId: String): Flow<List<BazaarExpense>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/bazaar_expenses"
        emitAll(
            db.collection("messes").document(messId).collection("bazaar_expenses")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(BazaarExpense::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    fun observeDeposits(messId: String): Flow<List<Deposit>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/deposits"
        emitAll(
            db.collection("messes").document(messId).collection("deposits")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Deposit::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    fun observeUtilityBills(messId: String): Flow<List<UtilityBill>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/utility_bills"
        emitAll(
            db.collection("messes").document(messId).collection("utility_bills")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(UtilityBill::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    fun observeAuditLogs(messId: String): Flow<List<AuditLog>> = flow {
        if (messId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "messes/$messId/audit_logs"
        emitAll(
            db.collection("messes").document(messId).collection("audit_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(AuditLog::class.java) }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    suspend fun recordAuditLog(messId: String, log: AuditLog) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId).collection("audit_logs").document()
        val payload = mapOf(
            "action" to log.action,
            "performedByUid" to uid,
            "performedByName" to log.performedByName,
            "targetMemberId" to log.targetMemberId,
            "targetMemberName" to log.targetMemberName,
            "details" to log.details,
            "reason" to log.reason,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            Log.w("MessRepository", "Audit log write failure: ${e.message}")
        }
    }

    // --- Mess Setup & Deadlines ---
    suspend fun createMess(messName: String, managerName: String, defaultMonth: String): String {
        val uid = requireUserId()
        val messDocRef = db.collection("messes").document()
        val messId = messDocRef.id

        val messPayload = mapOf(
            "name" to messName,
            "managerUid" to uid,
            "adminUids" to listOf(uid),
            "memberUids" to listOf(uid),
            "activeMonth" to defaultMonth,
            "lunchBookingDeadline" to "09:00",
            "nightBookingDeadline" to "17:00",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val memberDocRef = messDocRef.collection("members").document(uid)
        val memberPayload = mapOf(
            "userId" to uid,
            "displayName" to managerName,
            "room" to "Manager",
            "role" to "MANAGER",
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            messDocRef.set(messPayload).await()
            memberDocRef.set(memberPayload).await()
            val userDocRef = db.collection("users").document(uid)
            val userPayload = mapOf(
                "currentMessId" to messId,
                "displayName" to (auth.currentUser?.displayName?.ifBlank { managerName } ?: managerName),
                "email" to (auth.currentUser?.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                userDocRef.set(userPayload, SetOptions.merge()).await()
            } catch (profileError: Exception) {
                Log.w("MessRepository", "Non-fatal user profile merge: ${profileError.message}")
            }
            return messId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, messDocRef.path)
            throw e
        }
    }

    suspend fun joinMess(messId: String, memberName: String, room: String) {
        val uid = requireUserId()
        val messDocRef = db.collection("messes").document(messId)

        val memberDocRef = messDocRef.collection("members").document(uid)
        val memberPayload = mapOf(
            "userId" to uid,
            "displayName" to memberName,
            "room" to room,
            "role" to "MEMBER",
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            messDocRef.update("memberUids", FieldValue.arrayUnion(uid)).await()
            memberDocRef.set(memberPayload).await()
            val userDocRef = db.collection("users").document(uid)
            val userPayload = mapOf(
                "currentMessId" to messId,
                "displayName" to (auth.currentUser?.displayName?.ifBlank { memberName } ?: memberName),
                "email" to (auth.currentUser?.email ?: ""),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                userDocRef.set(userPayload, SetOptions.merge()).await()
            } catch (profileError: Exception) {
                Log.w("MessRepository", "Non-fatal user profile merge on join: ${profileError.message}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, messDocRef.path)
            throw e
        }
    }

    suspend fun updateBookingDeadlines(messId: String, lunchDeadline: String, nightDeadline: String) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId)
        try {
            docRef.update(
                mapOf(
                    "lunchBookingDeadline" to lunchDeadline,
                    "nightBookingDeadline" to nightDeadline,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            recordAuditLog(
                messId,
                AuditLog(
                    action = "DEADLINE_CONFIGURED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    details = "Lunch cutoff: $lunchDeadline, Night cutoff: $nightDeadline (Asia/Dhaka)"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Service Status & Unexpected Cancellation ---
    suspend fun updateMealServiceStatus(
        messId: String,
        date: String,
        lunchStatus: String,
        nightStatus: String,
        cancellationReason: String
    ) {
        val uid = requireUserId()
        val isFriday = DhakaTimeHelper.isFriday(date)
        val docRef = db.collection("messes").document(messId).collection("daily_meals").document(date)

        val payload = mapOf(
            "date" to date,
            "isFriday" to isFriday,
            "lunchServiceStatus" to lunchStatus,
            "nightServiceStatus" to (if (isFriday) "CANCELLED" else nightStatus),
            "cancellationReason" to cancellationReason,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload, SetOptions.merge()).await()

            recordAuditLog(
                messId,
                AuditLog(
                    action = "SERVICE_STATUS_UPDATED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    details = "Date: $date | Lunch: $lunchStatus, Night: $nightStatus | Reason: $cancellationReason",
                    reason = cancellationReason
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Member Meal Booking (Before Deadline) ---
    suspend fun saveMemberBooking(
        messId: String,
        date: String,
        memberId: String,
        booking: MealBookingEntry
    ) {
        val uid = requireUserId()
        val isFriday = DhakaTimeHelper.isFriday(date)
        val docRef = db.collection("messes").document(messId).collection("daily_meals").document(date)

        val effectiveBooking = if (isFriday) {
            booking.copy(statusNight = "CANCELLED", guestNight = 0, updatedBy = uid)
        } else {
            booking.copy(updatedBy = uid)
        }

        val bookingPayload = mapOf(
            "statusLunch" to effectiveBooking.statusLunch,
            "statusNight" to effectiveBooking.statusNight,
            "guestLunch" to effectiveBooking.guestLunch,
            "guestNight" to effectiveBooking.guestNight,
            "updatedBy" to uid,
            "managerOverrideReason" to effectiveBooking.managerOverrideReason
        )

        val payload = mapOf(
            "date" to date,
            "isFriday" to isFriday,
            "bookings.$memberId" to bookingPayload,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(
                mapOf(
                    "date" to date,
                    "isFriday" to isFriday,
                    "lunchServiceStatus" to "ACTIVE",
                    "nightServiceStatus" to (if (isFriday) "CANCELLED" else "ACTIVE")
                ),
                SetOptions.merge()
            ).await()

            docRef.update(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Manager Booking Override (With Audit History) ---
    suspend fun overrideMemberBooking(
        messId: String,
        date: String,
        memberId: String,
        memberName: String,
        booking: MealBookingEntry,
        reason: String
    ) {
        val uid = requireUserId()
        val isFriday = DhakaTimeHelper.isFriday(date)
        val docRef = db.collection("messes").document(messId).collection("daily_meals").document(date)

        val bookingPayload = mapOf(
            "statusLunch" to booking.statusLunch,
            "statusNight" to (if (isFriday) "CANCELLED" else booking.statusNight),
            "guestLunch" to booking.guestLunch,
            "guestNight" to (if (isFriday) 0 else booking.guestNight),
            "updatedBy" to uid,
            "managerOverrideReason" to reason
        )

        val payload = mapOf(
            "date" to date,
            "isFriday" to isFriday,
            "bookings.$memberId" to bookingPayload,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(
                mapOf("date" to date, "isFriday" to isFriday),
                SetOptions.merge()
            ).await()

            docRef.update(payload).await()

            recordAuditLog(
                messId,
                AuditLog(
                    action = "BOOKING_OVERRIDE",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    targetMemberId = memberId,
                    targetMemberName = memberName,
                    details = "Date: $date | Lunch: ${booking.statusLunch}, Night: ${booking.statusNight}, Guests: L${booking.guestLunch}/N${booking.guestNight}",
                    reason = reason
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Confirmed Actual Attendance (Manager only) ---
    suspend fun saveAttendance(
        messId: String,
        date: String,
        memberId: String,
        memberName: String,
        attendance: MealAttendanceEntry,
        lunchServiceStatus: String,
        nightServiceStatus: String
    ) {
        val uid = requireUserId()
        val isFriday = DhakaTimeHelper.isFriday(date)
        val docRef = db.collection("messes").document(messId).collection("daily_meals").document(date)

        val units = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = isFriday,
            lunchServiceStatus = lunchServiceStatus,
            nightServiceStatus = nightServiceStatus,
            attendance = attendance
        )

        val attendancePayload = mapOf(
            "attendedLunch" to attendance.attendedLunch,
            "attendedNight" to attendance.attendedNight,
            "guestLunchAttended" to attendance.guestLunchAttended,
            "guestNightAttended" to attendance.guestNightAttended,
            "calculatedUnits" to units
        )

        val payload = mapOf(
            "date" to date,
            "isFriday" to isFriday,
            "attendances.$memberId" to attendancePayload,
            "updatedBy" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(
                mapOf(
                    "date" to date,
                    "isFriday" to isFriday,
                    "lunchServiceStatus" to lunchServiceStatus,
                    "nightServiceStatus" to nightServiceStatus
                ),
                SetOptions.merge()
            ).await()

            docRef.update(payload).await()

            recordAuditLog(
                messId,
                AuditLog(
                    action = "ATTENDANCE_CONFIRMED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    targetMemberId = memberId,
                    targetMemberName = memberName,
                    details = "Date: $date | Lunch: ${attendance.attendedLunch}, Night: ${attendance.attendedNight}, Guests: L${attendance.guestLunchAttended}/N${attendance.guestNightAttended} -> $units units"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Financial Records (Deposits & Utilities) ---
    suspend fun addDeposit(messId: String, deposit: Deposit) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId).collection("deposits").document()
        val payload = mapOf(
            "memberId" to deposit.memberId,
            "memberName" to deposit.memberName,
            "amount" to deposit.amount,
            "date" to deposit.date,
            "paymentMethod" to deposit.paymentMethod,
            "notes" to deposit.notes,
            "recordedBy" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordAuditLog(
                messId,
                AuditLog(
                    action = "DEPOSIT_RECORDED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    targetMemberId = deposit.memberId,
                    targetMemberName = deposit.memberName,
                    details = "Amount: ${deposit.amount} | Method: ${deposit.paymentMethod} | Notes: ${deposit.notes}"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteDeposit(messId: String, deposit: Deposit) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId).collection("deposits").document(deposit.id)
        try {
            docRef.delete().await()
            recordAuditLog(
                messId,
                AuditLog(
                    action = "DEPOSIT_DELETED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    targetMemberId = deposit.memberId,
                    targetMemberName = deposit.memberName,
                    details = "Deleted deposit of ${deposit.amount} (${deposit.paymentMethod})"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun addUtilityBill(messId: String, bill: UtilityBill) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId).collection("utility_bills").document()
        val payload = mapOf(
            "title" to bill.title,
            "amount" to bill.amount,
            "month" to bill.month,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordAuditLog(
                messId,
                AuditLog(
                    action = "UTILITY_BILL_ADDED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    details = "Title: ${bill.title} | Amount: ${bill.amount} | Month: ${bill.month}"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteUtilityBill(messId: String, billId: String, billTitle: String, amount: Double) {
        val uid = requireUserId()
        val docRef = db.collection("messes").document(messId).collection("utility_bills").document(billId)
        try {
            docRef.delete().await()
            recordAuditLog(
                messId,
                AuditLog(
                    action = "UTILITY_BILL_DELETED",
                    performedByUid = uid,
                    performedByName = auth.currentUser?.displayName ?: "Manager",
                    details = "Deleted bill: $billTitle | Amount: $amount"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Bazaar Expenses ---
    suspend fun addBazaarExpense(messId: String, expense: BazaarExpense) {
        requireUserId()
        val docRef = db.collection("messes").document(messId).collection("bazaar_expenses").document()
        val payload = mapOf(
            "buyerMemberId" to expense.buyerMemberId,
            "buyerName" to expense.buyerName,
            "amount" to expense.amount,
            "itemsDescription" to expense.itemsDescription,
            "category" to expense.category,
            "date" to expense.date,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBazaarExpense(messId: String, expenseId: String) {
        val docRef = db.collection("messes").document(messId).collection("bazaar_expenses").document(expenseId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun updateMemberStatus(messId: String, memberId: String, isActive: Boolean) {
        val docRef = db.collection("messes").document(messId).collection("members").document(memberId)
        try {
            docRef.update("isActive", isActive).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }
}
