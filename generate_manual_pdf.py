from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

OUTPUT = r"C:\Users\fgel\Documents\Codex\2026-06-07\ik-heb-deze-translator-op-bijgevoegde\outputs\Comelit-dooropener-0.1.118-user-guide.pdf"
VERSION = "0.1.118"


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#64748b"))
    canvas.drawString(18 * mm, 12 * mm, f"Comelit Dooropener {VERSION}")
    canvas.drawRightString(192 * mm, 12 * mm, f"Page {doc.page}")
    canvas.restoreState()


def p(text, style):
    return Paragraph(text, style)


def bullet(text, styles):
    return Paragraph(f"- {text}", styles["Body"])


def section(title, styles):
    return [Spacer(1, 6 * mm), Paragraph(title, styles["H2"]), Spacer(1, 2 * mm)]


def make_table(rows, widths=None):
    table = Table(rows, colWidths=widths or [50 * mm, 118 * mm], repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.4),
        ("LEADING", (0, 0), (-1, -1), 10.5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def build():
    doc = SimpleDocTemplate(
        OUTPUT,
        pagesize=A4,
        rightMargin=17 * mm,
        leftMargin=17 * mm,
        topMargin=17 * mm,
        bottomMargin=17 * mm,
        title=f"Comelit Dooropener {VERSION} User Guide",
        author="Frank Geleijns",
    )

    base = getSampleStyleSheet()
    styles = {
        "Title": ParagraphStyle("Title", parent=base["Title"], fontName="Helvetica-Bold", fontSize=24, leading=29, textColor=colors.HexColor("#0f172a"), spaceAfter=7),
        "Subtitle": ParagraphStyle("Subtitle", parent=base["BodyText"], fontName="Helvetica", fontSize=10.8, leading=15, textColor=colors.HexColor("#475569"), spaceAfter=12),
        "H2": ParagraphStyle("H2", parent=base["Heading2"], fontName="Helvetica-Bold", fontSize=14.2, leading=18, textColor=colors.HexColor("#0f766e"), spaceBefore=3, spaceAfter=3),
        "Body": ParagraphStyle("Body", parent=base["BodyText"], fontName="Helvetica", fontSize=9.4, leading=13.2, textColor=colors.HexColor("#1e293b"), spaceAfter=3.5),
        "Warn": ParagraphStyle("Warn", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=9.4, leading=13.2, textColor=colors.HexColor("#991b1b"), backColor=colors.HexColor("#fee2e2"), borderPadding=6, spaceAfter=6),
    }

    story = []
    story.append(p("Comelit Dooropener", styles["Title"]))
    story.append(p(f"Android app user guide - version {VERSION}", styles["Subtitle"]))
    story.append(p(
        "Comelit Dooropener is an Android app for controlling a Simplebus2 Bridge through Home Assistant. "
        "It provides entrance door opening, optional parking garage control, doorbell notifications, automatic opening modes, "
        "bridge configuration, bus monitor information and update checks through GitHub.",
        styles["Body"],
    ))
    story.append(p(
        "This guide is generic. It does not contain private Home Assistant URLs, tokens, WiFi passwords, "
        "bridge proxy tokens or personal network settings.",
        styles["Warn"],
    ))

    story += section("Requirements", styles)
    for item in [
        "Android phone or tablet with notification permission enabled.",
        "Home Assistant with the Simplebus2 Bridge exposed through MQTT entities.",
        "A long-lived Home Assistant access token.",
        "Optional: Home Assistant Cloud/Nabu Casa or another secure HTTPS external URL.",
        "Optional: a secure bridge proxy if the Simplebus2 Bridge web/API must be reached from outside the local network.",
        "Optional: a Home Assistant switch entity for extra controls, for example a parking garage door.",
    ]:
        story.append(bullet(item, styles))

    story += section("Installation", styles)
    for item in [
        "Download the APK from the GitHub release page.",
        "Install the APK on the Android device.",
        "Allow installation from the selected source if Android asks for permission.",
        "Allow notifications when Android asks. Without this, doorbell alerts may not appear correctly.",
        "Open the app, go to Settings, and configure Home Assistant before using the controls.",
    ]:
        story.append(bullet(item, styles))

    story += section("Main Screen", styles)
    story.append(make_table([
        ["Area", "Description"],
        ["Header", "Shows the app name, connection state and compact status line for entrance ring, bridge state and auto-open state."],
        ["Entry door open", "Large primary button for opening the main entrance door."],
        ["Extra control", "Optional section for additional Home Assistant controls, such as a parking garage door."],
        ["Auto-open", "Lets the administrator choose one-time, timed, permanent or disabled automatic opening."],
        ["Doorbell notifications", "Starts or stops the persistent monitor service for doorbell alerts."],
        ["Doorbell history", "Opens the complete local history of rings and actions."],
        ["Footer", "Shows author and app version."],
    ]))

    story += section("User Mode and Admin Mode", styles)
    story.append(p(
        "The app can run in a simple user mode or in administrator mode. User mode is intended for normal residents and "
        "shows only the safe daily controls, such as opening the entrance door and optional parking garage. Administrator "
        "mode gives access to settings, monitoring, bridge configuration, tokens and advanced options.",
        styles["Body"],
    ))
    story.append(make_table([
        ["Mode", "Visible controls"],
        ["User mode", "Entrance door open and optional parking garage. Advanced controls and settings are hidden."],
        ["Admin mode", "All controls, settings, monitoring, bridge configuration and update options."],
        ["PIN protection", "Settings and administrator-only screens can be protected with a PIN of up to 8 digits."],
    ]))

    story += section("Home Assistant Settings", styles)
    story.append(make_table([
        ["Setting", "Purpose"],
        ["Internal URL", "Local Home Assistant address, for example http://homeassistant.local:8123."],
        ["External URL", "Home Assistant Cloud/Nabu Casa URL or another secure HTTPS address for use outside the home network."],
        ["Connection mode", "Automatic, Internal or External. Automatic switches between configured addresses."],
        ["Long-lived access token", "Created in Home Assistant under the user profile. Keep it private."],
        ["App name", "Name shown in the app header and notifications."],
        ["Language", "Dutch, English or German."],
        ["Admin/User mode", "Chooses whether the app exposes administrator controls or only the simplified user interface."],
    ]))

    story += section("Doorbell Notifications", styles)
    story.append(make_table([
        ["Setting", "Purpose"],
        ["Doorbell ringtone", "Selects an Android ringtone or audio file for the doorbell alert."],
        ["Ringtone repeat count", "Number of times the ringtone is played for one doorbell event."],
        ["Notification duration", "How long the doorbell notification stays available for quick action."],
        ["Open from notification", "The notification can open the app or send an open-door command, depending on the action chosen."],
        ["Persistent monitor", "The app uses a foreground monitor service so doorbell alerts can work while the app is not in the foreground."],
    ]))

    story += section("Doorbell History", styles)
    story.append(p(
        "The main screen shows a compact history preview. The full history screen shows recent doorbell events and actions, "
        "including rings, manual open commands, automatic open attempts, failed automatic opens and parking garage events. "
        "The history is stored locally on the phone.",
        styles["Body"],
    ))

    story.append(PageBreak())
    story += section("Automatic Opening Modes", styles)
    story.append(p(
        "Automatic opening is powerful and should be used deliberately. One-time mode is designed for situations where "
        "somebody must be able to enter after the next ring, for example emergency assistance. Timed and permanent modes "
        "should only be used when the access risk is understood.",
        styles["Warn"],
    ))
    story.append(make_table([
        ["Mode", "Behavior"],
        ["Off", "No automatic door opening."],
        ["One-time", "The next valid ring opens the door once, then auto-open turns itself off."],
        ["Timed", "Rings open the door until the selected time expires. The app shows the active timed state."],
        ["Permanent", "Rings keep opening the door until manually disabled. This is the highest-risk mode."],
        ["Repeat count", "Auto-open can repeat the open command up to a limited number of attempts for reliability."],
    ]))

    story += section("Parking Garage Control", styles)
    story.append(p(
        "An optional parking garage button can be shown on the main screen. It calls a configured Home Assistant entity, "
        "for example a Tasmota switch entity. This feature is optional and can be hidden for users who do not have a garage.",
        styles["Body"],
    ))
    story.append(make_table([
        ["Setting", "Purpose"],
        ["Show parking garage", "Shows or hides the parking garage button."],
        ["Garage entity ID", "Home Assistant entity to trigger, for example switch.tasmota_2."],
        ["Action behavior", "The app sends the service call and records the action in local history."],
        ["Notification sound", "The app can use a notification sound for garage actions separately from the main doorbell ringtone."],
    ]))

    story += section("Bridge Web/API Settings", styles)
    story.append(p(
        "Bridge Web/API settings are for configuring the ESP32 Simplebus2 Bridge itself. These settings are separate from "
        "Home Assistant. If the phone can reach the bridge directly on the local network, the proxy fields can stay empty.",
        styles["Body"],
    ))
    story.append(make_table([
        ["Setting", "Purpose"],
        ["Bridge web/API URL", "Local bridge address, for example http://simplebus2-bridge.local or the bridge IP address."],
        ["Bridge proxy URL", "Optional secure remote proxy endpoint for reaching the bridge from outside the local network."],
        ["Bridge proxy token", "Optional token used by your own bridge proxy."],
        ["WiFi SSID/password", "Bridge WiFi configuration fields."],
        ["MQTT host/user/password/prefix", "MQTT connection settings used by the bridge."],
        ["Intercom address", "Simplebus address used by the bridge."],
        ["Gain and threshold", "Receiver tuning values for the Simplebus signal."],
        ["Ring-to-open minutes", "Bridge firmware setting for its own ring-to-open behavior."],
    ]))

    story += section("Bridge Actions", styles)
    story.append(make_table([
        ["Action", "Description"],
        ["Save WiFi", "Sends WiFi settings to the bridge."],
        ["Save MQTT", "Sends MQTT settings to the bridge."],
        ["Save bus settings", "Saves address, gain, threshold and ring-to-open values."],
        ["Start config mode", "Starts the bridge configuration portal if supported by the firmware."],
        ["Learn address", "Attempts to start or assist address learning if supported by the firmware."],
        ["Factory reset", "Resets bridge settings. Use only when you really intend to reconfigure the bridge."],
    ]))

    story += section("Bus Monitor", styles)
    story.append(p(
        "The bus monitor reads the debug/status information exposed by the Simplebus2 Bridge API. It is not a complete "
        "packet analyzer, but it helps show whether the bridge sees bus activity and whether decoding errors occur.",
        styles["Body"],
    ))
    story.append(make_table([
        ["Field", "Meaning"],
        ["Last bus activity", "Timestamp of the most recent bus code/address combination remembered by the app."],
        ["Last bus code/address", "Latest decoded code and address reported or remembered."],
        ["Previous bus activities", "The app remembers the previous two bus activities for comparison."],
        ["Decode OK", "Counter for successfully decoded telegrams reported by the bridge."],
        ["Checksum errors", "Counter for checksum failures reported by the bridge."],
        ["Address mismatch", "Counter for telegrams not matching the configured address, if reported by the bridge."],
        ["ISR count", "Low-level interrupt/debug count, useful for seeing if the receiver still sees signal edges."],
    ]))

    story.append(PageBreak())
    story += section("Security Notes", styles)
    for item in [
        "Keep the Home Assistant long-lived token private.",
        "Use HTTPS for external Home Assistant access, preferably Home Assistant Cloud/Nabu Casa.",
        "Do not publish personal URLs, tokens, WiFi passwords or bridge proxy tokens.",
        "Use user mode for residents who only need the entrance and garage controls.",
        "Protect administrator screens with a PIN.",
        "Use permanent auto-open only when you fully understand the access risk.",
    ]:
        story.append(bullet(item, styles))

    story += section("Updates Through GitHub", styles)
    story.append(p(
        "The app checks a public version metadata file on GitHub. When a newer version is available, it can show an update "
        "dialog that points to the release page or APK download.",
        styles["Body"],
    ))
    story.append(make_table([
        ["File", "Purpose"],
        ["version.json", "Public metadata file containing versionCode, versionName, APK URL, guide URL and release notes."],
        ["Signed APK", "The installable Android package uploaded to the GitHub Release."],
        ["User guide PDF", "This document, uploaded to the same GitHub Release."],
        ["README.md", "Repository overview and current release links."],
    ]))

    story += section("Release Files", styles)
    for item in [
        "Comelit-dooropener-0.1.118-release.apk",
        "Comelit-dooropener-0.1.118-user-guide.pdf",
        "version.json in the repository root",
    ]:
        story.append(bullet(item, styles))

    story += section("Troubleshooting", styles)
    for item in [
        "No ringtone: check Android notification permission, notification volume, selected ringtone and app notification settings.",
        "Notification disappears too quickly: increase the notification duration setting.",
        "No Home Assistant connection: check internal/external URL, token, WiFi/mobile data and Home Assistant availability.",
        "External Home Assistant works but bridge config does not: configure a secure bridge proxy or use the local network.",
        "Auto-open failed: check the history entry, Home Assistant connection and whether the selected mode was active.",
        "Garage button does nothing: verify the configured Home Assistant entity ID and whether the entity can be controlled from Home Assistant.",
        "Bridge config unavailable: verify bridge web/API URL, proxy URL, proxy token and bridge firmware support.",
        "Bus monitor shows no new data: verify the bridge is connected, powered correctly and exposing status through the API.",
    ]:
        story.append(bullet(item, styles))

    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
