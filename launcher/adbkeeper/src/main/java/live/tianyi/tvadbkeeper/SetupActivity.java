package live.tianyi.tvadbkeeper;

import android.app.Activity;
import android.app.job.JobScheduler;
import android.os.Bundle;

/** Shell-only explicit activation; no launcher entry and no persistent service. */
public final class SetupActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean enabled = !getIntent().getBooleanExtra("disable", false);
        Restore.prefs(this).edit().putBoolean("enabled", enabled).commit();
        if (enabled) Restore.schedule(this, "setup");
        else getSystemService(JobScheduler.class).cancelAll();
        finish();
    }
}
