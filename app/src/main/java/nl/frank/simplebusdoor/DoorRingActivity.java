package nl.frank.simplebusdoor;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoorRingActivity extends Activity {
    static final String ACTION_SHOW = "nl.frank.simplebusdoor.SHOW_RING";
    static final String ACTION_OPEN = "nl.frank.simplebusdoor.OPEN_FROM_RING";
    static final String ACTION_DISMISS = "nl.frank.simplebusdoor.DISMISS_FROM_RING";

    private static final int BG = Color.rgb(18, 20, 22);
    private static final int PANEL = Color.rgb(34, 37, 40);
    private static final int ACCENT = Color.rgb(0, 150, 95);
    private static final int ACCENT_DARK = Color.rgb(0, 118, 78);
    private static final int TEXT = Color.rgb(248, 250, 252);
    private static final int MUTED = Color.rgb(182, 194, 209);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        String action = getIntent() == null ? ACTION_SHOW : getIntent().getAction();
        if (ACTION_OPEN.equals(action)) {
            openDoor();
            return;
        }
        if (ACTION_DISMISS.equals(action)) {
            dismissRing();
            return;
        }
        showRingScreen();
    }

    private void showRingScreen() {
        SharedPreferences prefs = prefs();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(32), dp(28), dp(32));
        root.setBackgroundColor(BG);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(24), dp(28), dp(24), dp(28));
        card.setBackground(round(PANEL, 24));
        root.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView title = text(t(prefs, "Er werd aangebeld", "Someone rang the doorbell", "Es wurde geklingelt"), 24, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        String time = android.text.format.DateFormat.getTimeFormat(this).format(new Date());
        TextView subtitle = text(t(prefs, "Om ", "At ", "Um ") + time, 16, false);
        subtitle.setTextColor(MUTED);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleLp = new LinearLayout.LayoutParams(-1, -2);
        subtitleLp.setMargins(0, dp(8), 0, dp(22));
        card.addView(subtitle, subtitleLp);

        Button open = button(t(prefs, "Entree deur openen", "Open entrance door", "Eingangstür öffnen"), ACCENT);
        open.setOnClickListener(v -> openDoor());
        card.addView(open);

        Button dismiss = button(t(prefs, "Niet openen", "Do not open", "Nicht öffnen"), Color.rgb(70, 75, 80));
        dismiss.setOnClickListener(v -> dismissRing());
        card.addView(dismiss);

        setContentView(root);
    }

    private void openDoor() {
        clearRingNotificationAndSound();
        markHandled(t(prefs(), "Deur geopend", "Door opened", "Tür geöffnet"));
        int attempts = Math.max(1, Math.min(5, prefs().getInt("manual_open_repeat_count", 2)));
        int delay = Math.max(300, Math.min(2000, prefs().getInt("open_repeat_delay_ms", 700)));
        executor.execute(() -> {
            try {
                for (int i = 0; i < attempts; i++) {
                    callServiceWithFallback("button", "press", HaClient.OPEN_DOOR);
                    if (i + 1 < attempts) Thread.sleep(delay);
                }
                handler.post(() -> {
                    Toast.makeText(this, t(prefs(), "Deur openen verstuurd", "Open door sent", "Türöffnen gesendet"), Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(this, t(prefs(), "Mislukt: ", "Failed: ", "Fehlgeschlagen: ") + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void dismissRing() {
        clearRingNotificationAndSound();
        markHandled(t(prefs(), "Niet geopend", "Not opened", "Nicht geöffnet"));
        Toast.makeText(this, t(prefs(), "Niet geopend", "Not opened", "Nicht geöffnet"), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void clearRingNotificationAndSound() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(DoorMonitorService.RING_NOTIFICATION_ID);
        Intent stopSound = new Intent(this, DoorMonitorService.class);
        stopSound.setAction(DoorMonitorService.ACTION_STOP_RING_SOUND);
        startService(stopSound);
    }

    private void markHandled(String message) {
        SharedPreferences prefs = prefs();
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
        prefs.edit()
                .putString("doorbell_history", trimmed.toString())
                .putBoolean("doorbell_pending_handled", true)
                .apply();
        Intent refresh = new Intent(this, DoorMonitorService.class);
        refresh.setAction(DoorMonitorService.ACTION_REFRESH_SERVICE_NOTIFICATION);
        startService(refresh);
    }

    private void callServiceWithFallback(String domain, String service, String entityId) throws Exception {
        SharedPreferences prefs = prefs();
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

    private String[] activeUrls(SharedPreferences prefs) {
        String mode = prefs.getString("connection_mode", "auto");
        String internal = prefs.getString("ha_internal_url", prefs.getString("ha_url", ""));
        String external = prefs.getString("ha_external_url", "");
        if ("external".equals(mode)) return external.isEmpty() ? new String[0] : new String[]{external};
        if ("internal".equals(mode)) return internal.isEmpty() ? new String[0] : new String[]{internal};
        String lastWorking = prefs.getString("ha_last_working_url", "");
        if (!lastWorking.isEmpty() && lastWorking.equals(external) && !internal.isEmpty()) return new String[]{external, internal};
        if (!lastWorking.isEmpty() && lastWorking.equals(internal) && !external.isEmpty()) return new String[]{internal, external};
        if (!internal.isEmpty() && !external.isEmpty()) return new String[]{internal, external};
        if (!internal.isEmpty()) return new String[]{internal};
        if (!external.isEmpty()) return new String[]{external};
        return new String[0];
    }

    private SharedPreferences prefs() {
        return getSharedPreferences("settings", MODE_PRIVATE);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextColor(TEXT);
        tv.setTextSize(size);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(18);
        button.setAllCaps(false);
        button.setMinHeight(dp(82));
        button.setBackground(round(color, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(12), 0, 0);
        button.setLayoutParams(lp);
        return button;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String t(SharedPreferences prefs, String nl, String en, String de) {
        String lang = prefs.getString("language", "nl");
        if ("de".equals(lang)) return de;
        if ("en".equals(lang)) return en;
        return nl;
    }
}
