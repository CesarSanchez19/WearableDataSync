# Wearable Data Sync – Two-Way Reset Signal (Wear OS + Android)

> Two-app Kotlin and Jetpack Compose project: a Wear OS watch app and an Android phone app that talk to each other through the Google Wearable `MessageClient`. The watch sends commands (`+5`, `-5` and `RESET`), the phone updates a counter between 0 and 100 and confirms every command, and the watch shows the result. Both interfaces use the NomadKit design system.
---

## ℹ️ Information & Prerequisites

Before you start, make sure you have the following installed on your machine:

| Tool           | Required Version                         | Download                                    |
|----------------|------------------------------------------|---------------------------------------------|
| Android Studio | Recent stable version                    | https://developer.android.com/studio        |
| JDK            | 25 (toolchain), included with Android Studio | (no separate install needed)            |
| Android SDK    | API 37 (compile/target), API 34+ minimum | Installed from Android Studio → SDK Manager |
| Git            | any                                      | https://git-scm.com                         |

> **How to verify Git is installed?**
> ```bash
> git --version
> ```

You also need **two emulators**, paired with each other:

- **Phone:** Medium Phone (a system image **with Google Play**).
- **Watch:** Wear OS Small Round (API 34).

> Pair them from **Tools > Device Manager > (watch menu) > Pair Wearable**. A chain-link icon means the pairing worked.

---

## 📥 Project Creation & Installation

### 1. Clone the repository

```bash
git clone https://github.com/YOUR-USERNAME/WearableDataSync.git
```

### 2. Open it in Android Studio

1. Open Android Studio.
2. Select **File > Open** and choose the `WearableDataSync` folder.
3. Wait for the **Gradle sync** to finish without errors.

> Android Studio creates the `local.properties` file by itself with the path of your Android SDK. This file is not part of the repository.

### 3. Run the apps

1. Select the **`mobile`** configuration and run it on the **phone** emulator. Keep the app open in the foreground.
2. Select the **`wear`** configuration and run it on the **watch** emulator.

> `mobile` goes to the phone and `wear` goes to the watch. Installing the phone app on the watch shows a "missing uses-feature watch" warning.

---

## 📁 Project Structure

```
WearableDataSync/
├── mobile/
│   ├── build.gradle.kts
│   └── src/main/java/com/example/wearabledatasync/
│       ├── MainActivity.kt          # Listener, bounded counter, counter screen
│       ├── WearableConstants.kt     # Message paths and counter rules (shared protocol)
│       └── ui/theme/
│           ├── Color.kt             # NomadKit color tokens
│           ├── Theme.kt             # NomadKit light color scheme
│           └── Type.kt              # Typography
├── wear/
│   ├── build.gradle.kts
│   └── src/main/java/com/example/wearabledatasync/
│       ├── WearableConstants.kt     # Same protocol constants as the phone
│       └── presentation/
│           ├── MainActivity.kt      # Commands, confirmation handling and watch UI
│           └── theme/
│               ├── NomadColors.kt   # NomadKit color tokens for the watch
│               └── Theme.kt         # Wear Material 3 theme
├── gradle/
│   └── libs.versions.toml           # Dependency versions
├── build.gradle.kts                 # Root build configuration
├── settings.gradle.kts              # Project settings
├── gradlew / gradlew.bat            # Gradle wrapper scripts
└── README.md
```

---

## 🔁 Communication Protocol

| Watch button | Path sent (watch → phone) | Phone action                          | Confirmation (phone → watch)  |
|--------------|---------------------------|---------------------------------------|-------------------------------|
| `SEND RESET` | `/reset_counter`          | Sets the counter to 0                 | `RESET_OK`                    |
| `+5`         | `/increment_counter`      | Adds 5 (maximum 100)                  | `COUNT_OK:<new value>`        |
| `-5`         | `/decrement_counter`      | Subtracts 5 (minimum 0)               | `COUNT_OK:<new value>`        |

All confirmations travel on the `/reset_confirm` path.

**Counter rules**

- The phone starts at **100**.
- The phone is the source of truth: it clamps the value between 0 and 100 and sends the real value back.
- At the limits the watch shows a text hint: `(MAX)` or `(MIN)`.

**Watch status messages**

| Message                                          | Meaning                              |
|--------------------------------------------------|--------------------------------------|
| `Searching for phone...`                         | Looking for the connected phone      |
| `<command> signal SENT. Waiting for confirmation...` | Message delivered, waiting for answer |
| `SUCCESS: Phone Confirmed Reset (RESET_OK)!`     | Reset confirmed                      |
| `SUCCESS: Phone count is now <value>`            | `+5` or `-5` confirmed               |
| `Error: No connected phone found!`               | No paired phone available            |

---

## 🛠️ Technologies Used

- [Kotlin](https://kotlinlang.org/) – Programming language
- [Jetpack Compose](https://developer.android.com/jetpack/compose) – Declarative UI for the phone (Material 3)
- [Jetpack Compose for Wear OS](https://developer.android.com/training/wearables/compose) – Declarative UI for the watch (Wear Material 3)
- [Wearable Data Layer `MessageClient`](https://developers.google.com/android/reference/com/google/android/gms/wearable/MessageClient) – Two-way messages between watch and phone
- [kotlinx-coroutines-play-services](https://github.com/Kotlin/kotlinx.coroutines/tree/master/integration/kotlinx-coroutines-play-services) – `Task.await()` for the node lookup
- `lifecycleScope` – Coroutines tied to the activity lifecycle
- Listener registration in `onResume` / `onPause` – The phone and the watch listen only while visible
- [Android Studio](https://developer.android.com/studio) and Gradle – IDE and build system

---

## 🎨 Color Palette (NomadKit)

| Use                       | Name                | Value        |
|---------------------------|---------------------|--------------|
| Primary action            | Sand                | `0xFFD4A373` |
| Secondary action / titles | Ocean               | `0xFF0891B2` |
| Success / highlights      | Forest              | `0xFF166534` |
| Background                | Cream               | `0xFFFFFDF7` |
| Card surface              | White               | `0xFFFFFFFF` |
| Warning state             | Amber               | `0xFFCA8A04` |
| Error state               | Red                 | `0xFFDC2626` |
| Text on Sand (derived)    | Dark ink            | `0xFF2B2118` |
| Soft sand tint (derived)  | Sand soft           | `0xFFE7DCC8` |

**Design rules applied**

- Sand is the dominant color for the primary action (`SEND RESET`).
- Meaning never depends on color alone: buttons have signs (`+5`, `-5`) and states have text.
- Offline-first: the phone shows a skeleton placeholder until the first command arrives.

---

## 👤 Author

Cesar David Sanchez Trejo

---