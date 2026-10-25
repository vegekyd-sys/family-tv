package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/** One home-derived shell for all five top-bar destinations. */
final class HomeMenu {
    final LinearLayout panel, body;
    final PopupWindow popup;
    final TextView subtitle, footer;
    final int width, height;
    private final Activity host;
    private final boolean animations, reducedMotion;
    private LinearLayout rows;

    HomeMenu(Activity host, String title, String description, boolean animations,
             boolean reducedMotion, Runnable dismissed) {
        this.host = host;
        this.animations = animations;
        this.reducedMotion = reducedMotion;
        View decor = host.getWindow().getDecorView();
        width = Math.min(dp(800), Math.max(dp(200), decor.getWidth() - dp(64)));
        height = Math.min(dp(360), Math.max(dp(240), decor.getHeight() - dp(64)));
        panel = new LinearLayout(host);
        panel.setId(R.id.homeMenuPanel);
        panel.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable surface = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xEB15212D, 0xEB0B141E});
        surface.setCornerRadius(dp(20));
        surface.setStroke(dp(1), 0x28FFFFFF);
        panel.setBackground(surface);
        TextView heading = text(title, 22, Color.WHITE);
        heading.setId(R.id.homeMenuTitle);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(heading, new LinearLayout.LayoutParams(-1, dp(36)));
        subtitle = text(description, 13, 0xBBFFFFFF);
        subtitle.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(28)));
        body = new LinearLayout(host);
        body.setOrientation(LinearLayout.VERTICAL);
        panel.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        footer = text("上下选择 · 确定打开 · 返回关闭", 13, 0xBBFFFFFF);
        footer.setGravity(Gravity.BOTTOM);
        panel.addView(footer, new LinearLayout.LayoutParams(-1, dp(28)));
        popup = new PopupWindow(panel, width, height, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        popup.setOnDismissListener(() -> {
            panel.animate().cancel();
            GlassBackdrop.set(host, false);
            dismissed.run();
        });
    }

    LinearLayout rows() {
        if (rows != null) return rows;
        ScrollView scroll = new ScrollView(host);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        rows = new LinearLayout(host);
        rows.setId(R.id.rows);
        rows.setTag(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows, new ScrollView.LayoutParams(-1, -2));
        body.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        return rows;
    }

    View addRow(int icon, String label, String status, Runnable action) {
        LinearLayout parent = rows();
        View row = row(host, parent, icon, label, status, () -> { popup.dismiss(); action.run(); });
        parent.addView(row);
        return row;
    }

    static View row(Activity host, LinearLayout parent, int iconRes, String label,
                    String status, Runnable action) {
        float d = host.getResources().getDisplayMetrics().density;
        java.util.function.IntUnaryOperator dp = n -> Math.round(n * d);
        FrameLayout row = new FrameLayout(host);
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, dp.applyAsInt(58)));
        row.setFocusableInTouchMode(true);
        row.setClickable(true);
        row.setDefaultFocusHighlightEnabled(false);
        GlassView glass = new GlassView(host, null);
        glass.setRadius(dp.applyAsInt(16));
        glass.setVisibility(View.INVISIBLE);
        glass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(glass, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout line = new LinearLayout(host);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp.applyAsInt(22), 0, dp.applyAsInt(22), 0);
        row.addView(line, new FrameLayout.LayoutParams(-1, -1));
        ImageView icon = new ImageView(host);
        icon.setId(R.id.icon);
        if (iconRes != 0) icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        line.addView(icon, new LinearLayout.LayoutParams(dp.applyAsInt(22), dp.applyAsInt(22)));
        TextView title = new TextView(host);
        title.setId(R.id.title);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setTypeface(UiFont.get(host, R.string.font_medium));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
        nameParams.setMarginStart(dp.applyAsInt(14));
        line.addView(title, nameParams);
        TextView state = new TextView(host);
        state.setId(R.id.status);
        state.setText(status);
        state.setTextSize(13);
        state.setTextColor(0xBBFFFFFF);
        state.setTypeface(UiFont.get(host, R.string.font_body));
        state.setSingleLine(true);
        state.setMaxWidth(dp.applyAsInt(250));
        state.setEllipsize(TextUtils.TruncateAt.END);
        state.setPadding(dp.applyAsInt(12), 0, 0, 0);
        line.addView(state, new LinearLayout.LayoutParams(-2, -2));
        row.setOnFocusChangeListener((v, focus) -> glass.setVisibility(focus ? View.VISIBLE : View.INVISIBLE));
        row.setOnClickListener(v -> action.run());
        final boolean[] pressed = {false};
        row.setOnKeyListener((v, key, event) -> {
            if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getRepeatCount() == 0) pressed[0] = true;
                } else {
                    boolean activate = pressed[0] && !event.isCanceled();
                    pressed[0] = false;
                    if (activate) v.performClick();
                }
                return true;
            }
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
            if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                int step = key == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1;
                for (int i = parent.indexOfChild(v) + step; i >= 0 && i < parent.getChildCount(); i += step) {
                    View next = parent.getChildAt(i);
                    if (next.isFocusable()) { next.requestFocus(); break; }
                }
                return true;
            }
            return false;
        });
        return row;
    }

    void show() {
        GlassBackdrop.set(host, true);
        popup.showAtLocation(host.getWindow().getDecorView(), Gravity.CENTER, 0, 0);
        immersive(panel);
        panel.setAlpha(animations ? 0f : 1f);
        if (animations) panel.animate().alpha(1f).setDuration(reducedMotion ? Motion.REDUCED_MS : 180).start();
        if (rows != null) {
            for (int i = 0; i < rows.getChildCount(); i++) {
                if (rows.getChildAt(i).isFocusable()) { rows.getChildAt(i).requestFocus(); break; }
            }
        }
    }

    static void immersive(View view) {
        view.getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (Build.VERSION.SDK_INT >= 30 && view.getWindowInsetsController() != null) {
            WindowInsetsController controller = view.getWindowInsetsController();
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsets.Type.systemBars());
        }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(UiFont.get(host, R.string.font_medium));
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setIncludeFontPadding(false);
        return view;
    }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
