# PayTrack

**PayTrack** is an Android personal expense tracker that automatically detects UPI payments from
device notifications and turns them into logged, categorized expenses.

## Features

- Automatic payment detection via Android `NotificationListenerService` (no polling).
- Config-driven parsing from `app/src/main/assets/parsers.json` (GPay, PhonePe, Paytm, FamPay + generic fallback).
- QuickLog popup: heads-up notification backed by full-screen-intent bottom sheet, auto-dismisses after ~25s.
- Review queue for ambiguous, amount-less, or dismissed entries.
- Budgets with weekly/monthly/yearly periods, per-category scope, warning thresholds, notifications, Home indicators.
- Transactions, categories, cash-on-hand, CSV export. Fully offline.

## Getting started

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Requires Android Studio with JDK 17 and Android SDK (compileSdk 36).

## Tech stack

Kotlin 2.0, Jetpack Compose (Material 3), Hilt, Room, WorkManager, DataStore, Navigation Compose.

## Testing

```bash
./gradlew test
```
