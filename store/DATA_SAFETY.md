# Play Console: Data safety form answers

**Does your app collect or share any of the required user data types?** No.

**Is all of the user data collected by your app encrypted in transit?** Not applicable: no data is collected or transmitted. The app has no internet permission.

**Do you provide a way for users to request that their data is deleted?** Yes. Data is stored on the device only. Users can delete it under Settings → Your data → Clear my data, or by uninstalling.

## Data types

For each category (Location, Personal info, Financial info, Health and fitness, Messages, Photos and videos, Audio, Files and docs, Calendar, Contacts, App activity, Web browsing, App info and performance, Device or other IDs): **Not collected. Not shared.**

## Why "not collected" is accurate

Google's definition of "collected" is data transmitted off the device. Sukoon:
• has no backend, account system, analytics, crash reporting or ads SDK
• removes `android.permission.INTERNET` in the manifest, so no library can send data
• keeps favourites, collections, counters and settings in a local Room database and DataStore
• stores a salted PBKDF2 hash of the app-lock PIN, never the PIN
• writes backups only to a file location the user picks through the system file picker
• shares a dua's text only when the user taps Share and picks a target app (user-initiated, not collection)

## Security practices

• Data is not transmitted, so in-transit encryption doesn't apply.
• Cloud auto-backup is disabled (`allowBackup=false`, and cloud backup excludes everything in `data_extraction_rules.xml`).
• Not independently security-reviewed (MASA): No.
