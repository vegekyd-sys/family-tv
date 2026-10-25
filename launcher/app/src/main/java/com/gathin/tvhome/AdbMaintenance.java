package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

/** Wake the separately opted-in keeper after OEM boot policy stops third-party apps. */
final class AdbMaintenance {
    private static final String PACKAGE = "live.tianyi.tvadbkeeper";

    static void homeReady(Context context) {
        if (context.getPackageManager().checkSignatures(context.getPackageName(), PACKAGE)
                != PackageManager.SIGNATURE_MATCH) return;
        try {
            context.sendBroadcast(new Intent(PACKAGE + ".HOME_READY")
                    .setComponent(new ComponentName(PACKAGE, PACKAGE + ".HomeReadyReceiver"))
                    .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES));
        } catch (SecurityException e) {
            Log.w("FamilyAdbKeeper", "Companion wake denied; Home continues normally");
        }
    }
}
