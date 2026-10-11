# Comelit Dooropener

Android app for controlling a Simplebus2 Bridge through Home Assistant.

## Features

- Door open control through Home Assistant.
- Doorbell notifications with selectable ringtone and repeat count.
- One-time, timed and permanent auto-open modes.
- Doorbell history with action status.
- Dutch, English and German interface.
- Internal/external Home Assistant URL fallback.
- Optional bridge proxy settings for remote bridge configuration.
- Manual and daily GitHub update checks.

## Configuration

The APK does not include private Home Assistant URLs, tokens or proxy settings. Configure these on the phone under Settings:

- Internal Home Assistant URL
- External Home Assistant URL
- Long-lived Home Assistant access token
- Bridge web/API URL
- Optional bridge proxy URL and token

## Current release

- Version: `0.1.119`
- Version code: `120`
- APK: `Comelit-dooropener-0.1.119-release.apk`
- User guide: [`Comelit-dooropener-0.1.119-user-guide.pdf`](docs/Comelit-dooropener-0.1.119-user-guide.pdf)

The release APK does not contain Frank's private Home Assistant URLs, access tokens, bridge proxy tokens or local network settings. These are configured on the phone after installation.

## Updates

The app checks this file for update metadata:

`https://raw.githubusercontent.com/fgeleijns/comelit-dooropener/main/version.json`

For each release, upload the signed APK to GitHub Releases and update `version.json` on the main branch.
Upload the PDF user guide to the same GitHub Release so users can download the APK and manual from one place.

Example `version.json`:

```json
{
  "versionCode": 120,
  "versionName": "0.1.119",
  "apkUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/download/v0.1.119/Comelit-dooropener-0.1.119-release.apk",
  "guideUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/download/v0.1.119/Comelit-dooropener-0.1.119-user-guide.pdf",
  "releaseUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/tag/v0.1.119",
  "notes": "Adds a polished information button in the app header with GitHub and user guide links. Includes a trilingual English/Dutch/German user guide with the default administrator PIN and safer first-start instructions."
}
```

## Release key

Keep the release keystore private. Do not upload `release/`, `.jks`, `.keystore` or `keystore.properties` to GitHub.
