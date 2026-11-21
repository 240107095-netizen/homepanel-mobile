# homepanel-mobile

An Android admin panel for a home media server, styled like a terminal UI
(TUI) on a true-black AMOLED background.

The server runs **Jellyfin** (the media library), **qBittorrent** (downloads),
**Jellyseerr** (requests for new titles) and **Radarr/Sonarr** (they find and
download whatever was requested). Managing all of them from a phone normally
means opening five different web UIs. This app puts the everyday tasks in one
place: see what is in the library, follow downloads live, find and request a new
movie, and check that the server is healthy.

It is the mobile version of `homepanel`, a terminal (Textual) panel for the same
server.

## Group members

| Name | Role |
| --- | --- |
| Yernazar | Solo project: architecture, UI, navigation, networking, localization |

## Main features

- **Library.** All movies and series from Jellyfin with watch progress
  (`✓ watched`, `▶ 54%`, `▶ 12/25`). You can search by Russian or original title
  and filter with the *All · Movies · Series* tabs. Pull down to refresh.
- **Title details.** Poster, original title, year, rating, an expandable
  description and a season list. A season opens its episodes with watched marks
  and runtime.
- **Downloads.** The live qBittorrent queue, refreshed every 2 seconds while the
  screen is visible. Each item shows a progress bar, speed, ETA and a pause/resume
  button. Changes appear at once; if the server refuses, the change is undone and
  a message explains why.
- **Find new.** Searches TMDB through Jellyseerr with posters and status
  (`✓ in library`, `… requested`). *Request* asks for confirmation and explains
  what will happen; after that the server downloads the title by itself.
- **Server.** Disk usage, online/offline status and version of every service, and
  a button to rescan the Jellyfin library.
- **Three languages: English (default), Russian and Kazakh.** You can switch
  inside the app. Search results also come back in the selected language.
- **Demo mode.** Without server keys the app runs on built-in sample data, so it
  works for anyone who clones the repository.
- **Every screen has a loading, empty and error state.** Errors are written in
  plain words ("No connection to the server…") and come with a *Retry* button.

## Architecture (MVVM)

```
UI (Compose screens)  ──events──▶  ViewModel  ──suspend calls──▶  MediaRepository
        ▲                              │                              │
        └────── StateFlow<UiState> ────┘           FakeMediaRepository / NetworkMediaRepository
                                                                      │
                                         Retrofit APIs: Jellyfin · qBittorrent · Jellyseerr · Radarr
```

- **Model / data layer.** `data/model` holds plain data classes (`Title`,
  `Season`, `Torrent`…). `MediaRepository` is the only interface the UI layer
  knows about. It has two implementations: `FakeMediaRepository` (demo data) and
  `NetworkMediaRepository` (the real server). Network DTOs live in
  `data/network/dto` and are mapped to the models there.
- **ViewModel.** One per screen. It exposes a single `StateFlow` of UI state
  built from `UiState` (Loading / Error / Success). It runs coroutines in
  `viewModelScope` and gets the repository through a `viewModelFactory` from
  `AppContainer` (manual dependency injection in `HomepanelApplication`).
- **View.** Each screen is split into a stateful `XxxScreen`, which collects
  state with `collectAsStateWithLifecycle`, and a stateless `XxxContent`, which
  only draws what it is given.
- **Reusable composables** live in `ui/components`: `TopStatusBar`, `TuiFrame`,
  `TuiButton`, `TuiTabs`, `TuiSearchField`, `TuiProgress`, `StatusBadge`,
  `Poster`, `TitleRow`, `Loading/Empty/ErrorState` and `LanguageSwitcher`.
- **Theme.** All colours are defined in `ui/theme` (Material 3 `darkColorScheme`
  plus status colours), and the whole app uses one monospace typography.

## Navigation

Navigation Compose with string routes, defined in `ui/navigation/Routes.kt`.

| Route | Screen | Argument |
| --- | --- | --- |
| `library` | Library (tab) | — |
| `title/{itemId}` | Title details | `itemId`: Jellyfin item id |
| `season/{seriesId}/{seasonId}` | Season episodes | two ids |
| `downloads` | Downloads (tab) | — |
| `search` | Find new (tab) | — |
| `server` | Server (tab) | — |

- Arguments are declared with `navArgument(...)`. Each ViewModel reads them from
  `SavedStateHandle` (`createSavedStateHandle()` in its factory).
- **Back navigation:** the `←` arrow in the top bar calls
  `navController.navigateUp()`, and the system back gesture works too.
- The bottom bar is shown only on the four tab screens. Switching tabs uses
  `popUpTo(start) { saveState = true }`, `launchSingleTop` and `restoreState`, so
  every tab keeps its own state.

## Tech stack

Kotlin 2.1, Jetpack Compose (BOM 2024.11), Material 3, Navigation Compose 2.8,
Lifecycle ViewModel / StateFlow, Kotlin coroutines, Retrofit 2.11 + OkHttp 4.12 +
kotlinx.serialization, Coil 2.7 (posters with a disk cache), AppCompat
per-app language. Built with AGP 8.7.3 and Gradle 8.11.1; minSdk 24,
targetSdk 35.

## How to run

1. Open the project in Android Studio and press **Run**. Without keys, the app
   starts in demo mode.
2. To connect to a real server, add the following to `local.properties`. This
   file is in `.gitignore` and never committed.

   ```properties
   jellyfin.url=http://192.168.1.250:8096
   jellyfin.key=...
   qbit.url=http://192.168.1.250:8085
   qbit.user=...
   qbit.password=...
   seerr.url=http://192.168.1.250:5055
   seerr.key=...
   radarr.url=http://192.168.1.250:7878
   radarr.key=...
   sonarr.url=http://192.168.1.250:8989
   sonarr.key=...
   prowlarr.url=http://192.168.1.250:9696
   prowlarr.key=...
   mediadl.url=http://192.168.1.250:8099
   ```

   The values become `BuildConfig` fields at build time. Plain HTTP is allowed
   only for the server's address (`res/xml/network_security_config.xml`).

## Adding a language

Create `app/src/main/res/values-xx/strings.xml` with the translated strings.
That is all: no code changes are needed. The language list in system settings
is generated from these folders (`generateLocaleConfig`). The only extra step
is adding the language to the in-app switcher in `LanguageSwitcher.kt`.
