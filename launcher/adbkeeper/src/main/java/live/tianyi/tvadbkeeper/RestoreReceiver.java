package live.tianyi.tvadbkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "mitv.action.STR_BOOT_COMPLETED".equals(action)
                || "live.tianyi.tvadbkeeper.RESTORE".equals(action)) {
            Restore.schedule(c, action);
        }
    }
}
