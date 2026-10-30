package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Home portrait: quiet at rest, a small focus lift and a slow breath on selection. */
public final class VIslandButton extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap face;
    private final BitmapShader portrait;
    private final Matrix matrix = new Matrix();
    private final boolean motion;
    private ValueAnimator focusAnimation, breathing;
    private float focusAmount, breath;
    private boolean launching;

    public VIslandButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultFocusHighlightEnabled(false);
        setFocusable(true); setClickable(true);
        setContentDescription("和 V 说话，按确定直接拨号");
        face = null; // Public distribution uses a generic V mark; personal portrait is excluded.
        portrait = null;
        Config config = Config.load(context);
        motion = config.animations && !config.reducedMotion;
        setOnFocusChangeListener((view, focused) -> {
            if (focusAnimation != null) focusAnimation.cancel();
            if (!motion || !ValueAnimator.areAnimatorsEnabled()) focusAmount = focused ? 1 : 0;
            else {
                focusAnimation = ValueAnimator.ofFloat(focusAmount, focused ? 1 : 0);
                focusAnimation.setDuration(focused ? 260 : 200);
                focusAnimation.setInterpolator(new PathInterpolator(.22f, .61f, .36f, 1));
                focusAnimation.addUpdateListener(a -> { focusAmount = (float)a.getAnimatedValue(); invalidate(); });
                focusAnimation.start();
            }
            context.sendBroadcast(new android.content.Intent("live.tianyi.vvoice.tv.QUERY_SURFACE").setPackage("live.tianyi.vvoice.tv.lab").putExtra("focused",focused));
            updateBreathing(); invalidate();
        });
    }
    // Immediate visual acknowledgement while the shared call page loads. The
    // final bounds match its 192dp portrait; no call state is invented here.
    public void setLaunching(boolean value, boolean animated) {
        launching=value;animate().cancel();
        float scale=value?3f:1f, x=value?dp(-64):0, y=value?dp(46):0;
        if(animated&&motion&&ValueAnimator.areAnimatorsEnabled())
            animate().scaleX(scale).scaleY(scale).translationX(x).translationY(y).setDuration(420)
                .setInterpolator(new PathInterpolator(.22f,.8f,.25f,1)).start();
        else {setScaleX(scale);setScaleY(scale);setTranslationX(x);setTranslationY(y);}
        updateBreathing();invalidate();
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private void updateBreathing() {
        boolean run = !launching && motion && ValueAnimator.areAnimatorsEnabled() && isFocused() && hasWindowFocus() && isShown() && getAlpha()>.01f;
        if (run && breathing == null) {
            breathing = ValueAnimator.ofFloat(0, 1);
            breathing.setDuration(1800); breathing.setRepeatCount(ValueAnimator.INFINITE); breathing.setRepeatMode(ValueAnimator.REVERSE);
            breathing.addUpdateListener(a -> { breath = (float)a.getAnimatedValue(); invalidate(); });
            breathing.start();
        } else if (!run && breathing != null) { breathing.cancel(); breathing = null; breath = 0; }
    }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); updateBreathing(); }
    @Override protected void onVisibilityChanged(View view, int visibility) { super.onVisibilityChanged(view, visibility); updateBreathing(); }
    @Override protected void onDetachedFromWindow() {
        if (focusAnimation != null) focusAnimation.cancel();
        if (breathing != null) { breathing.cancel(); breathing = null; }
        super.onDetachedFromWindow();
    }
    @Override public void setAlpha(float alpha){super.setAlpha(alpha);updateBreathing();}
    @Override public void setPressed(boolean pressed) { super.setPressed(pressed); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = dp(32) * (1 + (launching?0:focusAmount) * (.055f + .018f * breath) - (isPressed() ? .035f : 0));
        if (!launching && focusAmount > 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(cx, cy, radius + dp(9), new int[]{0x00BBDDEC, 0x32BBDDEC, 0x00BBDDEC}, new float[]{.72f, .84f, 1}, Shader.TileMode.CLAMP));
            paint.setAlpha(Math.round(255 * focusAmount * (.65f + breath * .35f)));
            canvas.drawCircle(cx, cy, radius + dp(9), paint);
        }
        // Match the shared H5 crop. Preserve the whole head with natural shoulder space.
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setAlpha(255);
        paint.setColor(0xFF173C4A);canvas.drawCircle(cx,cy,radius,paint);
        paint.setColor(0xFFE5F4FF);paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(radius);
        canvas.drawText("V",cx,cy-(paint.ascent()+paint.descent())/2,paint);
        paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(.65f + .35f * focusAmount));
        paint.setColor(isFocused() ? 0xDDE5F4FF : 0x70C6D8E6);
        canvas.drawCircle(cx, cy, radius + dp(.5f), paint);
    }
}
