package live.tianyi.tvadbkeeper;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

final class Restore {
    static final String TAG = "FamilyAdbKeeper";
    static android.content.SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences("maintenance", Context.MODE_PRIVATE);
    }
    static void apply(Context c, String reason) {
        if (!prefs(c).getBoolean("enabled", false)) return;
        if (c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, reason + ": permission missing; no change");
            return;
        }
        try {
            int before = Settings.Global.getInt(c.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
            boolean written = before == 1 || Settings.Global.putInt(c.getContentResolver(), Settings.Global.ADB_ENABLED, 1);
            Log.i(TAG, reason + ": before=" + before + " enabled=" + written);
        } catch (SecurityException e) {
            Log.w(TAG, reason + ": setting denied", e);
        }
    }
    static void schedule(Context c, String reason) {
        if (!prefs(c).getBoolean("enabled", false)) return;
        apply(c, reason);
        JobScheduler jobs = c.getSystemService(JobScheduler.class);
        // Finite retries tolerate the OEM receiver resetting adb_enabled after ours.
        long[] delays = {2000, 12000, 45000, 90000};
        for (int i = 0; i < delays.length; i++) {
            long delay = delays[i];
            int result = jobs.schedule(new JobInfo.Builder(4310 + i, new ComponentName(c, RestoreJob.class))
                    .setMinimumLatency(delay).setOverrideDeadline(delay + 8000).build());
            Log.i(TAG, reason + ": job=" + (4310+i) + " scheduled=" + result);
        }
    }
}
