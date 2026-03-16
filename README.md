# Info Watch Face

A Wear OS watch face for Galaxy Watch Ultra displaying Claude AI usage, server status, and battery — built with Watch Face Format (WFF) XML.

## Features

- Digital time display
- Date (month/day)
- Claude API usage bars — Session and Weekly
- Rust and Minecraft server online/offline status
- Battery indicator

## Architecture

- **Watch face**: WFF XML (`res/raw/watchface.xml`) — no Kotlin required
- **Companion app**: *(coming soon)* — polls Claude API, Minecraft, and Rust endpoints every 10 minutes and pushes data to the watch via Wearable Data Layer API

## Distribution

Distributed as a signed APK via GitHub Releases. Sideloading required (one-time "Install unknown apps" permission in Android settings).

## Requirements

- Wear OS 4+ (API 33)
- Galaxy Watch Ultra or compatible round Wear OS device
- Android phone for companion app *(coming soon)*

## Development

Built with Android Studio using Watch Face Format. No code — layout is entirely declarative XML.

## Status

Work in progress — static layout complete, companion app data integration pending.
