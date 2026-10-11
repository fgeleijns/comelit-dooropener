from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak

OUTPUT = r"C:\Users\fgel\Documents\Codex\2026-06-07\ik-heb-deze-translator-op-bijgevoegde\outputs\Comelit-dooropener-0.1.119-user-guide.pdf"
VERSION = "0.1.119"
DEFAULT_ADMIN_PIN = "8882"


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
    return [Spacer(1, 5 * mm), Paragraph(title, styles["H2"]), Spacer(1, 1.5 * mm)]


def make_table(rows, styles, widths=None):
    wrapped_rows = []
    for row_index, row in enumerate(rows):
        style = styles["TableHeader"] if row_index == 0 else styles["TableCell"]
        wrapped_rows.append([Paragraph(str(cell), style) for cell in row])

    table = Table(wrapped_rows, colWidths=widths or [46 * mm, 118 * mm], repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#0f766e")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.25, colors.HexColor("#cbd5e1")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#f8fafc")]),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return table


def common_release_files(styles):
    return make_table([
        ["File", "Purpose"],
        ["Comelit-dooropener-0.1.119-release.apk", "Installable Android package."],
        ["Comelit-dooropener-0.1.119-user-guide.pdf", "This trilingual user guide."],
        ["version.json", "Public update metadata with version, APK link, guide link and release notes."],
    ], styles, widths=[72 * mm, 92 * mm])


def english(story, styles):
    story.append(p("Comelit Dooropener", styles["Title"]))
    story.append(p(f"Trilingual user guide - version {VERSION}", styles["Subtitle"]))
    story.append(p(
        "Comelit Dooropener is an Android app for controlling a Simplebus2 Bridge through Home Assistant. "
        "It can open the entrance door, control an optional parking garage switch, show doorbell notifications, "
        "use automatic opening modes, read bridge status and open project information through the header info button.",
        styles["Body"],
    ))
    story.append(p(
        "This guide is generic. It does not contain private Home Assistant URLs, tokens, WiFi passwords or bridge proxy tokens.",
        styles["Warn"],
    ))

    story += section("First Start", styles)
    story.append(make_table([
        ["Item", "Description"],
        ["Default administrator PIN", f"{DEFAULT_ADMIN_PIN}. Use this PIN to unlock Settings and administrator-only screens."],
        ["Important", "Change the administrator PIN immediately after installation."],
        ["If the PIN is forgotten", "Reset or reinstall the app and configure it again."],
    ], styles))

    story += section("Requirements", styles)
    for item in [
        "Android phone or tablet with notification permission enabled.",
        "Home Assistant with the Simplebus2 Bridge exposed through MQTT entities.",
        "A long-lived Home Assistant access token.",
        "Optional: Home Assistant Cloud/Nabu Casa or another secure HTTPS external URL.",
        "Optional: a secure bridge proxy for reaching the bridge web/API from outside the local network.",
        "Optional: a Home Assistant switch entity for extra controls, for example a parking garage door.",
    ]:
        story.append(bullet(item, styles))

    story += section("Main Screen", styles)
    story.append(make_table([
        ["Area", "Description"],
        ["Header", "Shows the app name, connection state and compact status for entrance, bridge and auto-open."],
        ["Info button", "Opens app information, GitHub and this user guide."],
        ["Entry door open", "Large primary button for opening the main entrance door."],
        ["Extra control", "Optional button for a parking garage or another Home Assistant switch."],
        ["Auto-open", "Administrator control for one-time, timed, permanent or disabled automatic opening."],
        ["Doorbell notifications", "Starts or stops the persistent monitor service."],
        ["Doorbell history", "Shows recent rings and actions stored locally on the phone."],
    ], styles))

    story += section("Settings", styles)
    story.append(make_table([
        ["Setting", "Purpose"],
        ["Internal URL", "Local Home Assistant address, for example http://homeassistant.local:8123."],
        ["External URL", "Home Assistant Cloud/Nabu Casa URL or another secure HTTPS address."],
        ["Connection mode", "Automatic, Internal or External. Automatic switches between configured addresses."],
        ["Long-lived access token", "Created in Home Assistant under the user profile. Keep it private."],
        ["App name", "Name shown in the header and notifications."],
        ["Language", "Dutch, English or German."],
        ["Admin/User mode", "Admin shows all controls. User mode only shows safe daily controls."],
        ["Administrator PIN", f"Default is {DEFAULT_ADMIN_PIN}. Change it after installation."],
    ], styles))

    story += section("Automatic Opening", styles)
    story.append(p("Automatic opening is powerful. Use it deliberately, especially permanent mode.", styles["Warn"]))
    story.append(make_table([
        ["Mode", "Behavior"],
        ["Off", "No automatic opening."],
        ["One-time", "The next valid ring opens the door once, then turns itself off."],
        ["Timed", "Rings open the door until the selected time expires."],
        ["Permanent", "Rings keep opening the door until manually disabled."],
        ["Repeat count", "The app can repeat the open command for reliability."],
    ], styles))

    story += section("Bridge, Bus Monitor and Updates", styles)
    story.append(make_table([
        ["Function", "Description"],
        ["Bridge Web/API", "Configure bridge URL, optional proxy, WiFi, MQTT, address, gain and threshold."],
        ["Bus monitor", "Shows latest bus activity, previous activities and bridge debug counters. It is not a full packet analyzer."],
        ["GitHub updates", "The app reads version.json and can point to the latest release, APK and guide."],
        ["Security", "Keep tokens and proxy keys private. Use HTTPS for external access."],
    ], styles))

    story += section("Release Files", styles)
    story.append(common_release_files(styles))


def dutch(story, styles):
    story.append(PageBreak())
    story.append(p("Comelit Dooropener", styles["Title"]))
    story.append(p(f"Nederlandse handleiding - versie {VERSION}", styles["Subtitle"]))
    story.append(p(
        "Comelit Dooropener is een Android-app voor de Simplebus2 Bridge via Home Assistant. "
        "De app kan de entreedeur openen, optioneel een parkeergarage bedienen, belmeldingen tonen, "
        "automatisch openen gebruiken, bridge-status uitlezen en via het info-icoon projectinformatie openen.",
        styles["Body"],
    ))
    story.append(p(
        "Deze handleiding bevat geen privegegevens zoals Home Assistant URLs, tokens, WiFi-wachtwoorden of bridge-proxy tokens.",
        styles["Warn"],
    ))

    story += section("Eerste start", styles)
    story.append(make_table([
        ["Onderdeel", "Uitleg"],
        ["Standaard beheerders-PIN", f"{DEFAULT_ADMIN_PIN}. Gebruik deze PIN om Instellingen en beheerschermen te openen."],
        ["Belangrijk", "Wijzig de beheerders-PIN direct na installatie."],
        ["PIN vergeten", "Reset of installeer de app opnieuw en configureer de app opnieuw."],
    ], styles))

    story += section("Benodigdheden", styles)
    for item in [
        "Android-telefoon of tablet met toestemming voor meldingen.",
        "Home Assistant met de Simplebus2 Bridge via MQTT-entiteiten.",
        "Een long-lived access token van Home Assistant.",
        "Optioneel: Home Assistant Cloud/Nabu Casa of een andere veilige externe HTTPS URL.",
        "Optioneel: een veilige bridge-proxy om de bridge Web/API buiten het lokale netwerk te bereiken.",
        "Optioneel: een Home Assistant switch-entiteit voor extra bediening, bijvoorbeeld een parkeergarage.",
    ]:
        story.append(bullet(item, styles))

    story += section("Hoofdscherm", styles)
    story.append(make_table([
        ["Gebied", "Beschrijving"],
        ["Kop", "Toont appnaam, verbinding en compacte status voor hoofdingang, bridge en auto-open."],
        ["Info-knop", "Opent appinformatie, GitHub en deze handleiding."],
        ["Entree deur openen", "Grote primaire knop om de hoofdingang te openen."],
        ["Extra bediening", "Optionele knop voor parkeergarage of een andere Home Assistant switch."],
        ["Automatisch openen", "Beheerfunctie voor eenmalig, tijdelijk, permanent of uitgeschakeld automatisch openen."],
        ["Belmeldingen", "Start of stopt de permanente monitorservice."],
        ["Belgeschiedenis", "Toont recente belmomenten en acties die lokaal op de telefoon worden opgeslagen."],
    ], styles))

    story += section("Instellingen", styles)
    story.append(make_table([
        ["Instelling", "Doel"],
        ["Interne URL", "Lokaal Home Assistant-adres, bijvoorbeeld http://homeassistant.local:8123."],
        ["Externe URL", "Home Assistant Cloud/Nabu Casa URL of een andere veilige HTTPS URL."],
        ["Verbindingsmodus", "Automatisch, Intern of Extern. Automatisch wisselt tussen de ingestelde adressen."],
        ["Long-lived access token", "Aangemaakt in Home Assistant onder het gebruikersprofiel. Houd dit prive."],
        ["Appnaam", "Naam die in de kop en meldingen wordt getoond."],
        ["Taal", "Nederlands, Engels of Duits."],
        ["Beheer/Gebruiker", "Beheer toont alles. Gebruikersmodus toont alleen veilige dagelijkse bediening."],
        ["Beheerders-PIN", f"Standaard is {DEFAULT_ADMIN_PIN}. Wijzig deze na installatie."],
    ], styles))

    story += section("Automatisch openen", styles)
    story.append(p("Automatisch openen is krachtig. Gebruik dit bewust, vooral de permanente stand.", styles["Warn"]))
    story.append(make_table([
        ["Stand", "Werking"],
        ["Uit", "Geen automatisch openen."],
        ["Eenmalig", "De volgende geldige bel opent de deur een keer en schakelt daarna uit."],
        ["Tijdelijk", "Bellen opent de deur tot de ingestelde tijd voorbij is."],
        ["Permanent", "Bellen blijft de deur openen totdat dit handmatig wordt uitgezet."],
        ["Herhalen", "De app kan het open-commando herhalen voor betrouwbaarheid."],
    ], styles))

    story += section("Bridge, busmonitor en updates", styles)
    story.append(make_table([
        ["Functie", "Beschrijving"],
        ["Bridge Web/API", "Configureert bridge URL, optionele proxy, WiFi, MQTT, adres, gain en threshold."],
        ["Busmonitor", "Toont laatste busactiviteit, vorige activiteiten en debugtellers van de bridge. Dit is geen volledige packet-analyzer."],
        ["GitHub updates", "De app leest version.json en kan verwijzen naar de laatste release, APK en handleiding."],
        ["Beveiliging", "Houd tokens en proxy-sleutels prive. Gebruik HTTPS voor externe toegang."],
    ], styles))

    story += section("Releasebestanden", styles)
    story.append(common_release_files(styles))


def german(story, styles):
    story.append(PageBreak())
    story.append(p("Comelit Dooropener", styles["Title"]))
    story.append(p(f"Deutsche Anleitung - Version {VERSION}", styles["Subtitle"]))
    story.append(p(
        "Comelit Dooropener ist eine Android-App fuer die Simplebus2 Bridge ueber Home Assistant. "
        "Die App kann die Eingangstuer oeffnen, optional ein Garagentor steuern, Klingelmeldungen anzeigen, "
        "Auto-Oeffnen verwenden, Bridge-Status lesen und ueber das Info-Symbol Projektinformationen oeffnen.",
        styles["Body"],
    ))
    story.append(p(
        "Diese Anleitung enthaelt keine privaten Home Assistant URLs, Tokens, WLAN-Passwoerter oder Bridge-Proxy-Tokens.",
        styles["Warn"],
    ))

    story += section("Erster Start", styles)
    story.append(make_table([
        ["Punkt", "Beschreibung"],
        ["Standard-Administrator-PIN", f"{DEFAULT_ADMIN_PIN}. Mit dieser PIN werden Einstellungen und Administratorseiten freigeschaltet."],
        ["Wichtig", "Aendern Sie die Administrator-PIN direkt nach der Installation."],
        ["PIN vergessen", "App zuruecksetzen oder neu installieren und erneut konfigurieren."],
    ], styles))

    story += section("Voraussetzungen", styles)
    for item in [
        "Android-Telefon oder Tablet mit aktivierter Benachrichtigungsberechtigung.",
        "Home Assistant mit Simplebus2 Bridge ueber MQTT-Entitaeten.",
        "Ein Long-lived Access Token von Home Assistant.",
        "Optional: Home Assistant Cloud/Nabu Casa oder eine andere sichere externe HTTPS URL.",
        "Optional: ein sicherer Bridge-Proxy fuer Zugriff auf die Bridge Web/API ausserhalb des lokalen Netzwerks.",
        "Optional: eine Home Assistant Switch-Entitaet fuer Zusatzfunktionen, zum Beispiel Parkgarage.",
    ]:
        story.append(bullet(item, styles))

    story += section("Hauptbildschirm", styles)
    story.append(make_table([
        ["Bereich", "Beschreibung"],
        ["Kopfbereich", "Zeigt Appname, Verbindung und kompakten Status fuer Eingang, Bridge und Auto-Oeffnen."],
        ["Info-Taste", "Oeffnet Appinformationen, GitHub und diese Anleitung."],
        ["Eingangstuer oeffnen", "Grosse Haupttaste zum Oeffnen der Eingangstuer."],
        ["Zusatzbedienung", "Optionale Taste fuer Parkgarage oder einen anderen Home Assistant Switch."],
        ["Auto-Oeffnen", "Administratorfunktion fuer einmalig, zeitgesteuert, dauerhaft oder ausgeschaltet."],
        ["Klingelmeldungen", "Startet oder stoppt den permanenten Monitorservice."],
        ["Klingelverlauf", "Zeigt letzte Klingelereignisse und Aktionen, lokal auf dem Telefon gespeichert."],
    ], styles))

    story += section("Einstellungen", styles)
    story.append(make_table([
        ["Einstellung", "Zweck"],
        ["Interne URL", "Lokale Home Assistant Adresse, zum Beispiel http://homeassistant.local:8123."],
        ["Externe URL", "Home Assistant Cloud/Nabu Casa URL oder eine andere sichere HTTPS URL."],
        ["Verbindungsmodus", "Automatisch, Intern oder Extern. Automatisch wechselt zwischen den konfigurierten Adressen."],
        ["Long-lived access token", "In Home Assistant im Benutzerprofil erstellt. Privat halten."],
        ["Appname", "Name im Kopfbereich und in Benachrichtigungen."],
        ["Sprache", "Niederlaendisch, Englisch oder Deutsch."],
        ["Admin/Benutzer", "Admin zeigt alle Funktionen. Benutzermodus zeigt nur sichere Alltagsbedienung."],
        ["Administrator-PIN", f"Standard ist {DEFAULT_ADMIN_PIN}. Nach Installation aendern."],
    ], styles))

    story += section("Auto-Oeffnen", styles)
    story.append(p("Auto-Oeffnen ist eine starke Funktion. Bitte bewusst verwenden, besonders den dauerhaften Modus.", styles["Warn"]))
    story.append(make_table([
        ["Modus", "Verhalten"],
        ["Aus", "Kein automatisches Oeffnen."],
        ["Einmalig", "Das naechste gueltige Klingeln oeffnet einmal und schaltet danach aus."],
        ["Zeitgesteuert", "Klingeln oeffnet bis die eingestellte Zeit abgelaufen ist."],
        ["Dauerhaft", "Klingeln oeffnet weiter, bis die Funktion manuell ausgeschaltet wird."],
        ["Wiederholen", "Die App kann den Oeffnen-Befehl zur Zuverlaessigkeit wiederholen."],
    ], styles))

    story += section("Bridge, Busmonitor und Updates", styles)
    story.append(make_table([
        ["Funktion", "Beschreibung"],
        ["Bridge Web/API", "Konfiguriert Bridge URL, optionalen Proxy, WLAN, MQTT, Adresse, Gain und Threshold."],
        ["Busmonitor", "Zeigt letzte Busaktivitaet, vorherige Aktivitaeten und Debugzaehler der Bridge. Kein vollstaendiger Packet Analyzer."],
        ["GitHub Updates", "Die App liest version.json und kann zur letzten Release, APK und Anleitung verweisen."],
        ["Sicherheit", "Tokens und Proxy-Schluessel privat halten. Fuer externen Zugriff HTTPS verwenden."],
    ], styles))

    story += section("Release-Dateien", styles)
    story.append(common_release_files(styles))


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
        "Title": ParagraphStyle("Title", parent=base["Title"], fontName="Helvetica-Bold", fontSize=23, leading=28, textColor=colors.HexColor("#0f172a"), spaceAfter=6),
        "Subtitle": ParagraphStyle("Subtitle", parent=base["BodyText"], fontName="Helvetica", fontSize=10.5, leading=14.5, textColor=colors.HexColor("#475569"), spaceAfter=10),
        "H2": ParagraphStyle("H2", parent=base["Heading2"], fontName="Helvetica-Bold", fontSize=13.8, leading=17, textColor=colors.HexColor("#0f766e"), spaceBefore=2, spaceAfter=2),
        "Body": ParagraphStyle("Body", parent=base["BodyText"], fontName="Helvetica", fontSize=9.0, leading=12.7, textColor=colors.HexColor("#1e293b"), spaceAfter=3.2),
        "Warn": ParagraphStyle("Warn", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=9.0, leading=12.7, textColor=colors.HexColor("#991b1b"), backColor=colors.HexColor("#fee2e2"), borderPadding=6, spaceAfter=6),
        "TableHeader": ParagraphStyle("TableHeader", parent=base["BodyText"], fontName="Helvetica-Bold", fontSize=7.8, leading=9.8, textColor=colors.white),
        "TableCell": ParagraphStyle("TableCell", parent=base["BodyText"], fontName="Helvetica", fontSize=7.8, leading=9.8, textColor=colors.HexColor("#0f172a")),
    }

    story = []
    english(story, styles)
    dutch(story, styles)
    german(story, styles)
    doc.build(story, onFirstPage=footer, onLaterPages=footer)


if __name__ == "__main__":
    build()
