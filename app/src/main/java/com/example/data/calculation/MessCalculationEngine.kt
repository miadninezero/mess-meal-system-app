package com.example.data.calculation

import com.example.data.model.BazaarExpense
import com.example.data.model.DailyMealRecord
import com.example.data.model.Deposit
import com.example.data.model.MealAttendanceEntry
import com.example.data.model.Member
import com.example.data.model.UtilityBill
import com.example.util.DhakaTimeHelper
import kotlin.math.roundToInt

data class MemberSettlement(
    val memberId: String,
    val memberName: String,
    val totalUnits: Double,
    val mealCost: Double,
    val utilityCost: Double,
    val totalCost: Double,
    val totalDeposit: Double,
    val netBalance: Double
) {
    val totalMeals: Double get() = totalUnits
    val isRefund: Boolean get() = netBalance >= 0.0
}

data class MessSummary(
    val totalUnits: Double,
    val totalBazaarCost: Double,
    val mealRate: Double,
    val totalDeposits: Double,
    val totalUtilities: Double,
    val remainingFund: Double,
    val memberSettlements: List<MemberSettlement>
) {
    val totalMeals: Double get() = totalUnits
}

object MessCalculationEngine {

    /**
     * Calculates attendance units for a member and their guests on a given date according to the mess rules:
     * - Normal day (Lunch + Night served): Both = 2 units, Only Lunch = 1.5 units, Only Night = 1 unit.
     * - Friday: Only Lunch served = 1 unit.
     * - Single-meal served day (due to unexpected cancellation): Eating served meal = 1 unit.
     * - Guest meals: Evaluated using the exact same rules under the host member's account.
     */
    fun calculateAttendanceUnits(
        isFriday: Boolean,
        lunchServiceStatus: String,
        nightServiceStatus: String,
        attendance: MealAttendanceEntry
    ): Double {
        val lunchActive = lunchServiceStatus == "ACTIVE"
        val nightActive = if (isFriday) false else (nightServiceStatus == "ACTIVE")

        val totalServedMeals = (if (lunchActive) 1 else 0) + (if (nightActive) 1 else 0)

        fun computePersonUnits(ateLunch: Boolean, ateNight: Boolean): Double {
            return when (totalServedMeals) {
                2 -> {
                    when {
                        ateLunch && ateNight -> 2.0
                        ateLunch && !ateNight -> 1.5
                        !ateLunch && ateNight -> 1.0
                        else -> 0.0
                    }
                }
                1 -> {
                    val ateServed = (lunchActive && ateLunch) || (nightActive && ateNight)
                    if (ateServed) 1.0 else 0.0
                }
                else -> 0.0
            }
        }

        // Member's own units
        val memberUnits = computePersonUnits(attendance.attendedLunch, attendance.attendedNight)

        // Guest units under host member's account:
        val guestsBoth = minOf(attendance.guestLunchAttended, attendance.guestNightAttended)
        val guestsOnlyLunch = attendance.guestLunchAttended - guestsBoth
        val guestsOnlyNight = attendance.guestNightAttended - guestsBoth

        val guestUnits = (guestsBoth * computePersonUnits(true, true)) +
                         (guestsOnlyLunch * computePersonUnits(true, false)) +
                         (guestsOnlyNight * computePersonUnits(false, true))

        return roundTwoDecimals(memberUnits + guestUnits)
    }

    fun calculateSummary(
        members: List<Member>,
        dailyMeals: List<DailyMealRecord>,
        bazaarExpenses: List<BazaarExpense>,
        deposits: List<Deposit>,
        utilityBills: List<UtilityBill>
    ): MessSummary {
        val activeMembers = members.filter { it.isActive }
        val memberCount = if (activeMembers.isNotEmpty()) activeMembers.size else 1

        // 1. Calculate Total Bazaar Cost
        val totalBazaarCost = bazaarExpenses.sumOf { it.amount }

        // 2. Calculate Total Units per member and total mess units across the month
        val memberUnitsMap = mutableMapOf<String, Double>()
        for (member in members) {
            memberUnitsMap[member.id] = 0.0
        }

        var totalMessUnits = 0.0

        for (record in dailyMeals) {
            val isFriday = record.isFriday || DhakaTimeHelper.isFriday(record.date)

            for ((memberId, attendance) in record.attendances) {
                val units = calculateAttendanceUnits(
                    isFriday = isFriday,
                    lunchServiceStatus = record.lunchServiceStatus,
                    nightServiceStatus = record.nightServiceStatus,
                    attendance = attendance
                )
                memberUnitsMap[memberId] = (memberUnitsMap[memberId] ?: 0.0) + units
                totalMessUnits += units
            }
        }

        // 3. Compute Meal Rate: Total Bazaar Cost / Total Mess Units
        val mealRate = if (totalMessUnits > 0.0) {
            totalBazaarCost / totalMessUnits
        } else {
            0.0
        }

        // 4. Utility bills split equally among active members
        val totalUtilities = utilityBills.sumOf { it.amount }
        val perHeadUtility = if (memberCount > 0) totalUtilities / memberCount else 0.0

        // 5. Total deposits per member
        val memberDepositsMap = mutableMapOf<String, Double>()
        var totalDeposits = 0.0
        for (deposit in deposits) {
            memberDepositsMap[deposit.memberId] = (memberDepositsMap[deposit.memberId] ?: 0.0) + deposit.amount
            totalDeposits += deposit.amount
        }

        // 6. Build individual member settlements based on actual attendance units
        val settlements = members.map { member ->
            val units = memberUnitsMap[member.id] ?: 0.0
            val mealCost = units * mealRate
            val utilityShare = if (member.isActive) perHeadUtility else 0.0
            val totalCost = mealCost + utilityShare
            val deposit = memberDepositsMap[member.id] ?: 0.0
            val netBalance = deposit - totalCost

            MemberSettlement(
                memberId = member.id,
                memberName = member.displayName.ifBlank { "Member" },
                totalUnits = roundTwoDecimals(units),
                mealCost = roundTwoDecimals(mealCost),
                utilityCost = roundTwoDecimals(utilityShare),
                totalCost = roundTwoDecimals(totalCost),
                totalDeposit = roundTwoDecimals(deposit),
                netBalance = roundTwoDecimals(netBalance)
            )
        }

        val remainingFund = totalDeposits - totalBazaarCost - totalUtilities

        return MessSummary(
            totalUnits = roundTwoDecimals(totalMessUnits),
            totalBazaarCost = roundTwoDecimals(totalBazaarCost),
            mealRate = roundTwoDecimals(mealRate),
            totalDeposits = roundTwoDecimals(totalDeposits),
            totalUtilities = roundTwoDecimals(totalUtilities),
            remainingFund = roundTwoDecimals(remainingFund),
            memberSettlements = settlements
        )
    }

    fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
