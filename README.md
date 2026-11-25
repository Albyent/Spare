# Spare

Spare is an offline Android app for personal spending, account balances, and optional monthly category limits.

Install [Spare.apk](Spare.apk) on Android 8.0 or newer. Choose a currency and create your first account, or restore a Spare backup.

- **Overview** opens on this month’s spending. Tap a category to review the transactions behind it.
- **Add** records expenses, income, refunds, and transfers. A credit card payment is a transfer from the paying account to the card.
- **Activity** lets you search, filter, and edit transactions. **Accounts** shows balances and lets you manage bank, cash, e-wallet, and card accounts.
- **Settings** contains the System, Light, and Dark themes, categories, monthly limits, full JSON backup and restore, and CSV export.

## Build

Use JDK 17 or newer and Android SDK Platform 35. Set `ANDROID_HOME` or open the project in Android Studio.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The development build is at `app/build/outputs/apk/debug/app-debug.apk`. The signed APK uses this workspace’s release key in `~/.local/share/spare/signing`. Reuse that key to sign upgrades that install over the existing app.
