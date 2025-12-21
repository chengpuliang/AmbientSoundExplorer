# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project overview

- **Android app (`app/`)**: Kotlin / Jetpack Compose Android application that lets users browse and
  play ambient sounds and manage reminder notifications.
- **Backend prototype (`api/`)**: Simple Flask + Flask-RESTful API that serves a static list of
  music metadata from `api/music.json` and is intended to back the Android app.

The Android app currently hardcodes the API base URL and key in `MainActivity.kt` via
`ApiService(endpoint, apiKey)`. The Python API enforces an `X-API-KEY` header and listens on port
`8000` by default.

## Android app architecture (`app/`)

### Entry point, navigation, and theming

- **`MainActivity`** (`app/src/main/java/com/example/ambientsoundexplorer/MainActivity.kt`)
    - Standard `ComponentActivity` using Jetpack Compose.
    - Calls `Player.init(LocalContext.current)` once at startup to create a global `MediaSession`
      and configure the shared `MediaPlayer`.
    - Wraps all UI in `AmbientSoundExplorerTheme`, defined under `ui/theme`.
    - Uses a `PageViewModel` to maintain a **stack of composable screens** (
      `MutableList<@Composable () -> Unit>`). This is a very lightweight, non-navigation-component
      screen stack:
        - `screen` = the last composable in the list.
        - `push { ... }` adds a new screen; `pop()` removes the top screen.
        - `BackHandler` in `MainActivity` delegates back presses to `PageViewModel.pop()`.
    - Uses `Crossfade(pageViewModel.screen)` to animate between the active screen composables.

- **`AmbientSoundExplorerTheme` and UI theming** (`ui/theme/Theme.kt`, `Color.kt`, `Type.kt`)
    - Standard Material 3 theming with light/dark color schemes and typography.
    - Uses dynamic color (Material You) on Android 12+ via `dynamicDarkColorScheme` /
      `dynamicLightColorScheme`.

### Screens and UI flow

- **`MainScreen`** (inside `MainActivity.kt`)
    - Owns the current bottom-navigation tab (`Page.sounds` or `Page.reminders`).
    - Instantiates a single `ApiService` with a **hardcoded base URL and API key**:
        - Example in repo: `ApiService("http://57.180.75.22:8000", "YZ5TNCN55K")`.
    - Renders a `Scaffold` with a Material 3 bottom `NavigationBar`:
        - **Sounds tab** → `SoundScreen(apiService, pageViewModel)`.
        - **Reminders tab** → `ReminderScreen(apiService)`.

- **Sound list screen** (`ui/theme/SoundScreen.kt`)
    - Main responsibilities:
        - Fetch and display a **filterable, sortable list of `Music`** items from the API.
        - Provide a search box (`filter_term`) and a sort toggle (`ascending` / `descending`).
        - Inline playback controls for each track via the shared `Player.player`.
        - Route into the detailed player screen by `pageViewModel.push { PlayerScreen(...) }`.
    - Key behaviors:
        - Uses `LaunchedEffect(searchText)` to refetch the list whenever the search query changes.
        - Maintains loading state and displays a `CircularProgressIndicator` during network calls.
        - For each `Music` row, tapping the card pushes `PlayerScreen` onto the `PageViewModel`
          stack.
        - The play/pause button per row:
            - If that row is already playing, stops playback and clears `playingId`.
            - Otherwise resets the `MediaPlayer`, sets data source to
              `"$endpoint/music/audio?music_id=..."` with `X-API-KEY` header, calls `prepareAsync`,
              and updates the global `MediaSession` metadata (title, artist, album art via
              `getMusicPicture`).

- **Player screen** (`PlayerScreen.kt`)
    - Detailed view for a single `Music` item, pushed from `SoundScreen`.
    - Fetches and displays:
        - Cover image for the selected track via `ApiService.getMusicPicture(music_id)`.
        - **Reminders associated with this track** via
          `ApiService.getReminderList(music_id = music.music_id)`.
    - Manages playback controls over the shared `Player.player`:
        - Tracks `playerProgress` and updates it in a `while (true)` loop inside a `LaunchedEffect`,
          polling once per second while playing.
        - Slider scrubs playback using `player.seekTo`.
        - Play/pause toggle directly calls `player.start()` / `player.pause()` and keeps a local
          `isPlaying` flag.
    - Renders a list of `Reminder` cards with toggles:
        - Each toggle updates `reminder.enabled` locally and then calls
          `ApiService.patchReminder(reminder)` in a coroutine.
        - The result from the API is used to update the `checked` state to the server’s confirmed
          value.
    - Back navigation uses the `PageViewModel.pop()` method.

- **Reminder list screen** (`ui/theme/ReminderScreen.kt`)
    - Fetches **all reminders** via `ApiService.getReminderList()` and **all music** via
      `getMusicList(ApiService.sortOrder.ascending)` on first composition.
    - Shows each `Reminder` with its time and the associated `Music` title (
      `musicData.find { it.music_id == reminder.music_id }`).
    - Each reminder has a `Switch` that, when toggled, calls `ApiService.patchReminder(reminder)` to
      persist changes.

### Networking and models

- **`ApiService`** (`ApiService.kt`)
    - Thin wrapper over `HttpURLConnection` with `suspend` functions using `Dispatchers.IO` and
      manual JSON parsing via `org.json`.
    - All requests set the `X-API-KEY` header to the provided `apiKey`.
    - Methods:
        - `getMusicList(sort: sortOrder, filter_term: String = "")` → `MutableList<Music>`
            - Calls `GET $endpoint/music/list?sort_order={ascending|descending}&filter_term=...`.
            - Parses each item into a `Music` data class.
        - `getReminderList(music_id: Int = -1)` → `MutableList<Reminder>`
            - Calls `GET $endpoint/reminders/list` or `GET $endpoint/reminders/list?music_id=...`.
            - Parses each item into a `Reminder` data class.
        - `patchReminder(reminder: Reminder)` → `Reminder`
            - Calls `PATCH $endpoint/reminders/{reminder_id}` with JSON body
              `{ hour, minute, enabled }`.
            - Parses the updated `Reminder` from the response.
        - `getMusicPicture(music_id: Int)` → `Bitmap`
            - Calls `GET $endpoint/music/picture?music_id=...` expecting `image/jpeg`.
    - **Model types**:
        - `Music(music_id: Int, title: String, date: String, author: String)`.
        - `Reminder(reminder_id: Int, hour: Int, minute: Int, music_id: Int, var enabled: Boolean)`.

### Audio playback

- **`Player` singleton** (`PlayerService.kt`)
    - Global object holding:
        - A single `MediaPlayer` instance configured with `CONTENT_TYPE_MUSIC`.
        - A `MediaSession` created in `init(context)`.
    - `MediaPlayer` is configured with `setOnPreparedListener { it.start() }`, so playback begins
      automatically after `prepareAsync()` is called from `SoundScreen`.
    - The rest of the app (both `SoundScreen` and `PlayerScreen`) controls playback via this shared
      player rather than per-screen instances.

## Backend prototype architecture (`api/`)

- **`api/app.py`**
    - Flask application using `flask_restful.Api` and two resources:
        - `MusicList` (`GET /music/list`)
            - Loads `music_lists` from `music.json` at import time.
            - Supports `sort_order` (`ascending` or `descending`) and `filter_term` query
              parameters.
            - Returns a JSON list of matching music records.
        - `MusicAudio` (`GET /music/audio`)
            - Accepts `music_id` query parameter and checks for existence in `music_lists`.
            - On invalid `music_id`, returns `404` with JSON `{"detail":"Invalid music_id"}`.
            - On valid `music_id`, the current implementation does **not** return audio data yet (it
              only verifies the ID), so this endpoint is effectively incomplete.
    - `@app.before_request` enforces a simple API key check:
        - If `X-API-KEY` header is missing → `401 {"detail":"Not authenticated"}`.
        - If header is present but not in `api_keys` → `401 {"detail":"Invalid API key"}`.
    - Runs on `port=8000` with `debug=True` when executed as `__main__`.

- **`api/music.json`**
    - Static list of music metadata objects with fields `music_id`, `title`, `date`, and `author`.

### Android ↔ API contract notes

- The Android app expects additional endpoints that are **not currently implemented** in
  `api/app.py`:
    - `GET /reminders/list` and `PATCH /reminders/{id}` for reminder management.
    - `GET /music/picture?music_id=...` returning `image/jpeg` album art.
- The `SoundScreen` and `PlayerScreen` assume that `GET /music/audio?music_id=...` returns an audio
  stream consumable by `MediaPlayer`.
- When extending the backend, align it with the URL patterns and payloads used by `ApiService` to
  avoid breaking the mobile app.

## Common commands

### Android app (Gradle / Kotlin / Compose)

Run these from the repository root (`/Users/.../AmbientSoundExplorer`):

- **Build debug APK for the app module**
  ```bash
  ./gradlew :app:assembleDebug
  ```

- **Run all unit tests for the debug variant**
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

- **Run a single unit test (example)**
  ```bash
  ./gradlew :app:testDebugUnitTest --tests "com.example.ambientsoundexplorer.ExampleUnitTest"
  ```

- **Run instrumentation tests on a connected device/emulator**
  ```bash
  ./gradlew :app:connectedAndroidTest
  ```

- **Run lint for the app module**
  ```bash
  ./gradlew :app:lint
  ```

### Python API (`api/`)

From the repo root:

- **Install Python dependencies (once per environment)**
  ```bash
  pip install flask flask-restful
  ```

- **Run the API locally on port 8000**
  ```bash
  cd api
  python app.py
  ```

When running the backend locally and using the Android emulator, you will typically need to adjust
the `ApiService` base URL in `MainActivity.kt` (inside `MainScreen`) to point to your machine (for
example, `http://10.0.2.2:8000` for an emulator) and ensure the `X-API-KEY` you send matches one of
the keys in `api/app.py`.