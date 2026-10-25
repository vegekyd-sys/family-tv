package live.tianyi.tvadbkeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Signature-protected wake request; it never enables maintenance or accepts settings/extras. */
public final class HomeReadyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if ("live.tianyi.tvadbkeeper.HOME_READY".equals(intent.getAction())) {
            Restore.schedule(context, "home-ready");
        }
    }
}
