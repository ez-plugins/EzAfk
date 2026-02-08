# Simple Voice Chat Integration

EzAfk supports integration with the Simple Voice Chat plugin to play custom sounds when players go AFK.

## Features
- Plays a configurable MP3 sound to the player when they go AFK.
- Plays a configurable MP3 sound to the player when they go AFK, and optionally when they return from AFK.
- Optional integration: EzAfk works with or without Simple Voice Chat installed.
- Auto-detection: Enable, disable, or auto-detect integration via config.

## Setup Instructions
1. **Install Simple Voice Chat**
   - Download and install the Simple Voice Chat plugin on your server.
   - Ensure players have the Simple Voice Chat mod installed on their clients.
2. **Configure EzAfk**
   - In `config.yml`, set:
     ```yaml
     afk:
       sound:
         enabled: true
         file: "mp3/ezafk-sound.mp3" # Path relative to plugins/EzAfk/
    unafk:
      sound:
        enabled: true
        file: "mp3/ezafk-sound.mp3"
     integration:
       voicechat: auto # Options: true, false, auto
     ```
   - Place your MP3 file in `plugins/EzAfk/mp3/` or use the default provided.
3. **Permissions**
   - No special permissions are required for sound playback.

## Troubleshooting
- Ensure Simple Voice Chat is installed and enabled on both server and client.
- The MP3 file must be valid and compatible (48kHz recommended).
- Check server logs for any errors related to sound playback or decoding.
- If no sound plays, verify you are connected to voice chat and the config is correct.

## Advanced
- You can replace the default MP3 with your own sound by overwriting the file in `plugins/EzAfk/mp3/`.
- Use `integration.voicechat: true` to force integration, or `false` to disable.

## Links
- [Simple Voice Chat Plugin](https://modrepo.de/minecraft/voicechat)
- [EzAfk Documentation](../README.md)
