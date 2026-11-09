# PayTrack

**PayTrack** is an Android personal expense tracker that automatically detects UPI payments from
device notifications and turns them into logged, categorized expenses.

## Features

- Automatic payment detection via Android `NotificationListenerService` (event-driven, no polling).
- Config-driven parsing from `app/src/main/assets/parsers.json` (GPay, PhonePe, Paytm, FamPay + generic fallback).
- QuickLog popup: heads-up notification backed by full-screen-intent transparent bottom sheet, auto-dismisses after ~25s.
- Review queue for ambiguous, amount-less, or dismissed entries.
- Budgets with weekly/monthly/yearly periods, per-category scope, warning thresholds, notifications, Home indicators.
- Transactions, categories, cash-on-hand, CSV export. Fully offline.

## How it works

1. User grants PayTrack **Notification access** via the system settings deep link.
2. PayTrack receives posted notifications from whitelisted apps enabled in Settings.
3. Parser extracts amount, payee, and debit vs. credit.
4. Debits with an amount are pre-saved and surfaced via QuickLog notification + full-screen-intent popup; saving confirms, dismissing/timeout leaves as `needs_review`.
5. After each confirmed save budgets are re-evaluated; a WorkManager periodic safety-net sweep runs every 6 hours.

## Tech stack

- Kotlin 2.0, Jetpack Compose (Material 3, Navigation Compose), Hilt, Room, WorkManager, DataStore, Navigation Compose.
- AGP 8.7.3, compileSdk 36, minSdk 26, JVM target 17.

## Getting started

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Requires Android Studio with JDK 17 and Android SDK (compileSdk 36).

### Permissions

- Notification access (PayTrack listener) - required for detection.
- Post notifications (Android 13+) - required for QuickLog and budget alerts.

For reliable background delivery on Xiaomi / Oppo / Vivo / OnePlus, enable Autostart and keep PayTrack un-restricted in Recent apps.

## Configuration

Payment parsing rules live in `app/src/main/assets/parsers.json` with per-app regex entries, generic fallback patterns, and a dedup window.

## Project structure

```
app/src/main/java/com/paytrack/expensetracker/
  PayTrackApp.kt            # Application class, notification channels, worker scheduling
  MainActivity.kt           # Nav host with bottom tabs (Home, Txns, Review, Budgets, Settings)
  data/                     # Room DB, DAOs, repositories, DataStore prefs
  di/                       # Hilt modules
  notify/                   # QuickLogNotifier, BudgetChecker, BudgetStatus
  parser/                   # PaymentParser, ConfigParser, ParserRegistry, DedupFilter
  receiver/                 # RebootReceiver
  service/                  # ExpenseNotificationListener (NotificationListenerService)
  ui/
    quicklog/               # QuickLogActivity (full-screen-intent sheet)
    screens/                # Compose screens (Home, Txns, Review, Budgets, Categories, Onboarding, Settings, ...)
  util/                     # CsvExport, ListenerAccess
  worker/                   # BudgetCheckWorker (WorkManager)

app/src/main/assets/parsers.json  # Config-driven per-app payment regex patterns
```

## Testing

```bash
./gradlew test
```
