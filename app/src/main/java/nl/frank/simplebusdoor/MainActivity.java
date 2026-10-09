package nl.frank.simplebusdoor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int REQUEST_RINGTONE = 70;
    private static final String UPDATE_INFO_URL = "https://raw.githubusercontent.com/fgeleijns/comelit-dooropener/main/version.json";
    private static final long UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L;
    private static final String AUTO_OPEN_OFF = "off";
    private static final String AUTO_OPEN_ONCE = "once";
    private static final String AUTO_OPEN_TIMED = "timed";
    private static final String AUTO_OPEN_ALWAYS = "always";
    private static final int BG = Color.rgb(12, 18, 32);
    private static final int PANEL = Color.rgb(23, 32, 51);
    private static final int PANEL_SOFT = Color.rgb(34, 48, 74);
    private static final int ACCENT = Color.rgb(22, 160, 133);
    private static final int TEXT = Color.rgb(248, 250, 252);
    private static final int MUTED = Color.rgb(182, 194, 209);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout root;
    private LinearLayout content;
    private Button controlTab;
    private Button settingsTab;
    private Button monitorTab;
    private Button bridgeTab;
    private Button languageNl;
    private Button languageEn;
    private Button languageDe;
    private Button connectionAuto;
    private Button connectionInternal;
    private Button connectionExternal;
    private Button showFloorDoorButton;
    private Button monitorButton;
    private Button ringButton;
    private boolean showApiSettings;
    private TextView appTitle;
    private TextView statusLine;
    private TextView entryStatus;
    private TextView floorStatus;
    private TextView heartbeatStatus;
    private TextView ringStatus;
    private TextView bridgeStatusLine;
    private TextView doorbellHistory;
    private EditText appNameInput;
    private EditText internalUrlInput;
    private EditText externalUrlInput;
    private EditText bridgeUrlInput;
    private EditText bridgeProxyUrlInput;
    private EditText bridgeProxyTokenInput;
    private EditText tokenInput;
    private EditText bridgeWifiSsidInput;
    private EditText bridgeWifiPasswordInput;
    private EditText bridgeMqttHostInput;
    private EditText bridgeMqttUserInput;
    private EditText bridgeMqttPasswordInput;
    private EditText bridgeMqttPrefixInput;
    private EditText bridgeAddressInput;
    private EditText bridgeGainInput;
    private EditText bridgeThresholdInput;
    private EditText bridgeRtoInput;
    private EditText doorbellRepeatInput;
    private String language = "nl";
    private String connectionMode = "auto";
    private boolean settingsOpen;
    private boolean monitorOpen;
    private boolean bridgeOpen;
    private boolean monitorRunning;
    private boolean showFloorDoor;

    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 4000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        language = prefs().getString("language", "nl");
        connectionMode = prefs().getString("connection_mode", "auto");
        migrateDefaultAppName();
        showFloorDoor = prefs().getBoolean("show_floor_door", false);
        monitorRunning = prefs().getBoolean("door_notifications_enabled", false);
        buildShell();
        showControls();
        checkForUpdates(false);
        requestNotifications();
        if (monitorRunning) startForegroundService(new Intent(this, DoorMonitorService.class));
        handler.post(refresher);
        clearRingNotification();
        if ("open_door".equals(getIntent().getAction())) openDoor();
        if ("dismiss_ring".equals(getIntent().getAction())) dismissRing();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        clearRingNotification();
        if ("open_door".equals(intent.getAction())) openDoor();
        if ("dismiss_ring".equals(intent.getAction())) dismissRing();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_RINGTONE || resultCode != RESULT_OK || data == null) return;
        Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (ringtone == null) return;
        prefs().edit()
                .putString("doorbell_ringtone_uri", ringtone.toString())
                .putString("doorbell_ringtone_name", readableRingtoneName(ringtone))
                .apply();
        toast(t("Beltoon opgeslagen", "Ringtone saved"));
        if (settingsOpen) showSettings();
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 26 + statusBarHeight(), 28, 34 + navigationBarHeight());
        scroll.addView(root);

        appTitle = title(appTitleText(), 26);
        root.addView(appTitle);
        TextView subtitle = body("Simplebus2 Bridge via Home Assistant");
        subtitle.setTextColor(MUTED);
        root.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, 22, 0, 8);
        controlTab = smallButton(t("Bediening", "Controls"));
        monitorTab = smallButton(t("Monitoring", "Monitor"));
        bridgeTab = smallButton("Bridge");
        settingsTab = smallButton(t("Instellingen", "Settings"));
        tabs.addView(controlTab, new LinearLayout.LayoutParams(0, 86, 1));
        tabs.addView(monitorTab, new LinearLayout.LayoutParams(0, 86, 1));
        tabs.addView(bridgeTab, new LinearLayout.LayoutParams(0, 86, 1));
        tabs.addView(settingsTab, new LinearLayout.LayoutParams(0, 86, 1));
        root.addView(tabs);
        controlTab.setOnClickListener(v -> showControls());
        monitorTab.setOnClickListener(v -> showMonitor());
        bridgeTab.setOnClickListener(v -> showBridgeSettings());
        settingsTab.setOnClickListener(v -> showSettings());

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content);
        setContentView(scroll);
    }

    private void showControls() {
        settingsOpen = false;
        monitorOpen = false;
        bridgeOpen = false;
        content.removeAllViews();
        styleTabs();

        statusLine = body(t("Verbinden...", "Connecting..."));
        statusLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(card(statusLine));

        content.addView(title(t("Status", "Status"), 23));
        entryStatus = statusCard(t("Bel hoofdingang", "Main entrance bell"));
        floorStatus = showFloorDoor ? statusCard(t("Bel etage/appartement", "Floor/apartment bell")) : null;
        heartbeatStatus = statusCard("Bridge");
        ringStatus = statusCard(t("Automatisch openen", "Auto-open"));

        content.addView(title(t("Bediening", "Controls"), 23));
        Button open = actionButton(t("Deur openen", "Open door"));
        open.setOnClickListener(v -> openDoor());
        content.addView(open);

        ringButton = secondaryButton(t("Automatisch openen laden...", "Loading auto-open..."));
        ringButton.setOnClickListener(v -> toggleRing());
        content.addView(ringButton);

        monitorButton = secondaryButton(monitorRunning ? t("Belmeldingen uitzetten", "Turn doorbell alerts off") : t("Belmeldingen aanzetten", "Turn doorbell alerts on"));
        monitorButton.setOnClickListener(v -> toggleMonitor());
        content.addView(monitorButton);

        content.addView(title(t("Belgeschiedenis", "Doorbell history"), 23));
        doorbellHistory = body(doorbellHistoryText());
        content.addView(card(doorbellHistory));

        refresh();
    }

    private void showSettings() {
        settingsOpen = true;
        monitorOpen = false;
        bridgeOpen = false;
        content.removeAllViews();
        styleTabs();

        content.addView(title(t("Instellingen", "Settings"), 23));

        content.addView(label(t("Appnaam", "App name")));
        appNameInput = input(t("Naam die bovenin de app en meldingen staat", "Name shown in the app and notifications"), false, false);
        appNameInput.setText(appName());
        content.addView(appNameInput);

        content.addView(label(t("Taal", "Language", "Sprache")));
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        languageNl = smallButton("Nederlands");
        languageEn = smallButton("English");
        languageDe = smallButton("Deutsch");
        langRow.addView(languageNl, new LinearLayout.LayoutParams(0, 86, 1));
        langRow.addView(languageEn, new LinearLayout.LayoutParams(0, 86, 1));
        langRow.addView(languageDe, new LinearLayout.LayoutParams(0, 86, 1));
        content.addView(langRow);
        languageNl.setOnClickListener(v -> setLanguage("nl"));
        languageEn.setOnClickListener(v -> setLanguage("en"));
        languageDe.setOnClickListener(v -> setLanguage("de"));
        styleLanguageButtons();

        content.addView(label(t("Deurbellen", "Doorbells")));
        showFloorDoorButton = secondaryButton(showFloorDoor ? t("Etagebel tonen: aan", "Show floor bell: on") : t("Etagebel tonen: uit", "Show floor bell: off"));
        showFloorDoorButton.setOnClickListener(v -> toggleShowFloorDoor());
        content.addView(showFloorDoorButton);

        Button ringtoneButton = secondaryButton(t("Beltoon kiezen: ", "Choose ringtone: ") + ringtoneName());
        ringtoneButton.setOnClickListener(v -> chooseDoorbellRingtone());
        content.addView(ringtoneButton);

        content.addView(label(t("Beltoon herhalen", "Repeat ringtone")));
        doorbellRepeatInput = input(t("Aantal keer afspelen, 1-10", "Number of plays, 1-10"), false, false);
        doorbellRepeatInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        doorbellRepeatInput.setText(String.valueOf(prefs().getInt("doorbell_repeat_count", 1)));
        content.addView(doorbellRepeatInput);

        Button api = secondaryButton(showApiSettings ? t("API / Web instellingen verbergen", "Hide API / Web settings") : t("API / Web instellingen", "API / Web settings"));
        api.setOnClickListener(v -> {
            showApiSettings = !showApiSettings;
            showSettings();
        });
        content.addView(api);

        if (showApiSettings) addApiSettingsFields();

        Button save = actionButton(t("Instellingen opslaan", "Save settings"));
        save.setOnClickListener(v -> saveSettings());
        content.addView(save);

        Button update = secondaryButton(t("Op updates controleren", "Check for updates"));
        update.setOnClickListener(v -> checkForUpdates(true));
        content.addView(update);

        TextView help = body(t(
                "Beveiliging: gebruik buitenshuis bij voorkeur je Home Assistant Cloud/Nabu Casa URL met HTTPS. Het token wordt lokaal op je telefoon opgeslagen. Deel dit token niet.\n\nAutomatisch openen betekent: als iemand beneden aanbelt, opent de deur automatisch binnen de ingestelde tijd. WiFi-configuratie start alleen het tijdelijke configuratieportaal van de bridge.",
                "Security: outside your home, prefer your Home Assistant Cloud/Nabu Casa HTTPS URL. The token is stored locally on your phone. Do not share it.\n\nAuto-open means: when someone rings downstairs, the door opens automatically during the configured time. WiFi setup only starts the bridge's temporary setup portal."
        ));
        help.setTextColor(MUTED);
        content.addView(card(help));
    }

    private void addApiSettingsFields() {
        content.addView(label(t("Verbinding", "Connection")));
        LinearLayout connectionRow = new LinearLayout(this);
        connectionRow.setOrientation(LinearLayout.VERTICAL);
        connectionAuto = smallButton(t("Automatisch", "Automatic"));
        connectionInternal = smallButton(t("Intern", "Internal"));
        connectionExternal = smallButton(t("Extern", "External"));
        connectionRow.addView(connectionAuto);
        connectionRow.addView(connectionInternal);
        connectionRow.addView(connectionExternal);
        content.addView(connectionRow);
        connectionAuto.setOnClickListener(v -> setConnectionMode("auto"));
        connectionInternal.setOnClickListener(v -> setConnectionMode("internal"));
        connectionExternal.setOnClickListener(v -> setConnectionMode("external"));
        styleConnectionButtons();

        content.addView(label(t("Interne Home Assistant URL", "Internal Home Assistant URL")));
        internalUrlInput = input("http://homeassistant.local:8123", false, false);
        internalUrlInput.setText(prefs().getString("ha_internal_url", prefs().getString("ha_url", "")));
        content.addView(internalUrlInput);

        content.addView(label(t("Externe Home Assistant URL", "External Home Assistant URL")));
        externalUrlInput = input(t("Bijv. je Nabu Casa URL", "Example: your Nabu Casa URL"), false, false);
        externalUrlInput.setText(prefs().getString("ha_external_url", ""));
        content.addView(externalUrlInput);

        content.addView(label(t("Bridge web/API URL", "Bridge web/API URL")));
        bridgeUrlInput = input("http://simplebus2-bridge.local", false, false);
        bridgeUrlInput.setText(prefs().getString("bridge_url", ""));
        content.addView(bridgeUrlInput);

        TextView proxyNotice = body(t(
                "Proxy is optioneel. Laat leeg als je telefoon rechtstreeks bij de bridge kan.",
                "Proxy is optional. Leave empty if your phone can reach the bridge directly."
        ));
        proxyNotice.setTextColor(MUTED);
        content.addView(card(proxyNotice));

        content.addView(label(t("Bridge proxy URL (optioneel, mag leeg blijven)", "Bridge proxy URL (optional, can stay empty)")));
        bridgeProxyUrlInput = input(t("Leeg laten tenzij je zelf een proxy gebruikt", "Leave empty unless you use your own proxy"), false, false);
        bridgeProxyUrlInput.setText(prefs().getString("bridge_proxy_url", ""));
        content.addView(bridgeProxyUrlInput);

        content.addView(label(t("Bridge proxy token (optioneel, mag leeg blijven)", "Bridge proxy token (optional, can stay empty)")));
        bridgeProxyTokenInput = input(t("Leeg laten tenzij je proxy een token gebruikt", "Leave empty unless your proxy uses a token"), false, true);
        bridgeProxyTokenInput.setText(prefs().getString("bridge_proxy_token", ""));
        content.addView(bridgeProxyTokenInput);

        TextView proxyHelp = body(t(
                "Laat dit leeg als je telefoon rechtstreeks bij de bridge kan. Vul dit alleen in als je zelf een beveiligde proxy gebruikt; de app praat dan met die proxy en de proxy praat lokaal met de bridge.",
                "Leave this empty if your phone can reach the bridge directly. Only fill this in when you use your own secure proxy; the app talks to that proxy, and the proxy talks locally to the bridge."
        ));
        proxyHelp.setTextColor(MUTED);
        content.addView(card(proxyHelp));

        content.addView(label(t("Long-lived access token", "Long-lived access token")));
        tokenInput = input(t("Plak hier je token uit Home Assistant", "Paste your Home Assistant token here"), true, true);
        tokenInput.setText(prefs().getString("ha_token", ""));
        content.addView(tokenInput);
    }

    private void showMonitor() {
        settingsOpen = false;
        monitorOpen = true;
        bridgeOpen = false;
        content.removeAllViews();
        styleTabs();
        content.addView(title(t("Bus-monitor", "Bus monitor"), 23));
        TextView intro = body(t(
                "Deze pagina leest de debugstatus rechtstreeks uit de Simplebus2 Bridge. Dit is geen volledig live buslog, maar laat wel laatste code/adres en fouttellers zien.",
                "This page reads debug status directly from the Simplebus2 Bridge. It is not a full live bus log, but it shows the last code/address and error counters."
        ));
        intro.setTextColor(MUTED);
        content.addView(card(intro));

        statusLine = body(t("Monitoring ophalen...", "Loading monitor..."));
        statusLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(card(statusLine));

        refreshMonitor();
    }

    private void showBridgeSettings() {
        settingsOpen = false;
        monitorOpen = false;
        bridgeOpen = true;
        content.removeAllViews();
        styleTabs();

        content.addView(title(t("Bridge instellingen", "Bridge settings"), 23));
        TextView intro = body(t(
                "Deze instellingen gaan rechtstreeks naar de ESP32 Simplebus2 Bridge. Pas WiFi en factory reset voorzichtig aan.",
                "These settings are sent directly to the ESP32 Simplebus2 Bridge. Be careful with WiFi and factory reset."
        ));
        intro.setTextColor(MUTED);
        content.addView(card(intro));

        bridgeStatusLine = body(t("Bridge-config ophalen...", "Loading bridge config..."));
        bridgeStatusLine.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(card(bridgeStatusLine));

        content.addView(label(t("WiFi", "WiFi")));
        bridgeWifiSsidInput = input("SSID", false, false);
        bridgeWifiPasswordInput = input(t("Nieuw WiFi-wachtwoord, leeg laten = niet wijzigen", "New WiFi password, leave empty = unchanged"), false, true);
        content.addView(fieldRow("SSID", bridgeWifiSsidInput));
        content.addView(fieldRow(t("WiFi-wachtwoord", "WiFi password"), bridgeWifiPasswordInput));
        Button saveWifi = secondaryButton(t("WiFi opslaan en opnieuw verbinden", "Save WiFi and reconnect"));
        saveWifi.setOnClickListener(v -> saveBridgeWifi());
        content.addView(saveWifi);

        Button config = secondaryButton(t("WiFi-configuratie starten (4 min)", "Start WiFi setup (4 min)"));
        config.setOnClickListener(v -> callButton(HaClient.CONFIG_MODE, t("Config mode verstuurd", "Config mode sent")));
        content.addView(config);

        content.addView(label("MQTT"));
        bridgeMqttHostInput = input("Broker host", false, false);
        bridgeMqttUserInput = input(t("Gebruiker", "User"), false, false);
        bridgeMqttPasswordInput = input(t("Nieuw MQTT-wachtwoord, leeg laten = niet wijzigen", "New MQTT password, leave empty = unchanged"), false, true);
        bridgeMqttPrefixInput = input("Topic prefix", false, false);
        content.addView(fieldRow(t("Broker host", "Broker host"), bridgeMqttHostInput));
        content.addView(fieldRow(t("Gebruiker", "User"), bridgeMqttUserInput));
        content.addView(fieldRow(t("Wachtwoord", "Password"), bridgeMqttPasswordInput));
        content.addView(fieldRow(t("Topic prefix", "Topic prefix"), bridgeMqttPrefixInput));
        Button saveMqtt = secondaryButton(t("MQTT opslaan", "Save MQTT"));
        saveMqtt.setOnClickListener(v -> saveBridgeMqtt());
        content.addView(saveMqtt);

        content.addView(label(t("Bus en automatisch openen", "Bus and auto-open")));
        bridgeAddressInput = input(t("Adres 0-255", "Address 0-255"), false, false);
        bridgeGainInput = input("Gain 2-40", false, false);
        bridgeThresholdInput = input("Threshold mV 100-1500", false, false);
        bridgeRtoInput = input(t("Automatisch openen minuten 1-1440", "Auto-open minutes 1-1440"), false, false);
        content.addView(fieldRow(t("Adres", "Address"), bridgeAddressInput));
        content.addView(fieldRow("Gain", bridgeGainInput));
        content.addView(fieldRow("Threshold mV", bridgeThresholdInput));
        content.addView(fieldRow(t("Auto-open min.", "Auto-open min."), bridgeRtoInput));
        Button saveDevice = secondaryButton(t("Businstellingen opslaan", "Save bus settings"));
        saveDevice.setOnClickListener(v -> saveBridgeDevice());
        content.addView(saveDevice);

        TextView learnHelp = body(t(
                "Adres leren volgens de maker: druk de fysieke knop op de bridge 3-4 seconden in tot de LED twee keer kort knippert en aan blijft. Druk daarna op de deurbelknop bij het appartement. De LED gaat uit zodra het adres geleerd is. De knoppen hieronder proberen dit via MQTT, maar dat werkt alleen als jouw firmware dat ondersteunt.",
                "Address teach-in according to the maker: press the physical bridge button for 3-4 seconds until the LED flashes twice and stays on. Then press the apartment doorbell button. The LED turns off once the address is learned. The buttons below try this via MQTT, but only work if your firmware supports it."
        ));
        learnHelp.setTextColor(MUTED);
        content.addView(card(learnHelp));

        Button teachStart = secondaryButton(t("MQTT leerstand proberen", "Try MQTT teach-in"));
        teachStart.setOnClickListener(v -> sendTeachIn(true));
        content.addView(teachStart);

        Button teachStop = secondaryButton(t("MQTT leerstand stoppen", "Stop MQTT teach-in"));
        teachStop.setOnClickListener(v -> sendTeachIn(false));
        content.addView(teachStop);

        Button copyLastAddress = secondaryButton(t("Laatst gezien adres overnemen", "Use last seen address"));
        copyLastAddress.setOnClickListener(v -> copyLastSeenAddress());
        content.addView(copyLastAddress);

        content.addView(label(t("Systeem", "System")));
        Button restart = secondaryButton(t("Bridge herstarten", "Restart bridge"));
        restart.setOnClickListener(v -> confirmAndPost("/api/restart", t("Bridge nu herstarten?", "Restart bridge now?")));
        content.addView(restart);
        Button reset = secondaryButton(t("Factory reset", "Factory reset"));
        reset.setOnClickListener(v -> confirmAndPost("/api/factory-reset", t("Factory reset wist alle bridge-instellingen. Zeker weten?", "Factory reset erases all bridge settings. Are you sure?")));
        content.addView(reset);

        loadBridgeConfig();
    }

    private LinearLayout fieldRow(String name, EditText field) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
        rowLp.setMargins(0, 10, 0, 0);
        row.setLayoutParams(rowLp);

        TextView nameView = body(name);
        nameView.setTextColor(MUTED);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(nameView, new LinearLayout.LayoutParams(0, -2, 0.42f));

        field.setMinHeight(88);
        row.addView(field, new LinearLayout.LayoutParams(0, -2, 0.58f));
        return row;
    }

    private TextView statusCard(String label) {
        TextView tv = body(label + "\n-");
        tv.setTextSize(18);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tv.setMinHeight(120);
        content.addView(card(tv));
        return tv;
    }

    private LinearLayout card(TextView child) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(24, 22, 24, 22);
        box.setBackground(round(PANEL, 22));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 16, 0, 0);
        box.setLayoutParams(lp);
        box.addView(child);
        return box;
    }

    private TextView title(String value, int size) {
        TextView tv = body(value);
        tv.setTextSize(size);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 10, 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView body(String value) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(16);
        tv.setTextColor(TEXT);
        tv.setLineSpacing(4, 1.0f);
        return tv;
    }

    private TextView label(String value) {
        TextView tv = body(value);
        tv.setTextColor(MUTED);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 20, 0, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText input(String hint, boolean multiline, boolean secret) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(Color.rgb(148, 163, 184));
        input.setTextColor(TEXT);
        input.setTextSize(15);
        input.setSingleLine(!multiline);
        input.setMinLines(multiline ? 3 : 1);
        input.setMaxLines(multiline ? 5 : 2);
        input.setInputType(secret ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setBackground(round(PANEL_SOFT, 18));
        input.setPadding(20, 16, 20, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 10, 0, 0);
        input.setLayoutParams(lp);
        return input;
    }

    private Button actionButton(String label) {
        Button button = baseButton(label);
        button.setBackground(round(ACCENT, 20));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = baseButton(label);
        button.setBackground(round(PANEL_SOFT, 20));
        return button;
    }

    private Button smallButton(String label) {
        Button button = baseButton(label);
        button.setTextSize(14);
        button.setMinHeight(78);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(6, 6, 6, 6);
        button.setLayoutParams(lp);
        return button;
    }

    private Button baseButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setSingleLine(false);
        button.setMinLines(1);
        button.setMaxLines(2);
        button.setPadding(18, 12, 18, 12);
        button.setBackground(round(PANEL_SOFT, 20));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 16, 0, 0);
        button.setLayoutParams(lp);
        button.setMinHeight(104);
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 48;
    }

    private int navigationBarHeight() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private void styleTabs() {
        if (controlTab == null || settingsTab == null || monitorTab == null || bridgeTab == null) return;
        controlTab.setText(t("Bediening", "Controls", "Bedienung"));
        monitorTab.setText(t("Monitoring", "Monitor"));
        bridgeTab.setText("Bridge");
        settingsTab.setText(t("Instellingen", "Settings", "Einstellungen"));
        controlTab.setBackground(round(!settingsOpen && !monitorOpen && !bridgeOpen ? ACCENT : PANEL_SOFT, 18));
        monitorTab.setBackground(round(monitorOpen ? ACCENT : PANEL_SOFT, 18));
        bridgeTab.setBackground(round(bridgeOpen ? ACCENT : PANEL_SOFT, 18));
        settingsTab.setBackground(round(settingsOpen ? ACCENT : PANEL_SOFT, 18));
    }

    private void styleLanguageButtons() {
        if (languageNl == null || languageEn == null || languageDe == null) return;
        languageNl.setBackground(round("nl".equals(language) ? ACCENT : PANEL_SOFT, 18));
        languageEn.setBackground(round("en".equals(language) ? ACCENT : PANEL_SOFT, 18));
        languageDe.setBackground(round("de".equals(language) ? ACCENT : PANEL_SOFT, 18));
    }

    private void styleConnectionButtons() {
        if (connectionAuto == null) return;
        connectionAuto.setBackground(round("auto".equals(connectionMode) ? ACCENT : PANEL_SOFT, 18));
        connectionInternal.setBackground(round("internal".equals(connectionMode) ? ACCENT : PANEL_SOFT, 18));
        connectionExternal.setBackground(round("external".equals(connectionMode) ? ACCENT : PANEL_SOFT, 18));
    }

    private void setLanguage(String value) {
        language = value;
        prefs().edit().putString("language", language).apply();
        buildShell();
        showSettings();
    }

    private void setConnectionMode(String value) {
        connectionMode = value;
        prefs().edit().putString("connection_mode", connectionMode).apply();
        styleConnectionButtons();
    }

    private void toggleShowFloorDoor() {
        showFloorDoor = !showFloorDoor;
        prefs().edit().putBoolean("show_floor_door", showFloorDoor).apply();
        if (showFloorDoorButton != null) {
            showFloorDoorButton.setText(showFloorDoor ? t("Etagebel tonen: aan", "Show floor bell: on") : t("Etagebel tonen: uit", "Show floor bell: off"));
        }
    }

    private void saveSettings() {
        String name = appNameInput.getText().toString().trim();
        if (name.isEmpty()) name = "Comelit dooropener";
        prefs().edit()
                .putString("app_name", name)
                .putString("language", language)
                .putString("connection_mode", connectionMode)
                .putBoolean("show_floor_door", showFloorDoor)
                .putInt("doorbell_repeat_count", doorbellRepeatCount())
                .apply();
        if (internalUrlInput != null) {
            prefs().edit()
                    .putString("ha_internal_url", clean(internalUrlInput.getText().toString()))
                    .putString("ha_external_url", clean(externalUrlInput.getText().toString()))
                    .putString("bridge_url", clean(bridgeUrlInput.getText().toString()))
                    .putString("bridge_proxy_url", clean(bridgeProxyUrlInput.getText().toString()))
                    .putString("bridge_proxy_token", bridgeProxyTokenInput.getText().toString().trim())
                    .putString("ha_url", clean(internalUrlInput.getText().toString()))
                    .putString("ha_token", tokenInput.getText().toString().trim())
                    .apply();
        }
        appTitle.setText(appTitleText());
        toast(t("Opgeslagen", "Saved", "Gespeichert"));
        refresh();
    }

    private String activeUrl() {
        SharedPreferences p = prefs();
        String internal = p.getString("ha_internal_url", p.getString("ha_url", ""));
        String external = p.getString("ha_external_url", "");
        if ("external".equals(connectionMode)) return external;
        if ("internal".equals(connectionMode)) return internal;
        if (isWifiConnected() && !internal.isEmpty()) return internal;
        String lastWorking = p.getString("ha_last_working_url", "");
        if (!lastWorking.isEmpty()) return lastWorking;
        return internal.isEmpty() ? external : internal;
    }

    private String fallbackUrl(String currentUrl) {
        SharedPreferences p = prefs();
        String internal = p.getString("ha_internal_url", p.getString("ha_url", ""));
        String external = p.getString("ha_external_url", "");
        if (!"auto".equals(connectionMode)) return "";
        if (currentUrl.equals(internal) && !external.isEmpty()) return external;
        if (currentUrl.equals(external) && !internal.isEmpty()) return internal;
        if (!internal.isEmpty() && !currentUrl.equals(internal)) return internal;
        if (!external.isEmpty() && !currentUrl.equals(external)) return external;
        return "";
    }

    private boolean isWifiConnected() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private String token() {
        return prefs().getString("ha_token", "");
    }

    private String clean(String url) {
        url = url == null ? "" : url.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private String appName() {
        return prefs().getString("app_name", "Comelit dooropener");
    }

    private String appTitleText() {
        return appName() + " - " + BuildConfig.VERSION_NAME;
    }

    private void migrateDefaultAppName() {
        String name = prefs().getString("app_name", "");
        if (name.isEmpty() || "Frank Deur".equals(name)) {
            prefs().edit().putString("app_name", "Comelit dooropener").apply();
        }
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("settings", MODE_PRIVATE);
    }

    private void refresh() {
        if (settingsOpen) return;
        if (bridgeOpen) return;
        if (monitorOpen) {
            refreshMonitor();
            return;
        }
        if (doorbellHistory != null) doorbellHistory.setText(doorbellHistoryText());
        String url = activeUrl();
        String token = token();
        if (url.isEmpty() || token.isEmpty()) {
            if (statusLine != null) statusLine.setText(t("Vul URL en token in bij Instellingen.", "Enter URL and token in Settings."));
            return;
        }
        executor.execute(() -> {
            try {
                JSONObject[] states = readDoorStates(url, token);
                String usedUrl = url;
                if (states == null) {
                    String fallback = fallbackUrl(url);
                    if (!fallback.isEmpty()) {
                        states = readDoorStates(fallback, token);
                        usedUrl = fallback;
                    }
                }
                if (states == null) throw new IllegalStateException(t("geen verbinding met intern of extern adres", "no connection to internal or external URL"));
                JSONObject entry = states[0];
                JSONObject floor = states[1];
                JSONObject heart = states[2];
                JSONObject ring = states[3];
                String finalUsedUrl = usedUrl;
                runOnUiThread(() -> {
                    if ("auto".equals(connectionMode)) prefs().edit().putString("ha_last_working_url", finalUsedUrl).apply();
                    if (statusLine != null) statusLine.setText(t("Verbonden met Home Assistant", "Connected to Home Assistant") + ("auto".equals(connectionMode) ? "\n" + (finalUsedUrl.startsWith("https://") ? t("Via externe URL", "Via external URL") : t("Via interne URL", "Via internal URL")) : ""));
                    if (entryStatus != null) entryStatus.setText(t("Bel hoofdingang", "Main entrance bell") + "\n" + pretty(entry.optString("state")));
                    if (floorStatus != null && floor != null) floorStatus.setText(t("Bel etage/appartement", "Floor/apartment bell") + "\n" + pretty(floor.optString("state")));
                    if (heartbeatStatus != null) heartbeatStatus.setText("Bridge\n" + pretty(heart.optString("state")));
                    updateAutoOpenUi(autoOpenMode());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (statusLine != null) statusLine.setText(t("Geen verbinding: ", "No connection: ") + e.getMessage());
                });
            }
        });
    }

    private JSONObject[] readDoorStates(String url, String token) {
        try {
            JSONObject entry = HaClient.getState(url, token, HaClient.ENTRY_DOOR);
            JSONObject floor = showFloorDoor ? HaClient.getState(url, token, HaClient.FLOOR_DOOR) : null;
            JSONObject heart = HaClient.getState(url, token, HaClient.HEARTBEAT);
            JSONObject ring = HaClient.getState(url, token, HaClient.RING_SWITCH);
            return new JSONObject[]{entry, floor, heart, ring};
        } catch (Exception e) {
            return null;
        }
    }

    private String bridgeUrl() {
        String proxy = prefs().getString("bridge_proxy_url", "");
        if (!proxy.isEmpty()) return proxy;
        return prefs().getString("bridge_url", "");
    }

    private String bridgeApiUrl(String path) {
        String url = bridgeUrl() + path;
        String proxy = prefs().getString("bridge_proxy_url", "");
        String token = prefs().getString("bridge_proxy_token", "");
        if (!proxy.isEmpty() && !token.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "token=" + token;
        }
        return url;
    }

    private String bridgeDisplayUrl() {
        String proxy = prefs().getString("bridge_proxy_url", "");
        if (!proxy.isEmpty()) return proxy;
        return prefs().getString("bridge_url", "");
    }

    private void loadBridgeConfig() {
        if (bridgeStatusLine != null) {
            bridgeStatusLine.setText(t("Bridge-config ophalen via ", "Loading bridge config via ") + bridgeDisplayUrl());
        }
        executor.execute(() -> {
            try {
                JSONObject config = HaClient.getJson(bridgeApiUrl("/api/config"));
                runOnUiThread(() -> {
                    if (bridgeStatusLine != null) {
                        bridgeStatusLine.setText(t("Bridge-config verbonden via ", "Bridge config connected via ") + bridgeDisplayUrl());
                    }
                    bridgeWifiSsidInput.setText(config.optString("wifi_ssid", ""));
                    bridgeMqttHostInput.setText(config.optString("mqtt_host", ""));
                    bridgeMqttUserInput.setText(config.optString("mqtt_user", ""));
                    bridgeMqttPrefixInput.setText(config.optString("mqtt_prefix", "SimpleBus"));
                    bridgeAddressInput.setText(String.valueOf(config.optInt("address", 0)));
                    bridgeGainInput.setText(String.valueOf(config.optInt("gain", 20)));
                    bridgeThresholdInput.setText(String.valueOf(config.optInt("threshold", 600)));
                    bridgeRtoInput.setText(String.valueOf(config.optInt("rto_minutes", 600)));
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    String message = t("Bridge-config niet bereikbaar via ", "Bridge config unavailable via ") + bridgeDisplayUrl() + "\n" + e.getClass().getSimpleName() + ": " + e.getMessage();
                    if (bridgeStatusLine != null) bridgeStatusLine.setText(message);
                    toast(t("Bridge-config niet bereikbaar", "Bridge config unavailable"));
                });
            }
        });
    }

    private void saveBridgeWifi() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("ssid", bridgeWifiSsidInput.getText().toString().trim());
        fields.put("password", bridgeWifiPasswordInput.getText().toString());
        postBridgeForm("/api/wifi", fields, t("WiFi opgeslagen. Bridge verbindt opnieuw.", "WiFi saved. Bridge reconnecting."));
    }

    private void saveBridgeMqtt() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("mqtt_host", bridgeMqttHostInput.getText().toString().trim());
        fields.put("mqtt_user", bridgeMqttUserInput.getText().toString().trim());
        fields.put("mqtt_pass", bridgeMqttPasswordInput.getText().toString());
        fields.put("mqtt_prefix", bridgeMqttPrefixInput.getText().toString().trim());
        postBridgeForm("/api/config", fields, t("MQTT opgeslagen", "MQTT saved"));
    }

    private void saveBridgeDevice() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("update_device", "1");
        fields.put("address", bridgeAddressInput.getText().toString().trim());
        fields.put("gain", bridgeGainInput.getText().toString().trim());
        fields.put("threshold", bridgeThresholdInput.getText().toString().trim());
        fields.put("rto_minutes", bridgeRtoInput.getText().toString().trim());
        postBridgeFormWithMqttFallback("/api/config", fields, t("Businstellingen opgeslagen", "Bus settings saved"));
    }

    private void sendTeachIn(boolean enabled) {
        String prefix = bridgeMqttPrefixInput == null ? "SimpleBus" : bridgeMqttPrefixInput.getText().toString().trim();
        if (prefix.isEmpty()) prefix = "SimpleBus";
        String payload = enabled ? "ON" : "OFF";
        publishMqtt(prefix + "/StartTeachIn", payload, enabled
                ? t("Adres leren gestart. Laat nu beneden aanbellen.", "Teach-in started. Ring downstairs now.")
                : t("Adres leren gestopt", "Teach-in stopped"));
    }

    private void copyLastSeenAddress() {
        executor.execute(() -> {
            try {
                JSONObject status = HaClient.getJson(bridgeApiUrl("/api/status"));
                int address = status.optInt("dbg_last_addr", 0);
                runOnUiThread(() -> {
                    if (address > 0) {
                        bridgeAddressInput.setText(String.valueOf(address));
                        toast(t("Adres overgenomen: ", "Address copied: ") + address);
                    } else {
                        toast(t("Nog geen adres gezien. Start adres leren en laat aanbellen.", "No address seen yet. Start teach-in and ring downstairs."));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast(t("Adres ophalen mislukt: ", "Address read failed: ") + e.getMessage()));
            }
        });
    }

    private void postBridgeForm(String path, Map<String, String> fields, String okMessage) {
        executor.execute(() -> {
            try {
                JSONObject response = HaClient.postForm(bridgeApiUrl(path), fields);
                runOnUiThread(() -> toast(response.optBoolean("ok", true) ? okMessage : response.optString("error", "Error")));
            } catch (Exception e) {
                runOnUiThread(() -> toast(t("Opslaan mislukt: ", "Save failed: ") + e.getMessage()));
            }
        });
    }

    private void postBridgeFormWithMqttFallback(String path, Map<String, String> fields, String okMessage) {
        executor.execute(() -> {
            try {
                JSONObject response = HaClient.postForm(bridgeApiUrl(path), fields);
                runOnUiThread(() -> toast(response.optBoolean("ok", true) ? okMessage : response.optString("error", "Error")));
            } catch (Exception directError) {
                try {
                    String prefix = bridgeMqttPrefixInput == null ? "SimpleBus" : bridgeMqttPrefixInput.getText().toString().trim();
                    if (prefix.isEmpty()) prefix = "SimpleBus";
                    if (fields.containsKey("gain")) publishMqttBlocking(prefix + "/SetGain", fields.get("gain"));
                    if (fields.containsKey("threshold")) publishMqttBlocking(prefix + "/SetComparatorVoltage", fields.get("threshold"));
                    if (fields.containsKey("rto_minutes")) publishMqttBlocking(prefix + "/SetRingToOpenTime", fields.get("rto_minutes"));
                    runOnUiThread(() -> toast(t("Via Home Assistant/MQTT verstuurd. Adres wijzigen kan alleen lokaal of via leerstand.", "Sent via Home Assistant/MQTT. Address changes require local access or teach-in.")));
                } catch (Exception mqttError) {
                    runOnUiThread(() -> toast(t("Opslaan mislukt: ", "Save failed: ") + mqttError.getMessage()));
                }
            }
        });
    }

    private void publishMqtt(String topic, String payload, String okMessage) {
        executor.execute(() -> {
            try {
                publishMqttBlocking(topic, payload);
                runOnUiThread(() -> toast(okMessage));
            } catch (Exception e) {
                runOnUiThread(() -> toast(t("MQTT-opdracht mislukt: ", "MQTT command failed: ") + e.getMessage()));
            }
        });
    }

    private void publishMqttBlocking(String topic, String payload) throws Exception {
        String json = "{\"topic\":\"" + topic + "\",\"payload\":\"" + payload + "\"}";
        HaClient.callServiceJson(activeUrl(), token(), "mqtt", "publish", json);
    }

    private void confirmAndPost(String path, String question) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(appName())
                .setMessage(question)
                .setNegativeButton(t("Annuleren", "Cancel"), null)
                .setPositiveButton("OK", (dialog, which) -> {
                    if ("/api/restart".equals(path)) {
                        String prefix = bridgeMqttPrefixInput == null ? "SimpleBus" : bridgeMqttPrefixInput.getText().toString().trim();
                        if (prefix.isEmpty()) prefix = "SimpleBus";
                        publishMqtt(prefix + "/Restart", "ON", t("Herstart via MQTT verstuurd", "Restart sent via MQTT"));
                    } else {
                        postBridgeForm(path, new LinkedHashMap<>(), t("Opdracht verstuurd", "Command sent"));
                    }
                })
                .show();
    }

    private void refreshMonitor() {
        if (bridgeUrl().isEmpty()) {
            if (statusLine != null) statusLine.setText(t("Vul de bridge URL in bij Instellingen.", "Enter the bridge URL in Settings."));
            return;
        }
        executor.execute(() -> {
            try {
                JSONObject status = HaClient.getJson(bridgeApiUrl("/api/status"));
                runOnUiThread(() -> {
                    if (statusLine != null) {
                        statusLine.setText(
                                t("Bridge online", "Bridge online") +
                                "\nFirmware: " + status.optString("fw", "-") +
                                "\nWiFi: " + status.optBoolean("wifi") + "  RSSI: " + status.optInt("rssi") +
                                "\nMQTT: " + status.optBoolean("mqtt") +
                                "\nTemp: " + status.optDouble("temp_c") + " C" +
                                "\n" + t("Laatste buscode: ", "Last bus code: ") + status.optInt("dbg_last_code") +
                                "\n" + t("Laatste adres: ", "Last address: ") + status.optInt("dbg_last_addr") +
                                "\nDecode OK: " + status.optInt("dbg_decode_ok") +
                                "\n" + t("Checksum fouten: ", "Checksum errors: ") + status.optInt("dbg_checksum_fail") +
                                "\n" + t("Adres mismatch: ", "Address mismatch: ") + status.optInt("dbg_addr_mismatch") +
                                "\n" + t("MQTT gepubliceerd: ", "MQTT published: ") + status.optInt("dbg_mqtt_published")
                        );
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (statusLine != null) statusLine.setText(t("Monitoring niet bereikbaar: ", "Monitor unavailable: ") + e.getMessage());
                });
            }
        });
    }

    private String pretty(String state) {
        if ("on".equalsIgnoreCase(state)) return t("Actief", "Active");
        if ("off".equalsIgnoreCase(state)) return t("Rust", "Idle");
        if ("unknown".equalsIgnoreCase(state)) return t("Onbekend", "Unknown");
        return state;
    }

    private void openDoor() {
        clearRingNotification();
        appendDoorbellHistory(t("Deur geopend", "Door opened", "Tür geöffnet"), false);
        callButton(HaClient.OPEN_DOOR, t("Deur openen verstuurd", "Open door sent", "Türöffnen gesendet"));
    }

    private void dismissRing() {
        clearRingNotification();
        if (prefs().getBoolean("doorbell_pending_handled", false)) {
            toast(t("Belmoment was al afgehandeld", "Ring was already handled"));
            return;
        }
        appendDoorbellHistory(t("Niet geopend", "Not opened", "Nicht geöffnet"), false);
        toast(t("Niet geopend", "Not opened", "Nicht geöffnet"));
    }

    private void clearRingNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(DoorMonitorService.RING_NOTIFICATION_ID);
        Intent stopSound = new Intent(this, DoorMonitorService.class);
        stopSound.setAction(DoorMonitorService.ACTION_STOP_RING_SOUND);
        startService(stopSound);
    }

    private void chooseDoorbellRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, t("Kies beltoon voor de deurbel", "Choose doorbell ringtone"));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri());
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    private Uri currentRingtoneUri() {
        String ringtone = prefs().getString("doorbell_ringtone_uri", "");
        if (ringtone == null || ringtone.isEmpty()) return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return Uri.parse(ringtone);
    }

    private String ringtoneName() {
        String savedName = prefs().getString("doorbell_ringtone_name", "");
        if (savedName != null && !savedName.isEmpty()) return savedName;
        return readableRingtoneName(currentRingtoneUri());
    }

    private String readableRingtoneName(Uri uri) {
        String displayName = displayNameFromUri(uri);
        if (displayName != null && !displayName.isEmpty()) return displayName;
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            if (ringtone != null) return ringtone.getTitle(this);
        } catch (Exception ignored) {
        }
        return t("standaard", "default");
    }

    private String displayNameFromUri(Uri uri) {
        if (uri == null) return "";
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (column >= 0) {
                    String name = cursor.getString(column);
                    if (name != null && !name.trim().isEmpty()) return name.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private int doorbellRepeatCount() {
        if (doorbellRepeatInput == null) return prefs().getInt("doorbell_repeat_count", 1);
        try {
            int value = Integer.parseInt(doorbellRepeatInput.getText().toString().trim());
            return Math.max(1, Math.min(10, value));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void appendDoorbellHistory(String message, boolean countRing) {
        String time = android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date());
        String entry = time + " - " + message;
        String previous = prefs().getString("doorbell_history", "");
        String combined = previous.isEmpty() ? entry : entry + "\n" + previous;
        String[] lines = combined.split("\\n");
        StringBuilder trimmed = new StringBuilder();
        for (int i = 0; i < lines.length && i < 20; i++) {
            if (i > 0) trimmed.append('\n');
            trimmed.append(lines[i]);
        }
        SharedPreferences.Editor editor = prefs().edit().putString("doorbell_history", trimmed.toString());
        if (countRing) editor.putInt("doorbell_ring_count", prefs().getInt("doorbell_ring_count", 0) + 1);
        editor.putBoolean("doorbell_pending_handled", true);
        editor.apply();
        if (doorbellHistory != null) doorbellHistory.setText(doorbellHistoryText());
        refreshServiceNotification();
    }

    private void refreshServiceNotification() {
        Intent intent = new Intent(this, DoorMonitorService.class);
        intent.setAction(DoorMonitorService.ACTION_REFRESH_SERVICE_NOTIFICATION);
        startService(intent);
    }

    private void checkForUpdates(boolean manual) {
        long now = System.currentTimeMillis();
        long last = prefs().getLong("last_update_check_at", 0);
        if (!manual && now - last < UPDATE_CHECK_INTERVAL_MS) return;
        prefs().edit().putLong("last_update_check_at", now).apply();
        executor.execute(() -> {
            try {
                JSONObject info = HaClient.getJson(UPDATE_INFO_URL);
                int latestCode = info.optInt("versionCode", 0);
                String latestName = info.optString("versionName", "");
                String apkUrl = info.optString("apkUrl", "");
                String releaseUrl = info.optString("releaseUrl", apkUrl);
                String notes = info.optString("notes", "");
                if (latestCode > BuildConfig.VERSION_CODE && !releaseUrl.isEmpty()) {
                    runOnUiThread(() -> showUpdateDialog(latestName, releaseUrl, notes));
                } else if (manual) {
                    runOnUiThread(() -> toast(t("Je gebruikt de nieuwste versie.", "You are using the latest version.")));
                }
            } catch (Exception e) {
                if (manual) runOnUiThread(() -> toast(t("Updatecontrole mislukt: ", "Update check failed: ") + e.getMessage()));
            }
        });
    }

    private void showUpdateDialog(String versionName, String releaseUrl, String notes) {
        String message = t("Nieuwe versie beschikbaar: ", "New version available: ") + versionName;
        if (notes != null && !notes.trim().isEmpty()) message += "\n\n" + notes.trim();
        new AlertDialog.Builder(this)
                .setTitle(t("Update beschikbaar", "Update available"))
                .setMessage(message)
                .setNegativeButton(t("Later", "Later"), null)
                .setPositiveButton(t("Downloaden", "Download"), (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl));
                    startActivity(intent);
                })
                .show();
    }

    private String doorbellHistoryText() {
        int count = prefs().getInt("doorbell_ring_count", 0);
        String history = prefs().getString("doorbell_history", "");
        String total = t("Aantal keer aangebeld: ", "Times rung: ", "Anzahl Klingelereignisse: ") + count;
        if (history == null || history.isEmpty()) {
            return total + "\n" + t("Nog geen belmomenten.", "No rings yet.", "Noch keine Klingelereignisse.");
        }
        return total + "\n\n" + translatedHistory(history);
    }

    private void callButton(String entityId, String message) {
        executor.execute(() -> {
            try {
                HaClient.callService(activeUrl(), token(), "button", "press", entityId);
                runOnUiThread(() -> toast(message));
            } catch (Exception e) {
                runOnUiThread(() -> toast(t("Mislukt: ", "Failed: ", "Fehlgeschlagen: ") + e.getMessage()));
            }
        });
    }

    private void toggleRing() {
        String[] choices = new String[]{
                t("Permanent uit", "Permanently off"),
                t("Eenmalig automatisch openen", "Auto-open once"),
                t("Tijdelijk automatisch openen...", "Timed auto-open..."),
                t("Permanent automatisch openen", "Permanent auto-open")
        };
        new AlertDialog.Builder(this)
                .setTitle(t("Automatisch openen", "Auto-open"))
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) setAutoOpenMode(AUTO_OPEN_OFF);
                    if (which == 1) confirmAutoOpenMode(AUTO_OPEN_ONCE);
                    if (which == 2) askTimedAutoOpenMinutes();
                    if (which == 3) confirmAutoOpenMode(AUTO_OPEN_ALWAYS);
                })
                .show();
    }

    private void confirmAutoOpenMode(String mode) {
        boolean permanent = AUTO_OPEN_ALWAYS.equals(mode);
        new AlertDialog.Builder(this)
                .setTitle(permanent
                        ? t("Permanent automatisch openen?", "Permanent auto-open?")
                        : t("Eenmalig automatisch openen?", "Auto-open once?"))
                .setMessage(t(
                        permanent
                                ? "De deur wordt dan bij elke bel automatisch geopend, totdat je dit weer uitzet. Dit is gevaarlijk als je het per ongeluk laat aanstaan."
                                : "De deur wordt bij de eerstvolgende bel automatisch geopend. Daarna zet de app automatisch openen weer uit.",
                        permanent
                                ? "The door will open automatically every time someone rings, until you turn this off again. This is dangerous if left on accidentally."
                                : "The door will open automatically on the next ring only. After that the app turns auto-open off again."))
                .setNegativeButton(t("Annuleren", "Cancel"), null)
                .setPositiveButton(t("Inschakelen", "Enable"), (dialog, which) -> setAutoOpenMode(mode))
                .show();
    }

    private void askTimedAutoOpenMinutes() {
        EditText minutesInput = input(t("Aantal minuten", "Minutes"), false, false);
        minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        minutesInput.setText(String.valueOf(prefs().getInt("auto_open_timed_minutes", 60)));
        new AlertDialog.Builder(this)
                .setTitle(t("Tijdelijk automatisch openen", "Timed auto-open"))
                .setMessage(t(
                        "Hoeveel minuten moet automatisch openen actief blijven?",
                        "For how many minutes should auto-open stay active?"))
                .setView(minutesInput)
                .setNegativeButton(t("Annuleren", "Cancel"), null)
                .setPositiveButton(t("Inschakelen", "Enable"), (dialog, which) -> {
                    int minutes = parseMinutes(minutesInput.getText().toString());
                    confirmTimedAutoOpen(minutes);
                })
                .show();
    }

    private void confirmTimedAutoOpen(int minutes) {
        new AlertDialog.Builder(this)
                .setTitle(t("Tijdelijk automatisch openen?", "Timed auto-open?"))
                .setMessage(t(
                        "De deur wordt dan automatisch geopend bij elke bel gedurende " + minutes + " minuten. Daarna schakelt de app automatisch openen weer uit.",
                        "The door will open automatically on each ring for " + minutes + " minutes. After that the app turns auto-open off again."))
                .setNegativeButton(t("Annuleren", "Cancel"), null)
                .setPositiveButton(t("Inschakelen", "Enable"), (dialog, which) -> setTimedAutoOpen(minutes))
                .show();
    }

    private int parseMinutes(String value) {
        try {
            return Math.max(1, Math.min(1440, Integer.parseInt(value.trim())));
        } catch (Exception ignored) {
            return 60;
        }
    }

    private void setTimedAutoOpen(int minutes) {
        long until = System.currentTimeMillis() + (minutes * 60_000L);
        prefs().edit()
                .putString("auto_open_mode", AUTO_OPEN_TIMED)
                .putInt("auto_open_timed_minutes", minutes)
                .putLong("auto_open_until_ms", until)
                .apply();
        updateAutoOpenUi(AUTO_OPEN_TIMED);
        refreshServiceNotification();
        disableBridgeRingToOpen();
        toast(t("Tijdelijk automatisch openen aan", "Timed auto-open on"));
    }

    private void setAutoOpenMode(String mode) {
        prefs().edit()
                .putString("auto_open_mode", mode)
                .remove("auto_open_until_ms")
                .apply();
        updateAutoOpenUi(mode);
        refreshServiceNotification();
        disableBridgeRingToOpen();
        if (AUTO_OPEN_ALWAYS.equals(mode)) {
            toast(t("Permanent automatisch openen aan", "Permanent auto-open on"));
        } else if (AUTO_OPEN_ONCE.equals(mode)) {
            toast(t("Eenmalig automatisch openen aan", "One-time auto-open on"));
        } else {
            toast(t("Automatisch openen uit", "Auto-open off"));
        }
    }

    private void disableBridgeRingToOpen() {
        executor.execute(() -> {
            try {
                HaClient.callService(activeUrl(), token(), "switch", "turn_off", HaClient.RING_SWITCH);
            } catch (Exception ignored) {
            }
        });
    }

    private String autoOpenMode() {
        return prefs().getString("auto_open_mode", AUTO_OPEN_OFF);
    }

    private void updateAutoOpenUi(String mode) {
        if (AUTO_OPEN_ALWAYS.equals(mode)) {
            updateAutoOpenUiText(t("PERMANENT AAN - deur opent bij elke bel", "PERMANENTLY ON - door opens on every ring"),
                    t("Automatisch openen uitschakelen of wijzigen", "Turn off or change auto-open"));
        } else if (AUTO_OPEN_TIMED.equals(mode)) {
            long until = prefs().getLong("auto_open_until_ms", 0);
            if (until > 0 && until <= System.currentTimeMillis()) {
                setAutoOpenMode(AUTO_OPEN_OFF);
                return;
            }
            updateAutoOpenUiText(t("TIJDELIJK AAN - tot ", "TIMED ON - until ") + autoOpenUntilText(until),
                    t("Automatisch openen uitschakelen of wijzigen", "Turn off or change auto-open"));
        } else if (AUTO_OPEN_ONCE.equals(mode)) {
            updateAutoOpenUiText(t("EENMALIG AAN - opent bij volgende bel", "ONCE - opens on next ring"),
                    t("Automatisch openen uitschakelen of wijzigen", "Turn off or change auto-open"));
        } else {
            updateAutoOpenUiText(t("UIT", "OFF"),
                    t("Automatisch openen kiezen", "Choose auto-open mode"));
        }
    }

    private void updateAutoOpenUiText(String status, String button) {
        if (ringStatus != null) ringStatus.setText(t("Automatisch openen", "Auto-open") + "\n" + status);
        if (ringButton != null) ringButton.setText(button);
    }

    private String autoOpenUntilText(long until) {
        if (until <= 0) return "-";
        return android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(until));
    }

    private void setBridgeAutoOpen(boolean enabled) {
        executor.execute(() -> {
            try {
                HaClient.callService(activeUrl(), token(), "switch", enabled ? "turn_on" : "turn_off", HaClient.RING_SWITCH);
                runOnUiThread(() -> {
                    toast(enabled ? t("Automatisch openen tijdelijk aan", "Auto-open temporarily on") : t("Automatisch openen uit", "Auto-open off"));
                    refresh();
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast(t("Mislukt: ", "Failed: ") + e.getMessage()));
            }
        });
    }

    private void toggleMonitor() {
        if (monitorRunning) {
            stopService(new Intent(this, DoorMonitorService.class));
            monitorRunning = false;
            prefs().edit().putBoolean("door_notifications_enabled", false).apply();
            if (monitorButton != null) monitorButton.setText(t("Belmeldingen aanzetten", "Turn doorbell alerts on"));
        } else {
            startForegroundService(new Intent(this, DoorMonitorService.class));
            monitorRunning = true;
            prefs().edit().putBoolean("door_notifications_enabled", true).apply();
            if (monitorButton != null) monitorButton.setText(t("Belmeldingen uitzetten", "Turn doorbell alerts off"));
        }
    }

    private void requestNotifications() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 50);
        }
    }

    private String t(String nl, String en) {
        if ("de".equals(language)) return germanText(nl, en);
        return "en".equals(language) ? en : nl;
    }

    private String t(String nl, String en, String de) {
        if ("de".equals(language)) return de;
        return "en".equals(language) ? en : nl;
    }

    private String translatedHistory(String history) {
        String[] lines = history.split("\\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) out.append('\n');
            out.append(translateHistoryLine(lines[i]));
        }
        return out.toString();
    }

    private String translateHistoryLine(String line) {
        int marker = line.indexOf(" - ");
        if (marker < 0) return translateHistoryMessage(line);
        return line.substring(0, marker + 3) + translateHistoryMessage(line.substring(marker + 3));
    }

    private String translateHistoryMessage(String message) {
        if ("Er werd aangebeld".equals(message)) return t("Er werd aangebeld", "Someone rang the doorbell", "Es wurde geklingelt");
        if ("Deur geopend".equals(message)) return t("Deur geopend", "Door opened", "Tür geöffnet");
        if ("Niet geopend".equals(message)) return t("Niet geopend", "Not opened", "Nicht geöffnet");
        if ("Niet geopend (geen actie)".equals(message)) return t("Niet geopend (geen actie)", "Not opened (no action)", "Nicht geöffnet (keine Aktion)");
        if ("Automatisch geopend (eenmalig)".equals(message)) return t("Automatisch geopend (eenmalig)", "Auto-opened (once)", "Automatisch geöffnet (einmalig)");
        if ("Automatisch geopend (tijdelijk)".equals(message)) return t("Automatisch geopend (tijdelijk)", "Auto-opened (timed)", "Automatisch geöffnet (zeitlich begrenzt)");
        if ("Automatisch geopend (permanent)".equals(message)) return t("Automatisch geopend (permanent)", "Auto-opened (permanent)", "Automatisch geöffnet (dauerhaft)");
        if ("Automatisch openen geblokkeerd (cooldown actief)".equals(message)) return t("Automatisch openen geblokkeerd (cooldown actief)", "Auto-open blocked (cooldown active)", "Automatisches Öffnen blockiert (Cooldown aktiv)");
        if (message.startsWith("Automatisch openen mislukt: ")) {
            return t("Automatisch openen mislukt: ", "Auto-open failed: ", "Automatisches Öffnen fehlgeschlagen: ") + translateHistoryMessage(message.substring("Automatisch openen mislukt: ".length()));
        }
        if ("geen verbinding met Home Assistant".equals(message)) return t("geen verbinding met Home Assistant", "no connection to Home Assistant", "keine Verbindung zu Home Assistant");
        if ("geen Home Assistant URL ingesteld".equals(message)) return t("geen Home Assistant URL ingesteld", "no Home Assistant URL configured", "keine Home-Assistant-URL eingestellt");
        return message;
    }

    private String germanText(String nl, String en) {
        switch (nl) {
            case "Beltoon opgeslagen": return "Klingelton gespeichert";
            case "Bediening": return "Bedienung";
            case "Monitoring": return "Überwachung";
            case "Instellingen": return "Einstellungen";
            case "Verbinden...": return "Verbinden...";
            case "Status": return "Status";
            case "Bel hoofdingang": return "Klingel Haupteingang";
            case "Bel etage/appartement": return "Klingel Etage/Wohnung";
            case "Automatisch openen": return "Automatisches Öffnen";
            case "Deur openen": return "Tür öffnen";
            case "Automatisch openen laden...": return "Automatisches Öffnen laden...";
            case "Belmeldingen uitzetten": return "Klingelmeldungen ausschalten";
            case "Belmeldingen aanzetten": return "Klingelmeldungen einschalten";
            case "Belgeschiedenis": return "Klingelverlauf";
            case "Appnaam": return "App-Name";
            case "Naam die bovenin de app en meldingen staat": return "Name, der oben in der App und in Meldungen angezeigt wird";
            case "Deurbellen": return "Türklingeln";
            case "Etagebel tonen: aan": return "Etagenklingel anzeigen: ein";
            case "Etagebel tonen: uit": return "Etagenklingel anzeigen: aus";
            case "Beltoon kiezen: ": return "Klingelton wählen: ";
            case "Beltoon herhalen": return "Klingelton wiederholen";
            case "Aantal keer afspelen, 1-10": return "Anzahl der Wiederholungen, 1-10";
            case "API / Web instellingen verbergen": return "API-/Web-Einstellungen ausblenden";
            case "API / Web instellingen": return "API-/Web-Einstellungen";
            case "Instellingen opslaan": return "Einstellungen speichern";
            case "Op updates controleren": return "Auf Updates prüfen";
            case "Je gebruikt de nieuwste versie.": return "Du verwendest die neueste Version.";
            case "Updatecontrole mislukt: ": return "Updateprüfung fehlgeschlagen: ";
            case "Nieuwe versie beschikbaar: ": return "Neue Version verfügbar: ";
            case "Update beschikbaar": return "Update verfügbar";
            case "Later": return "Später";
            case "Downloaden": return "Herunterladen";
            case "Beveiliging: gebruik buitenshuis bij voorkeur je Home Assistant Cloud/Nabu Casa URL met HTTPS. Het token wordt lokaal op je telefoon opgeslagen. Deel dit token niet.\n\nAutomatisch openen betekent: als iemand beneden aanbelt, opent de deur automatisch binnen de ingestelde tijd. WiFi-configuratie start alleen het tijdelijke configuratieportaal van de bridge.":
                return "Sicherheit: Verwende außerhalb deines Zuhauses am besten deine Home Assistant Cloud-/Nabu-Casa-URL mit HTTPS. Das Token wird lokal auf deinem Telefon gespeichert. Teile dieses Token nicht.\n\nAutomatisches Öffnen bedeutet: Wenn unten jemand klingelt, wird die Tür innerhalb der eingestellten Zeit automatisch geöffnet. Die WLAN-Konfiguration startet nur das temporäre Konfigurationsportal der Bridge.";
            case "Verbinding": return "Verbindung";
            case "Automatisch": return "Automatisch";
            case "Intern": return "Intern";
            case "Extern": return "Extern";
            case "Interne Home Assistant URL": return "Interne Home-Assistant-URL";
            case "Externe Home Assistant URL": return "Externe Home-Assistant-URL";
            case "Bijv. je Nabu Casa URL": return "z. B. deine Nabu-Casa-URL";
            case "Bridge web/API URL": return "Bridge-Web-/API-URL";
            case "Proxy is optioneel. Laat leeg als je telefoon rechtstreeks bij de bridge kan.":
                return "Proxy ist optional. Leer lassen, wenn dein Telefon die Bridge direkt erreichen kann.";
            case "Bridge proxy URL (optioneel, mag leeg blijven)": return "Bridge-Proxy-URL (optional, darf leer bleiben)";
            case "Leeg laten tenzij je zelf een proxy gebruikt": return "Leer lassen, außer du verwendest einen eigenen Proxy";
            case "Bridge proxy token (optioneel, mag leeg blijven)": return "Bridge-Proxy-Token (optional, darf leer bleiben)";
            case "Leeg laten tenzij je proxy een token gebruikt": return "Leer lassen, außer dein Proxy verwendet ein Token";
            case "Laat dit leeg als je telefoon rechtstreeks bij de bridge kan. Vul dit alleen in als je zelf een beveiligde proxy gebruikt; de app praat dan met die proxy en de proxy praat lokaal met de bridge.":
                return "Lass dies leer, wenn dein Telefon die Bridge direkt erreichen kann. Fülle es nur aus, wenn du einen eigenen gesicherten Proxy verwendest; die App spricht dann mit diesem Proxy, und der Proxy spricht lokal mit der Bridge.";
            case "Long-lived access token": return "Long-lived Access Token";
            case "Plak hier je token uit Home Assistant": return "Füge hier dein Token aus Home Assistant ein";
            case "Bus-monitor": return "Bus-Monitor";
            case "Deze pagina leest de debugstatus rechtstreeks uit de Simplebus2 Bridge. Dit is geen volledig live buslog, maar laat wel laatste code/adres en fouttellers zien.":
                return "Diese Seite liest den Debugstatus direkt aus der Simplebus2 Bridge. Das ist kein vollständiges Live-Buslog, zeigt aber den letzten Code/die letzte Adresse und Fehlerzähler an.";
            case "Monitoring ophalen...": return "Überwachung wird geladen...";
            case "Bridge instellingen": return "Bridge-Einstellungen";
            case "Deze instellingen gaan rechtstreeks naar de ESP32 Simplebus2 Bridge. Pas WiFi en factory reset voorzichtig aan.":
                return "Diese Einstellungen werden direkt an die ESP32 Simplebus2 Bridge gesendet. Sei vorsichtig mit WLAN und Factory Reset.";
            case "Bridge-config ophalen...": return "Bridge-Konfiguration wird geladen...";
            case "WiFi": return "WLAN";
            case "Nieuw WiFi-wachtwoord, leeg laten = niet wijzigen": return "Neues WLAN-Passwort, leer lassen = nicht ändern";
            case "WiFi-wachtwoord": return "WLAN-Passwort";
            case "WiFi opslaan en opnieuw verbinden": return "WLAN speichern und neu verbinden";
            case "WiFi-configuratie starten (4 min)": return "WLAN-Konfiguration starten (4 Min.)";
            case "Config mode verstuurd": return "Konfigurationsmodus gesendet";
            case "Gebruiker": return "Benutzer";
            case "Nieuw MQTT-wachtwoord, leeg laten = niet wijzigen": return "Neues MQTT-Passwort, leer lassen = nicht ändern";
            case "Broker host": return "Broker-Host";
            case "Wachtwoord": return "Passwort";
            case "Topic prefix": return "Topic-Präfix";
            case "MQTT opslaan": return "MQTT speichern";
            case "Bus en automatisch openen": return "Bus und automatisches Öffnen";
            case "Adres 0-255": return "Adresse 0-255";
            case "Automatisch openen minuten 1-1440": return "Automatisches Öffnen Minuten 1-1440";
            case "Adres": return "Adresse";
            case "Auto-open min.": return "Auto-Open Min.";
            case "Businstellingen opslaan": return "Bus-Einstellungen speichern";
            case "Adres leren volgens de maker: druk de fysieke knop op de bridge 3-4 seconden in tot de LED twee keer kort knippert en aan blijft. Druk daarna op de deurbelknop bij het appartement. De LED gaat uit zodra het adres geleerd is. De knoppen hieronder proberen dit via MQTT, maar dat werkt alleen als jouw firmware dat ondersteunt.":
                return "Adresse anlernen laut Entwickler: Drücke die physische Taste auf der Bridge 3-4 Sekunden, bis die LED zweimal kurz blinkt und an bleibt. Drücke danach die Klingeltaste an der Wohnung. Die LED geht aus, sobald die Adresse gelernt wurde. Die Tasten unten versuchen dies per MQTT, funktionieren aber nur, wenn deine Firmware das unterstützt.";
            case "MQTT leerstand proberen": return "MQTT-Lernmodus versuchen";
            case "MQTT leerstand stoppen": return "MQTT-Lernmodus stoppen";
            case "Laatst gezien adres overnemen": return "Zuletzt gesehene Adresse übernehmen";
            case "Systeem": return "System";
            case "Bridge herstarten": return "Bridge neu starten";
            case "Bridge nu herstarten?": return "Bridge jetzt neu starten?";
            case "Factory reset": return "Factory Reset";
            case "Factory reset wist alle bridge-instellingen. Zeker weten?": return "Factory Reset löscht alle Bridge-Einstellungen. Bist du sicher?";
            case "Vul URL en token in bij Instellingen.": return "URL und Token in den Einstellungen eintragen.";
            case "geen verbinding met intern of extern adres": return "keine Verbindung mit interner oder externer URL";
            case "Verbonden met Home Assistant": return "Mit Home Assistant verbunden";
            case "Via externe URL": return "Über externe URL";
            case "Via interne URL": return "Über interne URL";
            case "Geen verbinding: ": return "Keine Verbindung: ";
            case "Bridge-config ophalen via ": return "Bridge-Konfiguration wird geladen über ";
            case "Bridge-config verbonden via ": return "Bridge-Konfiguration verbunden über ";
            case "Bridge-config niet bereikbaar via ": return "Bridge-Konfiguration nicht erreichbar über ";
            case "Bridge-config niet bereikbaar": return "Bridge-Konfiguration nicht erreichbar";
            case "WiFi opgeslagen. Bridge verbindt opnieuw.": return "WLAN gespeichert. Bridge verbindet neu.";
            case "MQTT opgeslagen": return "MQTT gespeichert";
            case "Businstellingen opgeslagen": return "Bus-Einstellungen gespeichert";
            case "Adres leren gestart. Laat nu beneden aanbellen.": return "Adresslernen gestartet. Jetzt unten klingeln lassen.";
            case "Adres leren gestopt": return "Adresslernen gestoppt";
            case "Adres overgenomen: ": return "Adresse übernommen: ";
            case "Nog geen adres gezien. Start adres leren en laat aanbellen.": return "Noch keine Adresse gesehen. Starte das Adresslernen und lass klingeln.";
            case "Adres ophalen mislukt: ": return "Adresse abrufen fehlgeschlagen: ";
            case "Opslaan mislukt: ": return "Speichern fehlgeschlagen: ";
            case "Via Home Assistant/MQTT verstuurd. Adres wijzigen kan alleen lokaal of via leerstand.": return "Über Home Assistant/MQTT gesendet. Die Adresse kann nur lokal oder über den Lernmodus geändert werden.";
            case "MQTT-opdracht mislukt: ": return "MQTT-Befehl fehlgeschlagen: ";
            case "Annuleren": return "Abbrechen";
            case "Herstart via MQTT verstuurd": return "Neustart per MQTT gesendet";
            case "Opdracht verstuurd": return "Befehl gesendet";
            case "Vul de bridge URL in bij Instellingen.": return "Bridge-URL in den Einstellungen eintragen.";
            case "Bridge online": return "Bridge online";
            case "Laatste buscode: ": return "Letzter Buscode: ";
            case "Laatste adres: ": return "Letzte Adresse: ";
            case "Checksum fouten: ": return "Checksum-Fehler: ";
            case "Adres mismatch: ": return "Adressabweichung: ";
            case "MQTT gepubliceerd: ": return "MQTT veröffentlicht: ";
            case "Monitoring niet bereikbaar: ": return "Überwachung nicht erreichbar: ";
            case "Actief": return "Aktiv";
            case "Rust": return "Ruhe";
            case "Onbekend": return "Unbekannt";
            case "Belmoment was al afgehandeld": return "Klingelereignis wurde bereits verarbeitet";
            case "Kies beltoon voor de deurbel": return "Klingelton für die Tür wählen";
            case "standaard": return "Standard";
            case "Mislukt: ": return "Fehlgeschlagen: ";
            case "Permanent uit": return "Dauerhaft aus";
            case "Eenmalig automatisch openen": return "Einmalig automatisch öffnen";
            case "Tijdelijk automatisch openen...": return "Zeitlich begrenzt automatisch öffnen...";
            case "Permanent automatisch openen": return "Dauerhaft automatisch öffnen";
            case "Permanent automatisch openen?": return "Dauerhaft automatisch öffnen?";
            case "Eenmalig automatisch openen?": return "Einmalig automatisch öffnen?";
            case "De deur wordt dan bij elke bel automatisch geopend, totdat je dit weer uitzet. Dit is gevaarlijk als je het per ongeluk laat aanstaan.":
                return "Die Tür wird dann bei jedem Klingeln automatisch geöffnet, bis du dies wieder ausschaltest. Das ist gefährlich, wenn es versehentlich eingeschaltet bleibt.";
            case "De deur wordt bij de eerstvolgende bel automatisch geopend. Daarna zet de app automatisch openen weer uit.":
                return "Die Tür wird beim nächsten Klingeln automatisch geöffnet. Danach schaltet die App das automatische Öffnen wieder aus.";
            case "Inschakelen": return "Einschalten";
            case "Aantal minuten": return "Anzahl Minuten";
            case "Tijdelijk automatisch openen": return "Zeitlich begrenzt automatisch öffnen";
            case "Hoeveel minuten moet automatisch openen actief blijven?": return "Wie viele Minuten soll das automatische Öffnen aktiv bleiben?";
            case "Tijdelijk automatisch openen?": return "Zeitlich begrenzt automatisch öffnen?";
            case "Tijdelijk automatisch openen aan": return "Zeitlich begrenztes automatisches Öffnen ein";
            case "Permanent automatisch openen aan": return "Dauerhaftes automatisches Öffnen ein";
            case "Eenmalig automatisch openen aan": return "Einmaliges automatisches Öffnen ein";
            case "Automatisch openen uit": return "Automatisches Öffnen aus";
            case "PERMANENT AAN - deur opent bij elke bel": return "DAUERHAFT EIN - Tür öffnet bei jedem Klingeln";
            case "Automatisch openen uitschakelen of wijzigen": return "Automatisches Öffnen ausschalten oder ändern";
            case "TIJDELIJK AAN - tot ": return "ZEITLICH BEGRENZT EIN - bis ";
            case "EENMALIG AAN - opent bij volgende bel": return "EINMALIG EIN - öffnet beim nächsten Klingeln";
            case "UIT": return "AUS";
            case "Automatisch openen kiezen": return "Automatisches Öffnen wählen";
            case "Automatisch openen tijdelijk aan": return "Automatisches Öffnen vorübergehend ein";
            default:
                if (nl.startsWith("De deur wordt dan automatisch geopend bij elke bel gedurende ") && nl.endsWith(" minuten. Daarna schakelt de app automatisch openen weer uit.")) {
                    String minutes = nl.substring("De deur wordt dan automatisch geopend bij elke bel gedurende ".length(), nl.indexOf(" minuten."));
                    return "Die Tür wird dann bei jedem Klingeln " + minutes + " Minuten lang automatisch geöffnet. Danach schaltet die App das automatische Öffnen wieder aus.";
                }
                return en;
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
