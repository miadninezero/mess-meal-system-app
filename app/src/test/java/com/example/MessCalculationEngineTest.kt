package com.example

import com.example.data.calculation.MessCalculationEngine
import com.example.data.model.BazaarExpense
import com.example.data.model.DailyMealRecord
import com.example.data.model.Deposit
import com.example.data.model.MealAttendanceEntry
import com.example.data.model.Member
import com.example.data.model.UtilityBill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessCalculationEngineTest {

    @Test
    fun testNormalDayAttendanceUnits() {
        val entryBoth = MealAttendanceEntry(attendedLunch = true, attendedNight = true)
        val entryLunchOnly = MealAttendanceEntry(attendedLunch = true, attendedNight = false)
        val entryNightOnly = MealAttendanceEntry(attendedLunch = false, attendedNight = true)
        val entryNeither = MealAttendanceEntry(attendedLunch = false, attendedNight = false)

        val unitsBoth = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "ACTIVE",
            attendance = entryBoth
        )
        val unitsLunch = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "ACTIVE",
            attendance = entryLunchOnly
        )
        val unitsNight = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "ACTIVE",
            attendance = entryNightOnly
        )
        val unitsNeither = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "ACTIVE",
            attendance = entryNeither
        )

        assertEquals("Both lunch and night on normal day must count as 2.0 units", 2.0, unitsBoth, 0.001)
        assertEquals("Lunch only on normal day must count as 1.5 units", 1.5, unitsLunch, 0.001)
        assertEquals("Night only on normal day must count as 1.0 unit", 1.0, unitsNight, 0.001)
        assertEquals("Neither meal attended must count as 0.0 units", 0.0, unitsNeither, 0.001)
    }

    @Test
    fun testFridayAttendanceUnits() {
        val entryAttended = MealAttendanceEntry(attendedLunch = true, attendedNight = true)
        val entryNotAttended = MealAttendanceEntry(attendedLunch = false, attendedNight = false)

        val unitsAttended = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = true,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "CANCELLED",
            attendance = entryAttended
        )
        val unitsNotAttended = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = true,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "CANCELLED",
            attendance = entryNotAttended
        )

        assertEquals("Friday lunch must count as 1.0 unit", 1.0, unitsAttended, 0.001)
        assertEquals("Friday not attended must count as 0.0 units", 0.0, unitsNotAttended, 0.001)
    }

    @Test
    fun testSingleMealServedDueToUnexpectedCancellation() {
        val entry = MealAttendanceEntry(attendedLunch = true, attendedNight = true)

        // Lunch served, night cancelled unexpectedly
        val unitsOnlyLunchServed = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "CANCELLED",
            attendance = entry
        )

        // Lunch cancelled, night served unexpectedly
        val unitsOnlyNightServed = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "CANCELLED",
            nightServiceStatus = "ACTIVE",
            attendance = entry
        )

        // Both cancelled
        val unitsBothCancelled = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "CANCELLED",
            nightServiceStatus = "CANCELLED",
            attendance = entry
        )

        assertEquals("If only lunch is served due to cancellation, eating it counts as 1.0 unit", 1.0, unitsOnlyLunchServed, 0.001)
        assertEquals("If only night is served due to cancellation, eating it counts as 1.0 unit", 1.0, unitsOnlyNightServed, 0.001)
        assertEquals("If both meals are cancelled, units must be 0.0", 0.0, unitsBothCancelled, 0.001)
    }

    @Test
    fun testGuestAttendanceCalculation() {
        // Member attends both meals (2 units) + 2 guests eating lunch only (each 1.5 units) on a normal day
        val entry = MealAttendanceEntry(
            attendedLunch = true,
            attendedNight = true,
            guestLunchAttended = 2,
            guestNightAttended = 0
        )

        val units = MessCalculationEngine.calculateAttendanceUnits(
            isFriday = false,
            lunchServiceStatus = "ACTIVE",
            nightServiceStatus = "ACTIVE",
            attendance = entry
        )

        // Member units = 2.0; 2 guests * 1.5 units = 3.0 units; Total = 5.0 units
        assertEquals("Host member + 2 lunch guests must equal 5.0 units", 5.0, units, 0.001)
    }

    @Test
    fun testFullMonthSettlementWithAttendanceUnits() {
        val members = listOf(
            Member(id = "m1", displayName = "Rahim", isActive = true),
            Member(id = "m2", displayName = "Karim", isActive = true)
        )

        // Day 1: Normal day (Rahim eats both = 2.0, Karim eats lunch only = 1.5)
        // Day 2: Friday (Rahim eats lunch = 1.0, Karim does not eat = 0.0)
        // Day 3: Cancellation day (night cancelled, both eat lunch = 1.0 each)
        // Total Rahim units = 2.0 + 1.0 + 1.0 = 4.0 units
        // Total Karim units = 1.5 + 0.0 + 1.0 = 2.5 units
        // Grand Total Mess Units = 6.5 units
        val dailyMeals = listOf(
            DailyMealRecord(
                id = "2026-11-01",
                date = "2026-11-01",
                isFriday = false,
                lunchServiceStatus = "ACTIVE",
                nightServiceStatus = "ACTIVE",
                attendances = mapOf(
                    "m1" to MealAttendanceEntry(attendedLunch = true, attendedNight = true),
                    "m2" to MealAttendanceEntry(attendedLunch = true, attendedNight = false)
                )
            ),
            DailyMealRecord(
                id = "2026-11-06",
                date = "2026-11-06",
                isFriday = true,
                lunchServiceStatus = "ACTIVE",
                nightServiceStatus = "CANCELLED",
                attendances = mapOf(
                    "m1" to MealAttendanceEntry(attendedLunch = true, attendedNight = false),
                    "m2" to MealAttendanceEntry(attendedLunch = false, attendedNight = false)
                )
            ),
            DailyMealRecord(
                id = "2026-11-07",
                date = "2026-11-07",
                isFriday = false,
                lunchServiceStatus = "ACTIVE",
                nightServiceStatus = "CANCELLED",
                attendances = mapOf(
                    "m1" to MealAttendanceEntry(attendedLunch = true, attendedNight = false),
                    "m2" to MealAttendanceEntry(attendedLunch = true, attendedNight = false)
                )
            )
        )

        // Total Bazaar = 650.0 (Meal rate = 650.0 / 6.5 = 100.0 / unit)
        val bazaarExpenses = listOf(
            BazaarExpense(id = "b1", amount = 650.0)
        )

        // Total Utility = 200.0 (Split between 2 active members = 100.0 each)
        val utilityBills = listOf(
            UtilityBill(id = "u1", title = "Gas & WiFi", amount = 200.0)
        )

        // Deposits
        val deposits = listOf(
            Deposit(id = "d1", memberId = "m1", amount = 600.0), // Rahim pays 600
            Deposit(id = "d2", memberId = "m2", amount = 300.0)  // Karim pays 300
        )

        val summary = MessCalculationEngine.calculateSummary(
            members = members,
            dailyMeals = dailyMeals,
            bazaarExpenses = bazaarExpenses,
            deposits = deposits,
            utilityBills = utilityBills
        )

        // Assertions
        assertEquals("Total mess units must be 6.5", 6.5, summary.totalUnits, 0.001)
        assertEquals("Total bazaar cost must be 650.0", 650.0, summary.totalBazaarCost, 0.001)
        assertEquals("Meal rate must be 100.0 per unit", 100.0, summary.mealRate, 0.001)
        assertEquals("Total utilities must be 200.0", 200.0, summary.totalUtilities, 0.001)
        assertEquals("Total deposits must be 900.0", 900.0, summary.totalDeposits, 0.001)
        assertEquals("Reserve balance = 900 - 850 = 50.0", 50.0, summary.remainingFund, 0.001)

        val rahim = summary.memberSettlements.first { it.memberId == "m1" }
        assertEquals("Rahim total units = 4.0", 4.0, rahim.totalUnits, 0.001)
        assertEquals("Rahim meal cost = 4.0 * 100 = 400.0", 400.0, rahim.mealCost, 0.001)
        assertEquals("Rahim utility share = 100.0", 100.0, rahim.utilityCost, 0.001)
        assertEquals("Rahim total cost = 500.0", 500.0, rahim.totalCost, 0.001)
        assertEquals("Rahim refund = 600 - 500 = +100.0", 100.0, rahim.netBalance, 0.001)
        assertTrue(rahim.isRefund)

        val karim = summary.memberSettlements.first { it.memberId == "m2" }
        assertEquals("Karim total units = 2.5", 2.5, karim.totalUnits, 0.001)
        assertEquals("Karim meal cost = 2.5 * 100 = 250.0", 250.0, karim.mealCost, 0.001)
        assertEquals("Karim utility share = 100.0", 100.0, karim.utilityCost, 0.001)
        assertEquals("Karim total cost = 350.0", 350.0, karim.totalCost, 0.001)
        assertEquals("Karim due = 300 - 350 = -50.0", -50.0, karim.netBalance, 0.001)
        assertFalse(karim.isRefund)
    }
}
