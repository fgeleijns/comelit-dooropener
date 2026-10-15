# Comelit Dooropener

Android app for controlling a Simplebus2 Bridge through Home Assistant.

## Required bridge and compatibility

This app is designed specifically for **Thomas (Elektroarzt)'s SimpleBus2 MQTT Bridge**: [Elektroarzt/simplebus2-mqtt-bridge](https://github.com/Elektroarzt/simplebus2-mqtt-bridge). It requires that bridge, an MQTT broker, and Home Assistant configured with the bridge's entities. It is not a standalone Comelit bus interface or a universal app for other intercom bridges.

Bridge hardware and original firmware are provided by Thomas' project; they are not included with this Android app. Follow that project's documentation for compatible intercom installations and bridge setup. This app is a separate companion project and is not an official Comelit app.

The latest reception diagnostics were tested with a locally modified `2.0.0-rxdiag.5` firmware derived from Thomas' bridge firmware, using a configurable decoder guard. The app falls back to the original status fields when extended diagnostic fields are unavailable; reliable reception still depends on the bridge firmware, installation and gain/threshold settings. The modified firmware is not bundled with this app release.

## Features

- Door open control through Home Assistant.
- Doorbell notifications with selectable ringtone and repeat count.
- One-time, timed and permanent auto-open modes.
- Doorbell history with action status.
- Dutch, English and German interface.
- Internal/external Home Assistant URL fallback.
- Optional bridge proxy settings for remote bridge configuration.
- Admin/User mode with PIN protection for settings and bridge screens.
- Optional parking garage/extra Home Assistant switch control.
- Bus monitor with latest bridge activity and recent activity history.
- Manual and daily GitHub update checks.

## Configuration

The APK does not include private Home Assistant URLs, tokens or proxy settings. Configure these on the phone under Settings:

- Internal Home Assistant URL
- External Home Assistant URL
- Long-lived Home Assistant access token
- Bridge web/API URL
- Optional bridge proxy URL and token
- Optional parking garage switch entity
- Administrator PIN

## Current release

- Version: `0.2.1`
- Version code: `204`
- APK: [`Comelit-dooropener-0.2.1-release.apk`](https://github.com/fgeleijns/comelit-dooropener/releases/download/v0.2.1/Comelit-dooropener-0.2.1-release.apk)
- User guide: [`Comelit-dooropener-0.2.0-user-guide.pdf`](docs/Comelit-dooropener-0.2.0-user-guide.pdf)

The release APK does not contain Frank's private Home Assistant URLs, access tokens, bridge proxy tokens or local network settings. These are configured on the phone after installation.

## Updates

The app checks this file for update metadata:

`https://raw.githubusercontent.com/fgeleijns/comelit-dooropener/main/version.json`

For each release, upload the signed APK to GitHub Releases and update `version.json` on the main branch.
Upload the PDF user guide to the same GitHub Release so users can download the APK and manual from one place.

## Changes in 0.2.1

- Continuous user-enabled doorbell monitoring uses Android's `specialUse` foreground service type instead of time-limited `dataSync`. A persistent notification remains visible, and monitoring can be stopped in the app.
- Settings includes **Clear doorbell history**, with confirmation. This clears history and the ring counter while preserving configuration and active monitoring.
- Every foreground-service start is acknowledged, including notification refreshes, fixing the history-clear crash.
- Known bus codes have descriptive names. Other addresses remain visible; code 63/address 255 is hidden from the ordinary monitoring display without assigning an unconfirmed meaning. Zero values after a bridge restart are not presented as received activity.
- Diagnostic firmware exposing `dbg_last_message_*` retains the latest useful activity even after 63/255 traffic. Older firmware uses the existing status fields.
- Doorbell polling runs once per second without overlapping requests. Auto-open dispatch uses a separate executor and a shorter delay.
- Internal Android build code is 204; intermediate local builds were not published as separate GitHub releases.

Validation: signed release build and release lint passed. On an Android 16 phone, history was cleared twice with 15-second waits; foreground monitoring remained active and connection, PIN and monitoring settings were preserved. Overnight endurance still requires observation. The existing 0.2.0 user guide remains available; the changes above supplement it.

## Release key

Keep the release keystore private. Do not upload `release/`, `.jks`, `.keystore` or `keystore.properties` to GitHub.
