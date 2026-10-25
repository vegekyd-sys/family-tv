package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Debounced full-resolution artwork. A superseded decode can never replace newer focus. */
final class FocusBackdrop {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final FrameLayout layer;
    private ImageView front, back;
    private final boolean motion;
    private String requested = "", displayed = "";
    private boolean gaussian;
    private volatile int generation;
    private volatile boolean closed;
    private Runnable pending;

    FocusBackdrop(Context context, FrameLayout root, boolean motion) {
        this.context = context.getApplicationContext(); this.motion = motion;
        layer = new FrameLayout(context); layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        back = image(context); front = image(context);
        layer.addView(back, new FrameLayout.LayoutParams(-1, -1));
        layer.addView(front, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(context);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x74050a13,0x18050a13,0x94050a13}));
        layer.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        root.addView(layer, root.indexOfChild(root.findViewById(R.id.sky)), new FrameLayout.LayoutParams(-1, -1));
    }
    private static ImageView image(Context context) {
        ImageView image = new ImageView(context); image.setScaleType(ImageView.ScaleType.CENTER_CROP); return image;
    }
    void show(String key) { show(key,false); }
    void setGaussian(boolean value){if(!requested.isEmpty())show(requested,value);}
    private void show(String key,boolean blur) {
        if (closed || (key.equals(requested) && gaussian==blur)) return;
        gaussian=blur;
        requested = key; int ticket = ++generation;
        if (pending != null) main.removeCallbacks(pending);
        pending = () -> {
            pending = null;
            decoder.execute(() -> {
                if (closed || ticket != generation || Thread.currentThread().isInterrupted()) return;
                Bitmap bitmap = ArtCatalog.backdrop(context, key, blur);
                main.post(() -> {
                    if (closed || ticket != generation) { bitmap.recycle(); return; }
                    front.animate().cancel(); back.animate().cancel();
                    // Keep the most recent incoming frame as the outgoing frame during rapid navigation.
                    ImageView previous = front; front = back; back = previous;
                    layer.removeView(front); layer.addView(front, 1);
                    back.setAlpha(1f); front.setImageBitmap(bitmap); front.setAlpha(motion ? 0f : 1f);
                    Runnable shown = () -> {
                        if (closed || ticket != generation) return;
                        back.setImageDrawable(null); displayed = key;
                        Log.i("FamilyBackdrop", "displayed=" + displayed + " mode=" + (blur ? "gaussian" : "clear"));
                    };
                    if (motion) front.animate().alpha(1f).setDuration(240).withEndAction(shown).start(); else shown.run();
                });
            });
        };
        main.postDelayed(pending, displayed.isEmpty() ? 0 : 100);
    }
    void close() {
        closed = true; generation++;
        if (pending != null) main.removeCallbacks(pending);
        decoder.shutdownNow(); front.animate().cancel(); back.animate().cancel();
        front.setImageDrawable(null); back.setImageDrawable(null);
    }
}
