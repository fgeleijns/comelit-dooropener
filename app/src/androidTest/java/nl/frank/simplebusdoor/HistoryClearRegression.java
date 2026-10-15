package nl.frank.simplebusdoor;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.Map;

/** On-device regression: clearing history must retain settings and foreground monitoring. */
public class HistoryClearRegression extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            SharedPreferences prefs = getTargetContext().getSharedPreferences("settings", 0);
            Map<String, ?> before = new HashMap<>(prefs.getAll());
            if (!prefs.getBoolean("door_notifications_enabled", false)) throw new AssertionError("Monitoring must be enabled for regression");
            Activity activity = startActivitySync(new Intent().setClassName(getTargetContext(), "nl.frank.simplebusdoor.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            for (int attempt=0; attempt<2; attempt++) {
                runOnMainSync(() -> {
                    try {
                        java.lang.reflect.Method clear = activity.getClass().getDeclaredMethod("clearDoorbellHistory");
                        clear.setAccessible(true);
                        clear.invoke(activity);
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
                // Longer than Android's foreground-service start acknowledgement deadline.
                SystemClock.sleep(15000);
                if (!prefs.getString("doorbell_history", "").isEmpty() || prefs.getInt("doorbell_ring_count", -1) != 0) throw new AssertionError("History not cleared");
                boolean foreground = false;
                for (android.service.notification.StatusBarNotification n : getTargetContext().getSystemService(NotificationManager.class).getActiveNotifications()) {
                    if (n.getId()==10 && (n.getNotification().flags & android.app.Notification.FLAG_FOREGROUND_SERVICE)!=0) foreground=true;
                }
                if (!foreground) throw new AssertionError("Foreground monitor disappeared");
            }
            for (String key : new String[]{"ha_token", "ha_internal_url", "ha_external_url", "bridge_url", "settings_pin", "door_notifications_enabled", "auto_open_mode"}) {
                if (!java.util.Objects.equals(before.get(key), prefs.getAll().get(key))) throw new AssertionError("Setting changed: " + key);
            }
            result.putString("result", "PASS: cleared twice; app and foreground monitor alive after 30 seconds; connection, PIN and monitoring settings preserved");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable e) {
            result.putString("error", e.toString());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
