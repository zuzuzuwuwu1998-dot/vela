# Vela — Media Harvester & Clipper for Android

Vela is a modern Material 3 Android media downloader powered by `yt-dlp`, `FFmpeg`, and `Aria2c`.

- **Web Preview (GitHub Pages)**: https://zuzuzuwuwu1998-dot.github.io/vela/
- **Repository**: https://github.com/zuzuzuwuwu1998-dot/vela

## Features
- **Smart Clipboard Auto-Detect & Share Target**: Detects links from YouTube, TikTok, Instagram, X/Twitter, SoundCloud, and Reddit.
- **4 Download Modes**:
  - **Video**: Best / 1080p / 720p / 480p (`MP4`, `MKV`, `WEBM`)
  - **Audio Studio**: `MP3 320k`, `M4A AAC`, `FLAC Hi-Fi`, `Podcast 64k (Mono)`, `Ringtone 30s`
  - **Segment Clipper**: Download only a specific timestamp range (`Start` → `End`) using `--download-sections` + FFmpeg
  - **Custom Command**: Run and save custom `yt-dlp` argument presets
- **Batch Multi-URL Queue**: Paste multiple URLs (one per line) and track live progress & ETA.
- **Media Vault**: Built-in library with search, category filters, and native playback/share sheet.

## Build Locally
```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
