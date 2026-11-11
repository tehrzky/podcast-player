# Podcast Player (Native Android Kotlin & Jetpack Compose)

A production-grade, single-module native Android podcast player built with Kotlin, Jetpack Compose, Material 3 Expressive, Jetpack Media3 (ExoPlayer), Room Database, Retrofit, WorkManager, and Google Drive sync.

## Architecture & Features

- **Jetpack Compose + Material 3 Expressive**: Dynamic color (Material You), OLED Pure Black mode (`#000000`), spring morphing FABs and play buttons, responsive 2/3/4-column adaptive grids, landscape two-pane layout.
- **Strict DAO Sort Order Contract**: No queries in the codebase hardcode `ORDER BY publishedDate DESC`. Every episode query dynamically handles `OLDEST_FIRST`, `NEWEST_FIRST`, `EPISODE_ASC`, `EPISODE_DESC`, `TITLE_ASC`, `UNPLAYED_FIRST`, and `DOWNLOADED_FIRST`.
- **Smart Queue Builder**: Binging engine that preserves manual queue items while walking episode lists upward (`UP`, oldest-first) or downward (`DOWN`, newest-first) based on per-podcast and global direction.
- **Media3 ExoPlayer & MediaSessionService**: Background playback, headphone disconnect pause, audio focus handling, playback speed overrides (0.5x–3.5x), sleep timers, and position persistence every 10s.
- **Interactive Transcripts**: Real-time VTT / SRT / JSON podcast transcript rendering with active line highlighting and tap-to-seek timestamp navigation.
- **Google Drive Sync & Backup**: Bidirectional sync to Google Drive AppData folder (`sync_state.json`) with conflict resolution dialogs, plus OPML 2.0 and JSON backup/restore (Merge & Replace modes).
- **Download Manager & Retention**: Background downloading, offline playback, and daily WorkManager retention cleanup with access timer resets.

---

## Building the APK via GitHub Actions CI

Every commit pushed to `main` triggers `.github/workflows/build-apk.yml`, which compiles the project with Gradle and uploads `app-debug.apk`.

### 1. Setting up Repository Secrets (Optional)
To provide API keys to the CI build, navigate to **Settings > Secrets and variables > Actions** in your GitHub repository and add:
- `PODCASTINDEX_KEY`: Your Podcast Index API Key
- `PODCASTINDEX_SECRET`: Your Podcast Index API Secret
- `GOOGLE_WEB_CLIENT_ID`: Google Sign-In Web Client ID for Drive App Data Sync

### 2. Triggering a Build
- Push any commit to `main` or open a Pull Request.
- Or trigger manually: Go to **Actions** tab -> **Build APK** -> Click **Run workflow**.
- Creating a version tag (e.g., `git tag v1.0.0 && git push --tags`) will compile the APK and automatically attach both Debug and Release APKs to a GitHub Release!

### 3. Downloading the Built APK
1. Open your repository on GitHub.
2. Click the **Actions** tab.
3. Select the latest **Build APK** workflow run.
4. Scroll down to the **Artifacts** section at the bottom of the summary page.
5. Click **app-debug-apk** to download the zip containing `app-debug.apk`.
6. Transfer to your Android device and install!

### 4. Signing a Release APK
For automated signed release APKs in CI, configure the following GitHub secrets:
- `RELEASE_KEYSTORE_PATH`: Path to `.jks` file (or decode from base64 secret in workflow)
- `RELEASE_KEYSTORE_PASSWORD`: Keystore password
- `RELEASE_KEY_ALIAS`: Key alias
- `RELEASE_KEY_PASSWORD`: Key password

Alternatively, build a signed release locally using Android Studio:
**Build > Generate Signed Bundle / APK > APK > select your keystore**.

---

## Local Development & CLI Build

```bash
# Clone the repository
git clone https://github.com/your-username/podcast-player.git
cd podcast-player

# Configure local.properties
cp local.properties.example local.properties

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest
```

The debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

This project is a Kotlin/Compose Android app. ./gradlew assembleDebug produces app/build/outputs/apk/debug/app-debug.apk. The GitHub Actions workflow will build and attach this APK to each push.
