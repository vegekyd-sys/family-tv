package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewConfiguration;
import android.text.TextUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Reusable TV action: focus, press and activation are independent states. */
final class LiquidGlassAction extends FrameLayout {
    private final LiquidGlassDrawable material;
    private final float density;
    private final boolean motion;
    private float focus;
    private boolean armed, longPressed;
    private LiquidGlassBackdrop backdrop;
    private int sampledX = Integer.MIN_VALUE, sampledY, sampledW, sampledH;
    private final Runnable hold = () -> {
        if (armed && isLongClickable() && isFocused()) longPressed = performLongClick();
    };
    private volatile boolean disposed;
    private volatile int renderTicket;
    private ValueAnimator focusAnimation;
    final TextView label;
    final TextView status;
    final ImageView icon;
    final LinearLayout content;

    LiquidGlassAction(Context context, int iconId, String title, boolean vertical, boolean motion) {
        super(context);
        this.motion = motion; density = getResources().getDisplayMetrics().density;
        material = new LiquidGlassDrawable(density, vertical ? 18 : 22);
        setBackground(material); setOutlineProvider(ViewOutlineProvider.BACKGROUND);
        setElevation(dp(3)); setFocusable(true); setFocusableInTouchMode(true); setClickable(true);
        setDefaultFocusHighlightEnabled(false); setContentDescription(title);
        content = new LinearLayout(context);
        content.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        content.setGravity(vertical ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        content.setPadding(dp(18), dp(4), dp(18), dp(4));
        content.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        icon = new ImageView(context); icon.setId(R.id.icon);
        if (iconId != 0) icon.setImageResource(iconId);
        icon.setColorFilter(Color.WHITE);
        content.addView(icon, new LinearLayout.LayoutParams(dp(21), dp(21)));
        label = new TextView(context); label.setId(R.id.title); label.setText(title); label.setTextColor(Color.WHITE);
        label.setTextSize(vertical ? 14 : 17); label.setTypeface(UiFont.get(context, R.string.font_medium));
        label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END); label.setIncludeFontPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(vertical ? -2 : 0, -2, vertical ? 0 : 1);
        if (vertical) lp.topMargin = dp(7); else lp.leftMargin = dp(13);
        content.addView(label, lp);
        status = new TextView(context); status.setId(R.id.status); status.setTextColor(0xBBFFFFFF);
        status.setTextSize(11); status.setSingleLine(true); status.setEllipsize(TextUtils.TruncateAt.END);
        status.setTypeface(UiFont.get(context, R.string.font_body)); status.setVisibility(GONE);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(vertical ? -1 : -2, -2);
        if (vertical) { sp.topMargin = dp(5); status.setGravity(Gravity.CENTER); }
        else { sp.leftMargin = dp(8); status.setMaxWidth(dp(100)); }
        content.addView(status, sp);
        addView(content, new FrameLayout.LayoutParams(-1, -1));
    }

    void setStatus(String value) {
        status.setText(value); status.setVisibility(value == null || value.isEmpty() ? GONE : VISIBLE);
        setContentDescription(label.getText() + (value == null || value.isEmpty() ? "" : " · " + value));
    }

    /** The same optical action can carry real artwork without changing focus or press behaviour. */
    void artwork(android.graphics.drawable.Drawable image, int width, int height) {
        icon.clearColorFilter(); icon.setImageDrawable(image); icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(width), dp(height)));
        android.graphics.drawable.GradientDrawable clip = new android.graphics.drawable.GradientDrawable();
        clip.setColor(0xff151b23); clip.setCornerRadius(dp(9)); icon.setBackground(clip); icon.setClipToOutline(true);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
    }

    void setBackdrop(LiquidGlassBackdrop backdrop) {
        if (getWidth() == 0 || disposed) return;
        int[] location = new int[2]; getLocationOnScreen(location);
        // Sample the nominal unscaled rectangle, not the focus animation's transformed origin.
        location[0] += Math.round(getWidth() * (getScaleX() - 1) / 2);
        location[1] += Math.round(getHeight() * (getScaleY() - 1) / 2);
        if (this.backdrop == backdrop && sampledX == location[0] && sampledY == location[1]
                && sampledW == getWidth() && sampledH == getHeight()) return;
        this.backdrop = backdrop; sampledX = location[0]; sampledY = location[1];
        sampledW = getWidth(); sampledH = getHeight();
        int ticket = ++renderTicket;
        int w = Math.max(1, Math.round(getWidth() * backdrop.scale));
        int h = Math.max(1, Math.round(getHeight() * backdrop.scale));
        float left = (location[0] - backdrop.screenX) * backdrop.scale;
        float top = (location[1] - backdrop.screenY) * backdrop.scale;
        float unit = density * backdrop.scale;
        LiquidGlassBackdrop.WORKER.execute(() -> {
            if (disposed || ticket != renderTicket) return;
            int[] rest = GlassLens.render(backdrop.sharp, backdrop.soft, backdrop.width, backdrop.height,
                    w, h, left, top, 1, 20 * unit, 5 * unit, 1.1f * unit);
            int[] active = GlassLens.render(backdrop.sharp, backdrop.soft, backdrop.width, backdrop.height,
                    w, h, left, top, 1, 20 * unit, 8 * unit, 4.0f * unit);
            Bitmap a = Bitmap.createBitmap(rest, w, h, Bitmap.Config.ARGB_8888);
            Bitmap b = Bitmap.createBitmap(active, w, h, Bitmap.Config.ARGB_8888);
            post(() -> {
                if (disposed || ticket != renderTicket) { a.recycle(); b.recycle(); return; }
                material.images(a, b);
            });
        });
    }

    @Override protected void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        animateFocus(hasFocus && hasWindowFocus());
    }
    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        animateFocus(hasWindowFocus && hasFocus());
    }
    private void animateFocus(boolean hasFocus) {
        if (!hasFocus) { armed = false; removeCallbacks(hold); setPressed(false); }
        if (focusAnimation != null) focusAnimation.cancel();
        if (!motion) { focus = hasFocus ? 1 : 0; updateMaterial(); return; }
        focusAnimation = ValueAnimator.ofFloat(focus, hasFocus ? 1 : 0);
        focusAnimation.setDuration(180); focusAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
        focusAnimation.addUpdateListener(a -> { focus = (float) a.getAnimatedValue(); updateMaterial(); });
        focusAnimation.start();
    }
    private void updateMaterial() {
        boolean pressed = isPressed();
        float scale = 1 + focus * .025f - (pressed ? .025f : 0);
        setScaleX(scale); setScaleY(scale); setTranslationZ(dp(5) * focus);
        material.progress(focus, pressed ? 1 : 0);
    }
    @Override public void setPressed(boolean pressed) { super.setPressed(pressed); if (material != null) updateMaterial(); }
    @Override public boolean onTouchEvent(android.view.MotionEvent event) {
        // Focus on down so View's up handler performs the click instead of consuming the first tap
        // solely to take focus. D-pad activation still requires its own armed key-down.
        if (event.getActionMasked() == android.view.MotionEvent.ACTION_DOWN && isEnabled()) requestFocus();
        return super.onTouchEvent(event);
    }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            if (event.getRepeatCount() == 0) {
                armed = true; longPressed = false; setPressed(true);
                if (isLongClickable()) postDelayed(hold, ViewConfiguration.getLongPressTimeout());
            } else if (event.isLongPress() && armed && !longPressed && isLongClickable()) {
                removeCallbacks(hold); longPressed = performLongClick();
            }
            return true;
        }
        return super.onKeyDown(code, event);
    }
    @Override public boolean onKeyUp(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
            boolean activate = armed && !longPressed && !event.isCanceled(); armed = false; removeCallbacks(hold); setPressed(false);
            if (activate) performClick(); return true;
        }
        return super.onKeyUp(code, event);
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); disposed = false; }
    @Override protected void onDetachedFromWindow() {
        disposed = true; ++renderTicket; removeCallbacks(hold); armed = false; backdrop = null; sampledX = Integer.MIN_VALUE;
        if (focusAnimation != null) focusAnimation.cancel(); material.release();
        super.onDetachedFromWindow();
    }
    private int dp(float v) { return Math.round(v * density); }
}
