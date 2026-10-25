package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/** Blurs only the home behind a separate popup window; no screenshots or bitmap allocations. */
final class GlassBackdrop {
    static void set(Activity activity, boolean visible) {
        if (Build.VERSION.SDK_INT < 31) return;
        View home = activity.findViewById(R.id.root);
        if (home == null || !home.isHardwareAccelerated()) return;
        float radius = 9f * home.getResources().getDisplayMetrics().density;
        home.setRenderEffect(visible ? RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP) : null);
    }
}
