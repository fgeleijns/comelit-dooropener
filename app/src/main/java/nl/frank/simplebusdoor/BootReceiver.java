package nl.frank.simplebusdoor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "";
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("door_notifications_enabled", false)) {
            try {
                context.startForegroundService(new Intent(context, DoorMonitorService.class));
            } catch (RuntimeException ignored) {
            }
        }
    }
}
