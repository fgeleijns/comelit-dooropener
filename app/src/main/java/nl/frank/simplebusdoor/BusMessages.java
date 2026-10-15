package nl.frank.simplebusdoor;

/** Names from the bridge's protocol constants; unknown codes stay unknown. */
final class BusMessages {
    private BusMessages() {}
    static boolean isVisible(int code, int address) {
        return code > 0 && code <= 63 && address >= 0 && address <= 255
                && !(code == 63 && address == 255);
    }
    static String name(int code, String language) {
        String nl, en, de;
        switch (code) {
            case 8: nl="Oproep hoofdcentrale"; en="Call to main switchboard"; de="Ruf zur Hauptzentrale"; break;
            case 9: nl="Oproep conciërge"; en="Call caretaker"; de="Ruf zum Hausmeister"; break;
            case 16: nl="Deur openen"; en="Open door"; de="Tür öffnen"; break;
            case 17: nl="Hoorn opgenomen"; en="Handset lifted"; de="Hörer abgenommen"; break;
            case 18: nl="Hoorn teruggelegd"; en="Handset replaced"; de="Hörer aufgelegt"; break;
            case 19: nl="Oproep tweede centrale"; en="Call secondary switchboard"; de="Ruf zur Nebenzentrale"; break;
            case 20: nl="Camera inschakelen"; en="Camera on"; de="Kamera einschalten"; break;
            case 21: nl="Etagebel"; en="Floor doorbell"; de="Etagenklingel"; break;
            case 24: nl="Intercomoproep"; en="Intercom call"; de="Internruf"; break;
            case 26: nl="Antwoord intercom"; en="Intercom response"; de="Internruf-Antwort"; break;
            case 29: nl="Algemene actuator"; en="Generic actuator"; de="Allgemeiner Aktor"; break;
            case 30: nl="Prioriteitsoproep hoofdcentrale"; en="Priority call to main switchboard"; de="Prioritätsruf zur Hauptzentrale"; break;
            case 32: nl="Oproep van centrale 1"; en="Call from switchboard 1"; de="Ruf von Zentrale 1"; break;
            case 37: nl="Oproep van centrale 2"; en="Call from switchboard 2"; de="Ruf von Zentrale 2"; break;
            case 42: nl="Oproep van centrale 3"; en="Call from switchboard 3"; de="Ruf von Zentrale 3"; break;
            case 43: nl="Oproep van centrale 4"; en="Call from switchboard 4"; de="Ruf von Zentrale 4"; break;
            case 45: nl="Oproep centrale met beeld"; en="Switchboard call with video"; de="Zentralenruf mit Bild"; break;
            case 48: nl="Oproep buitendeur"; en="Entrance door call"; de="Ruf von der Eingangstür"; break;
            case 49: nl="Scherm uitschakelen"; en="Screen off"; de="Bildschirm ausschalten"; break;
            case 50: nl="Oproep buitendeur met beeld"; en="Entrance door call with video"; de="Ruf von der Eingangstür mit Bild"; break;
            case 51: nl="Deuropenknop knipperen aan"; en="Door-open button blinking on"; de="Türöffnertaste blinkt"; break;
            case 52: nl="Deuropenknop knipperen uit"; en="Door-open button blinking off"; de="Blinken der Türöffnertaste aus"; break;
            default:
                if (code == 0) { nl="Nog geen busbericht"; en="No bus message yet"; de="Noch keine Busnachricht"; }
                else { nl="Onbekend commando"; en="Unknown command"; de="Unbekannter Befehl"; }
        }
        return "de".equals(language) ? de : "en".equals(language) ? en : nl;
    }
}
