# Mess Meal System Android App

A comprehensive, cloud-synced bachelor mess and hostel meal management application built natively with Jetpack Compose and Material 3, replacing manual notebooks and fragmented spreadsheets with automated meal rate calculation, daily meal status logging, bazaar expense auditing, deposit tracking, and month-end financial reconciliation.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Summary of Confirmed Architectural & Feature Choices:**
> - **Architecture**: Native Jetpack Compose with Material 3 (mobile-first, buttery 120Hz M3 motion, zero webview overhead, full offline resilience with cloud sync).
> - **Backend & Persistence**: Cloud Firestore with Google Sign-In via Jetpack Credential Manager for multi-member real-time synchronization across roommates and mess managers.
> - **Prioritized Functional Capabilities**:
>   1. Daily meal logging (Breakfast, Lunch, Dinner, Guest meals) with live meal count tracking.
>   2. Daily bazaar (grocery/market) expense logging categorized by items and member purchaser.
>   3. Member deposit tracking and fund balance ledger.
>   4. Dynamic real-time Meal Rate calculation: $\text{Meal Rate} = \frac{\text{Total Bazaar Cost}}{\text{Total Meals Consumed}}$.
>   5. Fixed/utility cost equal splitting (Cook, Gas, Electricity, Internet).
>   6. Meal On/Off switch with cutoff times to prevent food waste.
>   7. End-of-month reconciliation statement computing net dues and refunds per member: $\text{Balance} = \text{Deposit} - (\text{Meal Cost} + \text{Fixed Share})$.

---

## 1. Overview & Core Concept

### What It Does
The **Mess Meal System** manages the day-to-day shared living economy of hostel residents, bachelor flats, and shared mess communities. It tracks who eats each meal, records market grocery purchases with receipt details, tracks member deposit contributions, and automatically calculates the shared meal rate and individual financial balances in real time.

### Target Audience & Persona
- **Mess Managers / Monitors**: Responsible for recording daily bazaar expenses, logging member deposits, and generating the monthly settlement report without calculation errors or disputes.
- **Mess Members / Flatmates**: Need to toggle their meal status (on/off) before cutoff, check their consumed meal count, view live mess expenditure, track their remaining deposit balance, and verify their end-of-month refund or owed dues.

### Key Value Delivered
- **Zero Calculation Disputes**: Eliminates manual ledger errors with mathematical formulas.
- **Transparency**: Every purchase, deposit, and meal count is recorded with timestamps and member attributions visible to all members in real time.
- **Food Waste Prevention**: Members set daily Meal On/Off status before meal cutoffs so the cook purchases and prepares the exact quantity needed.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Authentication & Profile Gate**: Quick one-tap Google Sign-In; user links or creates a Mess group (with unique Mess Code or invitation), choosing their Manager or Member role.
2. **Dashboard / Home Overview**:
   - Hero summary card displaying current active month, live **Meal Rate** (e.g., `$48.50/meal`), Total Mess Meals, Total Bazaar Expenses, and Mess Fund Reserve.
   - Quick Action bar: "Log Today's Meal", "Add Bazaar Entry", "Record Deposit", "Toggle Meal On/Off".
   - Today's Meal Status board showing individual member meal states (Breakfast, Lunch, Dinner, Guests).
3. **Daily Meal Sheet & Guest Tracker**:
   - Date picker to view or edit historical days.
   - Matrix table and interactive cards for each member with steppers (+/-) for Breakfast, Lunch, Dinner, and outside Guests.
   - Status toggle: "Eating Today (3 meals)" or "Off / Away".
4. **Bazaar & Expenses Log**:
   - Chronological list of market runs with date, purchaser name, item breakdown (e.g., Rice, Meat, Vegetables, Oil), and total amount.
   - Add Expense sheet with instant categorization, photo receipt attachment placeholder, and multi-buyer split support.
5. **Deposits & Member Balances**:
   - Central Mess Cash Pool tracker with cumulative deposits per member.
   - "Add Deposit" modal with payment method tags (Cash, bKash/Nagad/UPI, Bank Transfer) and receipt note.
6. **Month-End Reconciliation & Final Settlement**:
   - Comprehensive balance sheet breaking down:
     - Member Name | Total Meals | Meal Cost | Shared Utilities | Total Expense | Total Deposit | Net Refund / Due
   - Color-coded badges: Green for "Refund Due (+)", Amber for "Settled ($0)", Red for "Pay Extra (-)".
   - Export and share summary (formatted text report and CSV data).

### Visual Identity & Theme
- **Aesthetic Direction**: *Warm Modern Culinary & Clean FinTech*. Trustworthy, crisp, and approachable with tactile Material 3 cards, subtle surface elevation, and clear numeric hierarchy.
- **Color Palette**:
  - Primary: Deep Terracotta Amber (`#C05621` / `#D97706` in Light, `#F59E0B` in Dark) symbolizing warmth, dining, and hospitality.
  - Secondary: Emerald Slate (`#059669` / `#10B981`) for positive deposits, balances, and refunds.
  - Surface & Background: Clean Warm Alabaster (`#FBFBF9`) in Light mode; Deep Charcoal Navy (`#12161A` / `#1E252D`) in Dark mode.
  - Accent / Error: Crimson Coral (`#DC2626`) for negative dues and penalties.
- **Typography & Hierarchy**:
  - Display & Headers: Bold, confident sans-serif with high contrast numbers for money and meal rates (`HeadlineMedium`, `TitleLarge`).
  - Data Grid & Figures: Tabular numeric alignment with monospaced digit formatting so balances, counts, and decimals align cleanly.
- **Component Styling & Layout**:
  - Material 3 `ElevatedCard` and `OutlinedCard` with 16dp rounded corners.
  - Seamless bottom navigation bar with icons: **Dashboard**, **Daily Meals**, **Bazaar Log**, **Deposits**, and **Settlement**.
  - Floating Action Buttons for quick daily logging.
  - Full-bleed edge-to-edge layout respecting system status and navigation bar insets.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Architecture: Native Jetpack Compose vs. Tauri / WebView
- **Chosen Approach**: Pure native Android Jetpack Compose with Material 3.
- **Why**: The user requested a fully functional Android app converted from a website. While the user inquired about Rust/Tauri, standard Android compilation and mobile deployment achieve top-tier performance, natural swipe gestures, Material ripples, adaptive orientation, and zero WebKit memory footprint when built natively in Jetpack Compose.
- **Alternatives Considered**: Embedded WebView was considered but rejected because native Compose provides instant 120fps responsiveness, offline caching, and native keyboard/gesture handling.

### Decision 2: Backend: Cloud Firestore with Local Fallback vs. Local Only
- **Chosen Approach**: Cloud Firestore integrated via Firebase Auth (Google Sign-In via Credential Manager) with structured collection models, paired with robust offline persistence.
- **Why**: A mess meal system inherently involves multiple roommates/members who need to see the same ledger, toggle meals from their own phones, and audit manager entries in real time.
- **Alternatives Considered**: Local Room-only database prevents multi-member collaboration; Firestore real-time listeners provide live synchronization across all devices.

### Decision 3: Standard Mess Calculation Formula Engine
- **Chosen Approach**: Standardized Hostel/Bachelor Mess Formula:
  1. $TM = \sum(\text{Member Meals} + \text{Guest Meals})$
  2. $TBC = \sum(\text{Daily Bazaar Expenses})$
  3. $MR = \begin{cases} \frac{TBC}{TM} & \text{if } TM > 0 \\ 0 & \text{otherwise} \end{cases}$
  4. $\text{Fixed Per Head} = \frac{\text{Total Utility/Cook Bills}}{\text{Active Members}}$
  5. $\text{Member Cost} = (\text{Member Meals} \times MR) + \text{Fixed Per Head} + \text{Guest Meal Cost}$
  6. $\text{Net Balance} = \text{Member Total Deposits} - \text{Member Total Cost}$
- **Why**: Matches universal mess accounting standards globally, preventing ambiguities.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├─────────────────┬──────────────────┬─────────────────┬────────────────┤
│ DashboardScreen │ DailyMealsScreen │ BazaarLogScreen │ SettlementView │
└────────┬────────┴────────┬─────────┴────────┬────────┴────────┬───────┘
         │                 │                  │                 │
┌────────┴─────────────────┴──────────────────┴─────────────────┴───────┐
│              MessMealViewModel (StateFlow & M3 UI States)             │
│   - mealRate: StateFlow<Double>        - currentMonth: String         │
│   - members: StateFlow<List<Member>>   - dailyLogs: StateFlow<...>    │
│   - bazaarEntries: StateFlow<...>      - deposits: StateFlow<...>     │
├────────────────────────────────────────────────────────────────────────┤
│                 MessCalculationEngine (Pure Kotlin Logic)              │
│   - computeMealRate()                  - computeSettlement()          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────┴────────────────────────────────────┐
│                  MessRepository & Cloud Firestore                      │
│   - Firestore instance: R.string.firestore_database_id                 │
│   - Collections:                                                      │
│     * /messes/{messId}/members/{memberId}                             │
│     * /messes/{messId}/daily_meals/{dateString}                       │
│     * /messes/{messId}/bazaar_expenses/{expenseId}                    │
│     * /messes/{messId}/deposits/{depositId}                           │
│     * /messes/{messId}/utility_bills/{billId}                         │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models & Schema (`firebase-blueprint.json` representation)

1. **`Mess`**:
   - `id`: Document ID (e.g., `mess_alpha`)
   - `name`: String (e.g., "Green View Mess")
   - `managerUid`: String (Creator/Manager Google UID)
   - `activeMonth`: String (e.g., "2026-11")
   - `createdAt`: Server Timestamp

2. **`Member`**:
   - `id`: Document ID
   - `userId`: Google Auth UID
   - `displayName`: String
   - `roomNumber`: String
   - `role`: String (`"MANAGER"` or `"MEMBER"`)
   - `isActive`: Boolean

3. **`DailyMealRecord`**:
   - `id`: Date string (e.g., `2026-11-08`)
   - `date`: Timestamp
   - `memberMeals`: Map of `memberId` -> `{ breakfast: Double, lunch: Double, dinner: Double, guestMeals: Double, isMealOn: Boolean }`
   - `totalDayMeals`: Double
   - `updatedBy`: String

4. **`BazaarExpense`**:
   - `id`: Document ID
   - `date`: Timestamp
   - `buyerMemberId`: String
   - `buyerName`: String
   - `amount`: Double
   - `itemsDescription`: String (e.g., "Chicken 2kg, Rice 5kg, Spices")
   - `category`: String (Groceries, Meat & Fish, Vegetables, Misc)
   - `createdAt`: Server Timestamp

5. **`Deposit`**:
   - `id`: Document ID
   - `memberId`: String
   - `memberName`: String
   - `amount`: Double
   - `date`: Timestamp
   - `paymentMethod`: String (Cash, Mobile Banking, Transfer)
   - `notes`: String

6. **`UtilityBill`**:
   - `id`: Document ID
   - `title`: String (e.g., "Cook Salary", "Gas & Cylinder", "WiFi")
   - `amount`: Double
   - `month`: String

---

## 5. Execution Steps & Verification Plan

1. **Phase 1: Firebase Backend Provisioning**:
   - Trigger `show_aistudio_ui` for Firebase terms confirmation.
   - Run `ProvisionFirebase` via `rpc_action` to provision Cloud Firestore and Auth.
   - Setup `firebase-blueprint.json` and hardened `firestore.rules`.
   - Validate security rules and deploy with `DeployRules`.
2. **Phase 2: Core Data Layer & Calculation Engine**:
   - Implement `MessModels.kt` (data classes with Firestore annotations and default parameters).
   - Implement `MessCalculationEngine.kt` with unit-tested math formulas.
   - Implement `MessRepository.kt` observing real-time Firestore collections.
3. **Phase 3: ViewModel & UI Presentation**:
   - Implement `MessMealViewModel.kt` handling auth state, active mess selection, and real-time state flows.
   - Build Material 3 UI screens:
     - `DashboardScreen`: Live meal rate banner, monthly stats, quick-entry actions.
     - `DailyMealSheetScreen`: Interactive daily meal matrix with member steppers and on/off toggles.
     - `BazaarLogScreen`: Chronological grocery expense ledger with add/edit dialogs.
     - `DepositLedgerScreen`: Member deposits, payment channels, and fund balance.
     - `SettlementScreen`: End-of-month breakdown with exportable refund/dues report.
4. **Phase 4: Compilation & Device Verification**:
   - Run `compile_applet` to ensure spotless Kotlin build and runtime functionality.
