# Info Watch Face

A Wear OS watch face for Galaxy Watch Ultra displaying real-time data from Claude AI, Minecraft, and Rust game servers — with battery, date, and time.

![Watch Face Preview](docs/preview.png)

## Features

- **Claude AI Usage Bars** — Session (S) and Weekly (W) usage meters pulled live from the Claude API
- **Minecraft Player Count** — Live query to your Java server via the Server List Ping (SLP) protocol
- **Rust Player Count** — Live query via Steam A2S protocol with challenge-response support
- **Battery** — Native WFF `BATTERY_PERCENT` data source with animated green fill bar
- **Time Format Toggle** — Switch between 12h (AM/PM) and 24h via the watch face customization screen
- **Date** — Month and day display
- **Ambient Mode** — All elements hidden in ambient for battery efficiency

## Architecture

Three Android modules:

| Module | Package | Target | Purpose |
|--------|---------|--------|---------|
| `watchface` | `com.damon1974.infowatchface.face` | Watch | WFF XML watch face |
| `wear` | `com.damon1974.infowatchface` | Watch | Complication services + Data Layer receiver |
| `phone` | `com.damon1974.infowatchface` | Phone | Claude/MC/Rust polling + Data Layer sender |

> **Note:** `phone` and `wear` share the same `applicationId` — this is required for the Wearable Data Layer API to work.

## Data Flow

```
Claude API ──┐
MC Server  ──┼──► Phone App (PollWorker) ──► Data Layer ──► DataLayerListenerService
Rust Server ─┘                                               │
                                                             ├──► SessionComplicationService
                                                             ├──► WeeklyComplicationService
                                                             ├──► McComplicationService
                                                             └──► RustComplicationService
                                                                        │
                                                               watchface.xml (WFF)
```

## Setup

### Prerequisites

- Android Studio
- Galaxy Watch Ultra (or Wear OS 4+ device)
- Samsung Galaxy phone paired via Galaxy Wearable app
- Claude.ai account (Pro or Team)

### Phone App

1. Deploy the `phone` module to your phone
2. Open the app and tap **Login** — log in to claude.ai in the WebView
3. Tap **Refresh Now** to push data to the watch

### Watch

1. Deploy the `wear` module to the watch
2. Deploy the `watchface` module to the watch
3. Select **Info Watch Face** from the watch face picker
4. The S/W Claude bars and Minecraft/Rust counts auto-bind — no manual complication assignment needed

### Configuration

Edit these constants in the phone module to point to your servers:

| File | Constant | Default |
|------|----------|---------|
| `PollWorker.kt` | Minecraft host | `mcj.thehackpig.com:25565` |
| `PollWorker.kt` | Rust host/port | `rust.thehackpig.com:28017` |

## Protocols

- **Claude API** — HTTPS to `claude.ai/api` using session cookie from WebView login
- **Minecraft** — TCP Server List Ping (SLP) protocol on port 25565
- **Rust** — UDP Steam A2S_INFO with Valve challenge-response on query port

## Development Notes

- `git push` via WSL hangs silently — push from Windows terminal
- Watch ADB reconnect: run `.\connect-watch.ps1`
- After changing `applicationId`, uninstall both packages before redeploying
- WFF `BATTERY_PERCENT` is a native integer data source (0-100), no complication needed
- Samsung Wear OS only serves `SHORT_TEXT` for `WATCH_BATTERY` system provider, not `RANGED_VALUE`

## License

MIT
