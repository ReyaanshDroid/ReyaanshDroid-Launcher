# Implementation Plan - Sleek Android Launcher

This plan outlines the steps to transform the current "Hello World" app into a fully functional, sleek Android Launcher suitable for a custom ROM.

## User Review Required

> [!IMPORTANT]
> To use this launcher as your default home screen, you will need to set it as the "Home app" in your Android settings after installation.

## Proposed Changes

### Configuration

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/AndroidManifest.xml)
- Add the necessary intent filters to register the app as a Home/Launcher activity.
- Add `QUERY_ALL_PACKAGES` permission (required for Android 11+ to list all apps).

### Data & Logic

#### [NEW] [AppInfo.java](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/java/com/reyaansh72/reyaanshdroidlauncher/AppInfo.java)
- A data class to hold app metadata: Label, Package Name, and Icon.

#### [MODIFY] [MainActivity.java](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/java/com/reyaansh72/reyaanshdroidlauncher/MainActivity.java)
- Implement logic to fetch all installed apps with a launcher intent.
- Set up a `RecyclerView` with a `GridLayoutManager` (4 columns).
- Handle item clicks to launch the selected application.

### UI Components

#### [MODIFY] [activity_main.xml](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/res/layout/activity_main.xml)
- Replace the "Hello World" TextView with a `RecyclerView`.
- Use a transparent/semi-transparent background for a modern look.

#### [NEW] [item_app.xml](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/res/layout/item_app.xml)
- Design a sleek item layout for individual apps (circular/rounded icon and centered label).

#### [NEW] [AppAdapter.java](file:///C:/Users/Reyaansh/Desktop/Projects/ReyaanshDroid/Launcher/app/src/main/java/com/reyaansh72/reyaanshdroidlauncher/AppAdapter.java)
- A standard RecyclerView Adapter to bind `AppInfo` data to `item_app.xml`.

---

## Verification Plan

### Automated Tests
- I will verify the build succeeds using `./gradlew assembleDebug`.

### Manual Verification
- Deploy to a device/emulator.
- Verify the app list is populated.
- Click an app to verify it launches correctly.
- Press the Home button and select "Reyaansh Launcher" to verify it works as a system home screen.
