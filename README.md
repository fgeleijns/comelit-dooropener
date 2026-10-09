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

## Updates

The app checks this file for update metadata:

`https://raw.githubusercontent.com/fgeleijns/comelit-dooropener/main/version.json`

For each release, upload the signed APK to GitHub Releases and update `version.json` on the main branch.
Upload the PDF user guide to the same GitHub Release so users can download the APK and manual from one place.

Example `version.json`:

```json
{
  "versionCode": 44,
  "versionName": "0.1.43",
  "apkUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/download/v0.1.43/Comelit-dooropener-0.1.43-release.apk",
  "guideUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/download/v0.1.43/Comelit-dooropener-0.1.43-user-guide.pdf",
  "releaseUrl": "https://github.com/fgeleijns/comelit-dooropener/releases/tag/v0.1.43",
  "notes": "Release notes"
}
```

## Release key

Keep the release keystore private. Do not upload `release/`, `.jks`, `.keystore` or `keystore.properties` to GitHub.
