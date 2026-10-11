from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    Table,
    TableStyle,
    PageBreak,
)


OUTPUT = r"C:\Users\fgel\Documents\Codex\2026-06-07\ik-heb-deze-translator-op-bijgevoegde\outputs\Comelit-dooropener-0.1.43-user-guide.pdf"
VERSION = "0.1.43"


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
    return [Spacer(1, 7 * mm), Paragraph(title, styles["H2"]), Spacer(1, 2 * mm)]


def build():
    doc = SimpleDocTemplate(
        OUTPUT,
        pagesize=A4,
        rightMargin=18 * mm,
        leftMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=18 * mm,
        title=f"Comelit Dooropener {VERSION} User Guide",
        author="Frank Geleijns",
    )

    base = getSampleStyleSheet()
    styles = {
        "Title": ParagraphStyle(
            "Title",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=25,
            leading=30,
            textColor=colors.HexColor("#0f172a"),
            spaceAfter=8,
        ),
        "Subtitle": ParagraphStyle(
            "Subtitle",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=11,
            leading=16,
            textColor=colors.HexColor("#475569"),
            spaceAfter=14,
        ),
        "H2": ParagraphStyle(
            "H2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=15,
            leading=19,
            textColor=colors.HexColor("#0f766e"),
            spaceBefore=4,
            spaceAfter=4,
        ),
        "Body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=9.8,
            leading=14,
            textColor=colors.HexColor("#1e293b"),
            spaceAfter=4,
        ),
        "Small": ParagraphStyle(
            "Small",
            parent=base["BodyText"],
            fontName="Helvetica",
            fontSize=8.7,
            leading=12,
            textColor=colors.HexColor("#475569"),
        ),
        "Warn": ParagraphStyle(
            "Warn",
            parent=base["BodyText"],
            fontName="Helvetica-Bold",
            fontSize=9.7,
            leading=14,
            textColor=colors.HexColor("#991b1b"),
            backColor=colors.HexColor("#fee2e2"),
            borderPadding=6,
            spaceAfter=6,
        ),
    }

    story = []
    story.append(p("Comelit Dooropener", styles["Title"]))
    story.append(p(f"Android app user guide - version {VERSION}", styles["Subtitle"]))
    story.append(p(
        "This app controls a Simplebus2 Bridge through Home Assistant. It can show doorbell events, "
        "send an open-door command, play a configurable ringtone, keep a small doorbell history and "
        "optionally enable automatic opening modes.",
        styles["Body"],
    ))
    story.append(p(
        "This document is generic. It does not contain private Home Assistant URLs, tokens, WiFi passwords "
        "or bridge proxy credentials.",
        styles["Warn"],
    ))

    story += section("Requirements", styles)
    for item in [
        "Android phone or tablet.",
        "Home Assistant with the Simplebus2 Bridge exposed through MQTT entities.",
        "A long-lived Home Assistant access token.",
        "Optional: a Home Assistant Cloud/Nabu Casa external URL for access outside the local network.",
        "Optional: a secure bridge proxy if the bridge web/API must be reached from outside the local network.",
    ]:
        story.append(bullet(item, styles))

    story += section("Installation", styles)
    for item in [
        "Install the APK on the Android device.",
        "Allow notifications when Android asks for permission. Without this, doorbell alerts may not be visible.",
        "Open the app and go to Settings before using the controls.",
        "If Android blocks the APK, allow installation from the source you used to open the file.",
    ]:
        story.append(bullet(item, styles))

    story += section("Main Settings", styles)
    settings = [
        ["Setting", "Purpose"],
        ["App name", "Name shown at the top of the app and in the persistent notification."],
        ["Language", "Dutch, English or German."],
        ["Show floor bell", "Shows or hides the floor/apartment bell sensor on the control screen."],
        ["Doorbell ringtone", "Selects the Android ringtone or audio file used for doorbell alerts."],
        ["Ringtone repeat count", "Number of times the ringtone is played for one doorbell event."],
    ]
    table = Table(settings, colWidths=[52 * mm, 116 * mm])
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.7),
        ("LEADING", (0, 0), (-1, -1), 11),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(table)

    story += section("Home Assistant Connection", styles)
    for item in [
        "Internal URL: local Home Assistant address, for example http://homeassistant.local:8123.",
        "External URL: Home Assistant Cloud/Nabu Casa URL or another secure HTTPS address.",
        "Connection mode: Automatic, Internal or External. Automatic tries the most suitable address and can fall back.",
        "Long-lived access token: created in Home Assistant under the user profile. Keep it private.",
    ]:
        story.append(bullet(item, styles))

    story += section("Bridge Web/API Settings", styles)
    story.append(p(
        "The bridge web/API URL is only needed for direct bridge configuration pages such as WiFi, MQTT, gain, threshold "
        "and address settings. If the phone can reach the bridge directly, the proxy fields can stay empty.",
        styles["Body"],
    ))
    for item in [
        "Bridge web/API URL: local bridge address, for example http://simplebus2-bridge.local.",
        "Bridge proxy URL: optional secure remote proxy endpoint.",
        "Bridge proxy token: optional token required by your own proxy.",
    ]:
        story.append(bullet(item, styles))

    story.append(PageBreak())
    story += section("Controls", styles)
    controls = [
        ["Control", "What it does"],
        ["Open door", "Sends the Home Assistant open-door command."],
        ["Auto-open once", "Opens the door automatically on the next valid ring, then disables itself."],
        ["Auto-open timed", "Opens the door automatically while the chosen timer is active, then disables itself."],
        ["Auto-open always", "Keeps automatic opening enabled until manually disabled. Use with care."],
        ["Disable auto-open", "Turns off all app-controlled automatic opening modes."],
        ["Doorbell notifications", "Enables or disables the Android doorbell alert behavior."],
    ]
    ctable = Table(controls, colWidths=[52 * mm, 116 * mm])
    ctable.setStyle(table._cellStyles and TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTNAME", (0, 1), (-1, -1), "Helvetica"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.7),
        ("LEADING", (0, 0), (-1, -1), 11),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))
    story.append(ctable)

    story += section("Important Safety Notes", styles)
    story.append(p(
        "Automatic opening is powerful and should be used deliberately. Auto-open once is the safest mode for situations "
        "where somebody must be able to enter after ringing once, for example emergency assistance. Timed and permanent "
        "modes should only be enabled when you understand the access risk.",
        styles["Warn"],
    ))
    for item in [
        "The app should send only one open command per ring event.",
        "The app keeps a short cooldown to avoid repeating open commands too quickly.",
        "If the phone has no Home Assistant connection, auto-open cannot work.",
        "If the bridge or Home Assistant is offline, open-door commands will fail.",
    ]:
        story.append(bullet(item, styles))

    story += section("Doorbell History", styles)
    story.append(p(
        "The app keeps a local history of recent doorbell activity. Entries show whether somebody rang, whether the door "
        "was opened manually, automatically, or not opened, and whether an automatic open attempt failed. This history is "
        "stored only on the phone.",
        styles["Body"],
    ))

    story += section("Updates Through GitHub", styles)
    story.append(p(
        "The app can check a public version metadata file on GitHub. When a newer version is available, it shows an update "
        "dialog with a link to the APK or release page.",
        styles["Body"],
    ))
    for item in [
        "Upload the signed APK to GitHub Releases.",
        "Keep version.json in the repository main branch.",
        "Increase versionCode for every update.",
        "Do not upload private keystores, tokens, passwords or personal configuration files.",
    ]:
        story.append(bullet(item, styles))

    story += section("Troubleshooting", styles)
    for item in [
        "No ringtone: check Android notification permission, notification volume, selected ringtone and app notification settings.",
        "No connection: check Home Assistant URL, token, WiFi/mobile connection and Home Assistant availability.",
        "External access works but bridge settings do not: configure a secure bridge proxy or use the local network.",
        "Auto-open did not work: check the history entry, Home Assistant connection and whether the selected mode was active.",
        "Bridge config unavailable: verify the bridge web/API address or proxy settings.",
    ]:
        story.append(bullet(item, styles))

    story += section("Files for a GitHub Release", styles)
    story.append(p("Recommended release assets:", styles["Body"]))
    for item in [
        "Comelit-dooropener-0.1.43-release.apk",
        "Comelit-dooropener-0.1.43-user-guide.pdf",
        "version.json in the repository root",
    ]:
        story.append(bullet(item, styles))

    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
