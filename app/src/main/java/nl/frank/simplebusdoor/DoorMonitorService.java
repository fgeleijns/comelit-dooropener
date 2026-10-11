package nl.frank.simplebusdoor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

public class DoorMonitorService extends Service {
    static final String ACTION_STOP_RING_SOUND = "stop_ring_sound";
    static final String ACTION_REFRESH_SERVICE_NOTIFICATION = "refresh_service_notification";
    static final String SERVICE_CHANNEL = "door_monitor_service";
    static final String RING_CHANNEL = "door_ring_manual_sound_v1";
    static final int SERVICE_NOTIFICATION_ID = 10;
    static final int RING_NOTIFICATION_ID = 11;
    private static final String AUTO_OPEN_OFF = "off";
    private static final String AUTO_OPEN_ONCE = "once";
    private static final String AUTO_OPEN_TIMED = "timed";
    private static final String AUTO_OPEN_ALWAYS = "always";
    private static final long AUTO_OPEN_DELAY_MS = 900;
    private static final long AUTO_OPEN_COOLDOWN_MS = 30000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Ringtone activeRingtone;
    private AudioAttributes ringAudioAttributes;
    private int repeatsLeft;
    private boolean lastEntryOn;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            poll();
            handler.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(SERVICE_NOTIFICATION_ID, serviceNotification());
        handler.post(tick);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_STOP_RING_SOUND.equals(intent.getAction())) {
                stopDoorbellSound();
                updateServiceNotification();
            } else if (ACTION_REFRESH_SERVICE_NOTIFICATION.equals(intent.getAction())) {
                updateServiceNotification();
            }
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopDoorbellSound();
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (prefs.getBoolean("door_notifications_enabled", false)) {
            Intent restart = new Intent(this, DoorMonitorService.class);
            startForegroundService(restart);
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }

    private void poll() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String token = prefs.getString("ha_token", "");
        String[] urls = activeUrls(prefs);
        if (urls.length == 0 || token.isEmpty()) return;
        executor.execute(() -> {
            for (String baseUrl : urls) {
                if (baseUrl == null || baseUrl.isEmpty()) continue;
                try {
                    JSONObject state = HaClient.getState(baseUrl, token, HaClient.ENTRY_DOOR);
                    prefs.edit().putString("ha_last_working_url", baseUrl).apply();
                    boolean on = "on".equalsIgnoreCase(state.optString("state"));
                    if (on && !lastEntryOn) {
                        showRingNotification();
                    }
                    lastEntryOn = on;
                    return;
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void showRingNotification() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        NotificationManager manager = getSystemService(NotificationManager.class);
        createRingChannel(manager);
        appendDoorbellHistory(prefs, t(prefs, "Er werd aangebeld", "Someone rang the doorbell", "Es wurde geklingelt"), true);
        long ringId = System.currentTimeMillis();
        prefs.edit()
                .putLong("doorbell_pending_id", ringId)
                .putBoolean("doorbell_pending_handled", false)
                .apply();
        updateServiceNotification();
        playDoorbellSound(prefs);
        long activeMs = doorbellNotificationMs(prefs);
        handler.postDelayed(() -> markUnansweredIfNeeded(ringId), activeMs);

        Intent ringIntent = new Intent(this, DoorRingActivity.class);
        ringIntent.setAction(DoorRingActivity.ACTION_SHOW);
        PendingIntent ringPending = PendingIntent.getActivity(this, RING_NOTIFICATION_ID + 2, ringIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(this, DoorRingActivity.class);
        openIntent.setAction(DoorRingActivity.ACTION_OPEN);
        PendingIntent openPending = PendingIntent.getActivity(this, RING_NOTIFICATION_ID, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent dismissIntent = new Intent(this, DoorRingActivity.class);
        dismissIntent.setAction(DoorRingActivity.ACTION_DISMISS);
        PendingIntent dismissPending = PendingIntent.getActivity(this, RING_NOTIFICATION_ID + 1, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long now = System.currentTimeMillis();
        String time = android.text.format.DateFormat.getTimeFormat(this).format(new Date(now));
        Notification notification = baseNotification(RING_CHANNEL,
                        t(prefs, "Er werd aangebeld", "Someone rang the doorbell", "Es wurde geklingelt"),
                        t(prefs, "Om ", "At ", "Um ") + time + t(prefs, ". Tik om de deurbediening te openen.", ". Tap to open door controls.", ". Tippen, um die Türbedienung zu öffnen."))
                .addAction(android.R.drawable.ic_menu_send, t(prefs, "Deur openen", "Open door", "Tür öffnen"), openPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, t(prefs, "Niet openen", "Do not open", "Nicht öffnen"), dismissPending)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setWhen(now)
                .setShowWhen(true)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(ringPending)
                .setFullScreenIntent(ringPending, true)
                .setTimeoutAfter(activeMs)
                .build();
        manager.notify(RING_NOTIFICATION_ID, notification);
        handler.postDelayed(() -> executor.execute(() -> handleAutoOpen(getSharedPreferences("settings", MODE_PRIVATE), ringId)), AUTO_OPEN_DELAY_MS);
    }

    private void handleAutoOpen(SharedPreferences prefs, long ringId) {
        String mode = activeAutoOpenMode(prefs);
        if (!AUTO_OPEN_ONCE.equals(mode) && !AUTO_OPEN_TIMED.equals(mode) && !AUTO_OPEN_ALWAYS.equals(mode)) return;
        long now = System.currentTimeMillis();
        if (prefs.getLong("auto_open_last_ring_id", -1) == ringId) return;
        long lastAttemptAt = prefs.getLong("auto_open_last_attempt_at", 0);
        if (lastAttemptAt > 0 && now - lastAttemptAt < AUTO_OPEN_COOLDOWN_MS) {
            SharedPreferences.Editor cooldown = prefs.edit()
                    .putBoolean("doorbell_pending_handled", true)
                    .putLong("auto_open_last_ring_id", ringId);
            if (AUTO_OPEN_ONCE.equals(mode)) cooldown.putString("auto_open_mode", AUTO_OPEN_OFF);
            cooldown.apply();
            appendDoorbellHistory(prefs, t(prefs, "Automatisch openen geblokkeerd (cooldown actief)", "Auto-open blocked (cooldown active)", "Automatisches Öffnen blockiert (Cooldown aktiv)"), false);
            updateServiceNotification();
            return;
        }

        SharedPreferences.Editor attempt = prefs.edit()
                .putBoolean("doorbell_pending_handled", true)
                .putLong("auto_open_last_ring_id", ringId)
                .putLong("auto_open_last_attempt_at", now);
        if (AUTO_OPEN_ONCE.equals(mode)) attempt.putString("auto_open_mode", AUTO_OPEN_OFF);
        attempt.apply();

        try {
            callOpenDoorRepeated(prefs);
            appendDoorbellHistory(prefs,
                    autoOpenHistoryText(mode),
                    false);
            updateServiceNotification();
        } catch (Exception e) {
            appendDoorbellHistory(prefs, t(prefs, "Automatisch openen mislukt: ", "Auto-open failed: ", "Automatisches Öffnen fehlgeschlagen: ") + readableError(prefs, e), false);
            updateServiceNotification();
        }
    }

    private void callOpenDoorRepeated(SharedPreferences prefs) throws Exception {
        int attempts = Math.max(1, Math.min(3, prefs.getInt("auto_open_repeat_count", 1)));
        int delayMs = Math.max(300, Math.min(2000, prefs.getInt("open_repeat_delay_ms", 700)));
        boolean sent = false;
        Exception last = null;
        for (int i = 0; i < attempts; i++) {
            if (i > 0) Thread.sleep(delayMs);
            try {
                callServiceWithFallback(prefs, "button", "press", HaClient.OPEN_DOOR);
                sent = true;
            } catch (Exception e) {
                last = e;
            }
        }
        if (!sent && last != null) throw last;
    }

    private void callServiceWithFallback(SharedPreferences prefs, String domain, String service, String entityId) throws Exception {
        String token = prefs.getString("ha_token", "");
        Exception last = null;
        for (String baseUrl : activeUrls(prefs)) {
            if (baseUrl == null || baseUrl.isEmpty()) continue;
            try {
                HaClient.callService(baseUrl, token, domain, service, entityId);
                prefs.edit().putString("ha_last_working_url", baseUrl).apply();
                return;
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) throw last;
        throw new IllegalStateException(t(prefs, "geen Home Assistant URL ingesteld", "no Home Assistant URL configured", "keine Home-Assistant-URL eingestellt"));
    }

    private String readableError(SharedPreferences prefs, Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty() || "0".equals(message.trim())) {
            return t(prefs, "geen verbinding met Home Assistant", "no connection to Home Assistant", "keine Verbindung zu Home Assistant");
        }
        return message;
    }

    private String activeAutoOpenMode(SharedPreferences prefs) {
        String mode = prefs.getString("auto_open_mode", AUTO_OPEN_OFF);
        if (!AUTO_OPEN_TIMED.equals(mode)) return mode;
        long until = prefs.getLong("auto_open_until_ms", 0);
        if (until > System.currentTimeMillis()) return mode;
        prefs.edit()
                .putString("auto_open_mode", AUTO_OPEN_OFF)
                .remove("auto_open_until_ms")
                .apply();
        return AUTO_OPEN_OFF;
    }

    private String autoOpenHistoryText(String mode) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (AUTO_OPEN_ONCE.equals(mode)) return t(prefs, "Automatisch geopend (eenmalig)", "Auto-opened (once)", "Automatisch geöffnet (einmalig)");
        if (AUTO_OPEN_TIMED.equals(mode)) return t(prefs, "Automatisch geopend (tijdelijk)", "Auto-opened (timed)", "Automatisch geöffnet (zeitlich begrenzt)");
        return t(prefs, "Automatisch geopend (permanent)", "Auto-opened (permanent)", "Automatisch geöffnet (dauerhaft)");
    }

    private void playDoorbellSound(SharedPreferences prefs) {
        try {
            stopDoorbellSound();
            ringAudioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            repeatsLeft = Math.max(1, Math.min(10, prefs.getInt("doorbell_repeat_count", 1))) - 1;
            playSingleDoorbellTone(prefs);
            scheduleNextRing();
        } catch (Exception ignored) {
        }
    }

    private long doorbellNotificationMs(SharedPreferences prefs) {
        int seconds = prefs.getInt("doorbell_notification_seconds", 120);
        seconds = Math.max(30, Math.min(300, seconds));
        return seconds * 1000L;
    }

    private void scheduleNextRing() {
        handler.postDelayed(() -> {
            try {
                if (repeatsLeft > 0) {
                    repeatsLeft--;
                    playSingleDoorbellTone(getSharedPreferences("settings", MODE_PRIVATE));
                    scheduleNextRing();
                } else {
                    if (activeRingtone != null && activeRingtone.isPlaying()) activeRingtone.stop();
                }
            } catch (Exception ignored) {
            }
        }, 4000);
        handler.postDelayed(this::stopDoorbellSound, 120000);
    }

    private void playSingleDoorbellTone(SharedPreferences prefs) {
        try {
            if (activeRingtone != null && activeRingtone.isPlaying()) activeRingtone.stop();
        } catch (Exception ignored) {
        }
        activeRingtone = RingtoneManager.getRingtone(this, ringSound(prefs));
        if (activeRingtone == null) return;
        if (ringAudioAttributes != null) activeRingtone.setAudioAttributes(ringAudioAttributes);
        activeRingtone.play();
    }

    private void stopDoorbellSound() {
        try {
            if (activeRingtone != null && activeRingtone.isPlaying()) {
                activeRingtone.stop();
            }
        } catch (Exception ignored) {
        } finally {
            activeRingtone = null;
            ringAudioAttributes = null;
            repeatsLeft = 0;
        }
    }

    private String activeUrl(SharedPreferences prefs) {
        String mode = prefs.getString("connection_mode", "auto");
        String internal = prefs.getString("ha_internal_url", prefs.getString("ha_url", ""));
        String external = prefs.getString("ha_external_url", "");
        if ("external".equals(mode)) return external;
        if ("internal".equals(mode)) return internal;
        if (isWifiConnected() && !internal.isEmpty()) return internal;
        String lastWorking = prefs.getString("ha_last_working_url", "");
        if (!lastWorking.isEmpty()) return lastWorking;
        return internal.isEmpty() ? external : internal;
    }

    private String[] activeUrls(SharedPreferences prefs) {
        String mode = prefs.getString("connection_mode", "auto");
        String internal = prefs.getString("ha_internal_url", prefs.getString("ha_url", ""));
        String external = prefs.getString("ha_external_url", "");
        if ("external".equals(mode)) return external.isEmpty() ? new String[0] : new String[]{external};
        if ("internal".equals(mode)) return internal.isEmpty() ? new String[0] : new String[]{internal};
        if (isWifiConnected()) {
            if (!internal.isEmpty() && !external.isEmpty()) return new String[]{internal, external};
            if (!internal.isEmpty()) return new String[]{internal};
            if (!external.isEmpty()) return new String[]{external};
            return new String[0];
        }
        String lastWorking = prefs.getString("ha_last_working_url", "");
        if (!lastWorking.isEmpty() && lastWorking.equals(external) && !internal.isEmpty()) {
            return new String[]{external, internal};
        }
        if (!lastWorking.isEmpty() && lastWorking.equals(internal) && !external.isEmpty()) {
            return new String[]{internal, external};
        }
        if (!internal.isEmpty() && !external.isEmpty()) return new String[]{internal, external};
        if (!internal.isEmpty()) return new String[]{internal};
        if (!external.isEmpty()) return new String[]{external};
        return new String[0];
    }

    private boolean isWifiConnected() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private String appName() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String name = prefs.getString("app_name", "Comelit dooropener");
        if ("Frank Deur".equals(name)) {
            name = "Comelit dooropener";
            prefs.edit().putString("app_name", name).apply();
        }
        return name.toUpperCase(Locale.ROOT);
    }

    private Notification.Builder baseNotification(String channel, String title, String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, SERVICE_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pending)
                .setOngoing(false);
    }

    private Notification serviceNotification() {
        String text = serviceSummary();
        return baseNotification(SERVICE_CHANNEL, appName(), firstLine(text))
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setOngoing(true)
                .build();
    }

    private void updateServiceNotification() {
        getSystemService(NotificationManager.class).notify(SERVICE_NOTIFICATION_ID, serviceNotification());
    }

    private String serviceSummary() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int count = prefs.getInt("doorbell_ring_count", 0);
        String history = prefs.getString("doorbell_history", "");
        String summary = t(prefs, "Aangebeld: ", "Times rung: ", "Anzahl Klingelereignisse: ") + count;
        String mode = activeAutoOpenMode(prefs);
        if (AUTO_OPEN_ONCE.equals(mode)) summary += "\n" + t(prefs, "Automatisch openen: eenmalig aan", "Auto-open: once enabled", "Automatisch öffnen: einmalig aktiv");
        if (AUTO_OPEN_TIMED.equals(mode)) summary += "\n" + t(prefs, "Automatisch openen: tijdelijk tot ", "Auto-open: timed until ", "Automatisch öffnen: zeitlich begrenzt bis ") + timeText(prefs.getLong("auto_open_until_ms", 0));
        if (AUTO_OPEN_ALWAYS.equals(mode)) summary += "\n" + t(prefs, "Automatisch openen: permanent aan", "Auto-open: permanently enabled", "Automatisch öffnen: dauerhaft aktiv");
        if (history == null || history.isEmpty()) return summary + "\n" + t(prefs, "Nog geen belmomenten.", "No rings yet.", "Noch keine Klingelereignisse.");
        String[] lines = history.split("\\n");
        StringBuilder builder = new StringBuilder(summary);
        for (int i = 0; i < lines.length && i < 3; i++) {
            builder.append('\n').append(displayHistoryLine(lines[i]));
        }
        return builder.toString();
    }

    private String displayHistoryLine(String line) {
        int marker = line.indexOf(" - ");
        if (marker < 0) return line;
        return line.substring(0, marker) + "  |  " + line.substring(marker + 3);
    }

    private String timeText(long millis) {
        if (millis <= 0) return "-";
        return android.text.format.DateFormat.getTimeFormat(this).format(new Date(millis));
    }

    private String firstLine(String text) {
        int newline = text.indexOf('\n');
        return newline >= 0 ? text.substring(0, newline) : text;
    }

    private void createChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);

        NotificationChannel serviceChannel = new NotificationChannel(
                SERVICE_CHANNEL,
                "Deurmonitor actief",
                NotificationManager.IMPORTANCE_LOW
        );
        serviceChannel.setDescription("Stille melding dat de deurmonitor op de achtergrond draait");
        serviceChannel.setSound(null, null);
        manager.createNotificationChannel(serviceChannel);

        createRingChannel(manager);
    }

    private void createRingChannel(NotificationManager manager) {
        NotificationChannel ringChannel = new NotificationChannel(
                RING_CHANNEL,
                "Deurbel",
                NotificationManager.IMPORTANCE_HIGH
        );
        ringChannel.setDescription("Luide belmelding wanneer de Simplebus2 Bridge aanbellen detecteert");
        ringChannel.setSound(null, null);
        ringChannel.enableVibration(true);
        ringChannel.setVibrationPattern(new long[]{0, 350, 180, 350, 180, 700});
        manager.createNotificationChannel(ringChannel);
    }

    private Uri ringSound(SharedPreferences prefs) {
        String ringtone = prefs.getString("doorbell_ringtone_uri", "");
        if (ringtone == null || ringtone.isEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }
        return Uri.parse(ringtone);
    }

    private void appendDoorbellHistory(SharedPreferences prefs, String message, boolean countRing) {
        String time = android.text.format.DateFormat.getTimeFormat(this).format(new Date());
        String entry = time + " - " + message;
        String previous = prefs.getString("doorbell_history", "");
        String combined = previous.isEmpty() ? entry : entry + "\n" + previous;
        String[] lines = combined.split("\\n");
        StringBuilder trimmed = new StringBuilder();
        for (int i = 0; i < lines.length && i < 20; i++) {
            if (i > 0) trimmed.append('\n');
            trimmed.append(lines[i]);
        }
        SharedPreferences.Editor editor = prefs.edit().putString("doorbell_history", trimmed.toString());
        if (countRing) editor.putInt("doorbell_ring_count", prefs.getInt("doorbell_ring_count", 0) + 1);
        editor.apply();
    }

    private void markUnansweredIfNeeded(long ringId) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        if (prefs.getLong("doorbell_pending_id", -1) != ringId) return;
        if (prefs.getBoolean("doorbell_pending_handled", false)) return;
        appendDoorbellHistory(prefs, t(prefs, "Niet geopend (geen actie)", "Not opened (no action)", "Nicht geöffnet (keine Aktion)"), false);
        prefs.edit().putBoolean("doorbell_pending_handled", true).apply();
        getSystemService(NotificationManager.class).cancel(RING_NOTIFICATION_ID);
        updateServiceNotification();
    }

    private String t(SharedPreferences prefs, String nl, String en, String de) {
        String lang = prefs.getString("language", "nl");
        if ("de".equals(lang)) return de;
        if ("en".equals(lang)) return en;
        return nl;
    }
}
