package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Thin optical surface. Rest and focus share identical tint; there is no white focus fill. */
final class LiquidGlassDrawable extends Drawable {
    private final float density, radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private Bitmap resting, focused;
    private BitmapShader restShader, focusShader;
    private float focus, press;

    LiquidGlassDrawable(float density, float radiusDp) {
        this.density = density; radius = radiusDp * density;
        edge.setStyle(Paint.Style.STROKE);
    }
    void progress(float focus, float press) {
        this.focus = Math.max(0, Math.min(1, focus));
        this.press = Math.max(0, Math.min(1, press)); invalidateSelf();
    }
    void images(Bitmap rest, Bitmap active) {
        // Do not recycle bitmaps referenced by a hardware display list; let Android release them safely.
        resting = rest; focused = active;
        restShader = new BitmapShader(rest, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        focusShader = new BitmapShader(active, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        updateMatrices(); invalidateSelf();
    }
    void release() {
        resting = null; focused = null; restShader = null; focusShader = null; invalidateSelf();
    }
    @Override protected void onBoundsChange(Rect bounds) { updateMatrices(); }
    private void updateMatrices() {
        if (resting == null) return;
        Matrix matrix = new Matrix();
        matrix.setScale(getBounds().width() / (float) resting.getWidth(), getBounds().height() / (float) resting.getHeight());
        matrix.postTranslate(getBounds().left, getBounds().top);
        restShader.setLocalMatrix(matrix); focusShader.setLocalMatrix(matrix);
    }
    @Override public void draw(Canvas canvas) {
        rect.set(getBounds()); rect.inset(.6f * density, .6f * density);
        float r = Math.min(radius, rect.height() * .5f);
        paint.setAlpha(255);
        if (restShader != null) {
            paint.setShader(restShader); canvas.drawRoundRect(rect, r, r, paint);
            paint.setShader(focusShader); paint.setAlpha(Math.round(255 * focus));
            canvas.drawRoundRect(rect, r, r, paint);
        }
        paint.setShader(null); paint.setAlpha(255);
        // Constant dark tint in every interaction state. No white/grey overlay on focus or press.
        paint.setColor(restShader == null ? 0xB8151B23 : 0x7010161F);
        canvas.drawRoundRect(rect, r, r, paint);

        // Opposed, directional reflections along a hairline; most of the perimeter stays quiet.
        int strong = Math.round(38 + focus * 100 + press * 20);
        int weak = Math.round(8 + focus * 13);
        edge.setStrokeWidth((.55f + focus * .2f) * density);
        edge.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                new int[]{strong << 24 | 0xe7f3ff, weak << 24 | 0xc6d8e7,
                        0x08d4e5f0, Math.round(strong * .7f) << 24 | 0xdbe8f4},
                new float[]{0, .32f, .66f, 1}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, r, r, edge);
        edge.setShader(null);
    }
    @Override public void getOutline(Outline outline) { outline.setRoundRect(getBounds(), radius); }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
