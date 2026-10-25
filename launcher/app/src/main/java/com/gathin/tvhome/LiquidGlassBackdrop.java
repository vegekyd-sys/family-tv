package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** One immutable backdrop shared by a group. Capture the content layer, never its glass descendants. */
final class LiquidGlassBackdrop {
    static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    final int[] sharp, soft;
    final int width, height, screenX, screenY;
    final float scale;

    private LiquidGlassBackdrop(Bitmap source, int screenX, int screenY, float scale) {
        width = source.getWidth(); height = source.getHeight();
        this.screenX = screenX; this.screenY = screenY; this.scale = scale;
        sharp = new int[width * height]; soft = new int[width * height];
        source.getPixels(sharp, 0, width, 0, 0, width, height);
        Bitmap blurred = BackdropBlur.gaussian(source);
        Bitmap full = Bitmap.createScaledBitmap(blurred, width, height, true);
        full.getPixels(soft, 0, width, 0, 0, width, height);
        if (full != blurred) full.recycle();
        blurred.recycle(); source.recycle();
    }

    /** Invoke on the UI thread after layout. Blur is done off-thread; callback returns on the UI thread. */
    static void capture(View content, Consumer<LiquidGlassBackdrop> ready) {
        if (content.getWidth() <= 0 || content.getHeight() <= 0) return;
        float scale = Math.min(1, 960f / content.getWidth());
        Bitmap image = Bitmap.createBitmap(Math.max(1, Math.round(content.getWidth() * scale)),
                Math.max(1, Math.round(content.getHeight() * scale)), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image); canvas.scale(scale, scale); content.draw(canvas);
        int[] position = new int[2]; content.getLocationOnScreen(position);
        WORKER.execute(() -> {
            LiquidGlassBackdrop result = new LiquidGlassBackdrop(image, position[0], position[1], scale);
            content.post(() -> ready.accept(result));
        });
    }
}
