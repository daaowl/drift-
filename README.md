# Sukoon

Every dua from *Seeking Serenity*, sorted by what you're going through, readable in seconds, fully offline.

Kotlin · Jetpack Compose · Material 3 · MVVM + Clean Architecture · Hilt · Room (FTS4) · DataStore · WorkManager · Glance · Navigation Compose (type-safe) · Coil 3 · Baseline Profiles. Min SDK 26, target/compile SDK 36. No INTERNET permission.

- Product spec: [docs/PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md)
- Design system: [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md)
- Store listing, content rating, permissions: [store/LISTING.md](store/LISTING.md)
- Data safety answers: [store/DATA_SAFETY.md](store/DATA_SAFETY.md)
- Privacy policy (host this URL for Play): [PRIVACY_POLICY.md](PRIVACY_POLICY.md)

## Build and run

Requirements: Android Studio (Narwhal 2025.1.2 or newer) with Android SDK 36, JDK 17.

```bash
# 1. Open the folder in Android Studio and let it sync, or from a terminal:
./gradlew assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug             # install on a connected device/emulator (package app.sukoon.debug)

# 2. Tests
./gradlew testDebugUnitTest        # JVM unit tests (use cases, view models, seed-data validation, backup codec, PIN hashing)
./gradlew connectedDebugAndroidTest  # Room DAO + migration tests, Compose UI smoke tests (device required)
```

The first build exports the Room schema to `app/schemas/app.sukoon.data.local.SukoonDatabase/1.json`. Commit it: `MigrationTest` and every future migration test read from it.

### Release build

1. Create a keystore: `keytool -genkeypair -v -keystore sukoon-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias sukoon`
2. `cp keystore.properties.template keystore.properties` and fill in the path and passwords (`keystore.properties` and `*.jks` are git-ignored).
3. Optional: generate the baseline profile on a connected device: `./gradlew :app:generateReleaseBaselineProfile`
4. `./gradlew bundleRelease`, which gives you `app/build/outputs/bundle/release/app-release.aab` (R8 minified, resources shrunk).

Without `keystore.properties`, release builds fall back to the debug key so `assembleRelease` still works locally.

### Versioning

`gradle.properties` holds `sukoon.versionMajor/Minor/Patch`. `versionName = MAJOR.MINOR.PATCH`, `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.2.3 → 10203). Bump PATCH for fixes, MINOR for features, MAJOR for breaking data changes. Debug builds get the `.debug` suffix so they install next to the store version.

### Contact e-mail

Set `sukoon.supportEmail=you@example.com` in `gradle.properties` to show About → Contact and feedback. Left empty, the row is hidden.

### Updating dua content

Edit `app/src/main/assets/duas.json` and bump `contentVersion`. On next launch `SeedLoader` upserts content in a single transaction; favourites, collections and counters are kept, and entries you remove cascade out cleanly. `SeedDataValidationTest` fails the build on missing fields, non-Arabic Arabic, duplicate ids or empty sections.

### Database migrations

Bump `SukoonDatabase.VERSION`, add a `Migration` to `Migrations.ALL`, build once to export the new schema, then add a `runMigrationsAndValidate` case to `MigrationTest`. Destructive fallback is deliberately never enabled.

### CI

`.github/workflows/android.yml` runs unit tests, builds the debug APK and uploads it as an artifact on every push.

## Architecture

```
ui (Compose screens + @HiltViewModel)  →  domain (models, repository interfaces, use cases)  ←  data (Room, DataStore, seed, backup)
```

- **Single source of truth:** Room for content and user data, DataStore for settings. Screens observe `StateFlow<LoadState<T>>` and render Loading / Error / Empty / Success.
- **Process death:** reader position, search query, selected tab and list-detail selection live in `SavedStateHandle`.
- **Crash-safe writes:** Room transactions for seeding, import and clear; DataStore atomic edits; backups are fully serialised before the output stream is touched.
- **Search:** text is normalised (diacritics, apostrophes, doubled letters, ee/oo/ou variants) into an FTS4 table, with prefix matching per token.
- **Reminders:** WorkManager periodic work aligned to the chosen minute (no exact-alarm permission). The widget refreshes daily and whenever widget settings change.
- **App lock:** locks whenever the process goes to the background; BiometricPrompt first, PBKDF2-hashed PIN fallback; system file pickers don't trigger a re-lock.

## Project structure

```
sukoon/
├── .github/
│   └── workflows/
│       └── android.yml
├── app/
│   ├── src/
│   │   ├── androidTest/
│   │   │   └── java/
│   │   │       └── app/
│   │   │           └── sukoon/
│   │   │               ├── DatabaseTest.kt
│   │   │               └── UiSmokeTest.kt
│   │   ├── main/
│   │   │   ├── assets/
│   │   │   │   └── duas.json
│   │   │   ├── java/
│   │   │   │   └── app/
│   │   │   │       └── sukoon/
│   │   │   │           ├── core/
│   │   │   │           │   ├── designsystem/
│   │   │   │           │   │   ├── component/
│   │   │   │           │   │   │   ├── Components.kt
│   │   │   │           │   │   │   ├── Ornament.kt
│   │   │   │           │   │   │   ├── SectionIcons.kt
│   │   │   │           │   │   │   ├── SharedElements.kt
│   │   │   │           │   │   │   ├── StateViews.kt
│   │   │   │           │   │   │   └── TopBar.kt
│   │   │   │           │   │   └── theme/
│   │   │   │           │   │       ├── Color.kt
│   │   │   │           │   │       ├── Theme.kt
│   │   │   │           │   │       ├── Tokens.kt
│   │   │   │           │   │       └── Type.kt
│   │   │   │           │   └── feedback/
│   │   │   │           │       └── Feedback.kt
│   │   │   │           ├── data/
│   │   │   │           │   ├── backup/
│   │   │   │           │   │   └── BackupRepositoryImpl.kt
│   │   │   │           │   ├── local/
│   │   │   │           │   │   ├── Daos.kt
│   │   │   │           │   │   ├── Entities.kt
│   │   │   │           │   │   ├── Mappers.kt
│   │   │   │           │   │   └── SukoonDatabase.kt
│   │   │   │           │   ├── prefs/
│   │   │   │           │   │   └── SettingsDataStore.kt
│   │   │   │           │   ├── repository/
│   │   │   │           │   │   ├── DuaRepositoryImpl.kt
│   │   │   │           │   │   ├── SettingsRepositoryImpl.kt
│   │   │   │           │   │   └── UserDataRepositoryImpl.kt
│   │   │   │           │   ├── search/
│   │   │   │           │   │   └── SearchNormalizer.kt
│   │   │   │           │   └── seed/
│   │   │   │           │       ├── SeedLoader.kt
│   │   │   │           │       └── SeedModels.kt
│   │   │   │           ├── di/
│   │   │   │           │   └── Modules.kt
│   │   │   │           ├── domain/
│   │   │   │           │   ├── model/
│   │   │   │           │   │   ├── LoadState.kt
│   │   │   │           │   │   ├── Models.kt
│   │   │   │           │   │   └── UserSettings.kt
│   │   │   │           │   ├── repository/
│   │   │   │           │   │   └── Repositories.kt
│   │   │   │           │   └── usecase/
│   │   │   │           │       └── UseCases.kt
│   │   │   │           ├── notifications/
│   │   │   │           │   ├── NotificationChannels.kt
│   │   │   │           │   ├── ReminderScheduler.kt
│   │   │   │           │   └── ReminderWorker.kt
│   │   │   │           ├── security/
│   │   │   │           │   ├── AppLockManager.kt
│   │   │   │           │   └── BiometricAuth.kt
│   │   │   │           ├── ui/
│   │   │   │           │   ├── about/
│   │   │   │           │   │   └── AboutScreens.kt
│   │   │   │           │   ├── counter/
│   │   │   │           │   │   ├── CounterScreen.kt
│   │   │   │           │   │   └── CounterViewModel.kt
│   │   │   │           │   ├── home/
│   │   │   │           │   │   ├── HomeScreen.kt
│   │   │   │           │   │   └── HomeViewModel.kt
│   │   │   │           │   ├── lock/
│   │   │   │           │   │   ├── LockScreen.kt
│   │   │   │           │   │   └── kotlin-0.0.1.tar.gz
│   │   │   │           │   ├── main/
│   │   │   │           │   │   ├── MainViewModel.kt
│   │   │   │           │   │   └── SukoonAppUi.kt
│   │   │   │           │   ├── navigation/
│   │   │   │           │   │   └── Routes.kt
│   │   │   │           │   ├── onboarding/
│   │   │   │           │   │   └── OnboardingScreen.kt
│   │   │   │           │   ├── permission/
│   │   │   │           │   │   └── NotificationPermission.kt
│   │   │   │           │   ├── reader/
│   │   │   │           │   │   ├── CollectionSheet.kt
│   │   │   │           │   │   ├── DuaPage.kt
│   │   │   │           │   │   ├── ReaderScreen.kt
│   │   │   │           │   │   └── ReaderViewModel.kt
│   │   │   │           │   ├── saved/
│   │   │   │           │   │   ├── CollectionScreen.kt
│   │   │   │           │   │   ├── NameDialog.kt
│   │   │   │           │   │   ├── SavedScreen.kt
│   │   │   │           │   │   └── SavedViewModels.kt
│   │   │   │           │   ├── search/
│   │   │   │           │   │   ├── SearchScreen.kt
│   │   │   │           │   │   └── SearchViewModel.kt
│   │   │   │           │   ├── section/
│   │   │   │           │   │   ├── SectionScreen.kt
│   │   │   │           │   │   └── SectionViewModel.kt
│   │   │   │           │   └── settings/
│   │   │   │           │       ├── PinSetup.kt
│   │   │   │           │       ├── SettingsComponents.kt
│   │   │   │           │       ├── SettingsScreen.kt
│   │   │   │           │       └── SettingsViewModel.kt
│   │   │   │           ├── widget/
│   │   │   │           │   ├── DuaWidget.kt
│   │   │   │           │   └── WidgetUpdater.kt
│   │   │   │           ├── MainActivity.kt
│   │   │   │           └── SukoonApp.kt
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── ic_launcher_background.xml
│   │   │   │   │   ├── ic_launcher_foreground.xml
│   │   │   │   │   ├── ic_launcher_monochrome.xml
│   │   │   │   │   ├── ic_notification.xml
│   │   │   │   │   ├── ic_splash.xml
│   │   │   │   │   ├── onboarding_browse.xml
│   │   │   │   │   ├── onboarding_read.xml
│   │   │   │   │   ├── onboarding_remind.xml
│   │   │   │   │   └── widget_preview.xml
│   │   │   │   ├── font/
│   │   │   │   │   └── (amiri_quran, amiri_regular, amiri_bold, fraunces_*, inter_*)
│   │   │   │   ├── mipmap-anydpi-v26/
│   │   │   │   │   ├── ic_launcher.xml
│   │   │   │   │   └── ic_launcher_round.xml
│   │   │   │   ├── raw/
│   │   │   │   │   ├── (counter_tick.wav, counter_done.wav)
│   │   │   │   │   ├── changelog.txt
│   │   │   │   │   ├── licenses.txt
│   │   │   │   │   └── privacy_policy.txt
│   │   │   │   ├── values/
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   ├── resources.xml
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   └── themes.xml
│   │   │   │   ├── values-night/
│   │   │   │   │   └── colors.xml
│   │   │   │   ├── values-ur/
│   │   │   │   │   └── strings.xml
│   │   │   │   └── xml/
│   │   │   │       ├── backup_rules.xml
│   │   │   │       ├── data_extraction_rules.xml
│   │   │   │       ├── dua_widget_info.xml
│   │   │   │       └── locales_config.xml
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │       └── java/
│   │           └── app/
│   │               └── sukoon/
│   │                   ├── DataTests.kt
│   │                   ├── DomainTests.kt
│   │                   ├── Fakes.kt
│   │                   └── ViewModelTests.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── baselineprofile/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── app/
│   │       │       └── sukoon/
│   │       │           └── baselineprofile/
│   │       │               └── BaselineProfileGenerator.kt
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── docs/
│   ├── DESIGN_SYSTEM.md
│   └── PRODUCT_SPEC.md
├── gradle/
│   ├── wrapper/
│   │   └── (gradle-wrapper.jar, gradle-wrapper.properties)
│   └── libs.versions.toml
├── store/
│   ├── DATA_SAFETY.md
│   └── LISTING.md
├── .gitignore
├── PRIVACY_POLICY.md
├── README.md
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── keystore.properties.template
└── settings.gradle.kts
```

## Content and credit

Content: *Seeking Serenity*, compiled from the duas of the late Zubeida Mohamed and dedicated to Mohamed Shafee Mohamed and Zubeida Mohamed; proceeds support the Nanima Foundation (nanima.co.za). Fonts: Amiri and Amiri Quran, Fraunces, Inter (SIL OFL 1.1).
