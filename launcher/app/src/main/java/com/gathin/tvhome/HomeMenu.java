package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Shared material and lifecycle, with content-sized layouts instead of a common panel rectangle. */
final class HomeMenu {
    enum Layout { SETTINGS, INPUTS, RECENT, NETWORK, LIBRARY }
    final LinearLayout panel, body;
    final PopupWindow popup;
    final TextView subtitle, footer;
    private final LinearLayout heading;
    private final Activity host;
    private final boolean animations, reducedMotion;
    private final Layout layout;
    private LinearLayout rows, tileRow;
    private ScrollView scroll;
    private int tileCount, columns, tileWidth, tileHeight, desiredWidth;
    private LiquidGlassBackdrop backdrop;
    private final View home;
    private Drawable homeForeground;
    private float homeGridAlpha;
    private int homeHintVisibility;
    private final List<View> quietButtons = new ArrayList<>();
    private final List<Drawable> buttonBackgrounds = new ArrayList<>();
    private final List<ColorStateList> buttonColors = new ArrayList<>(), buttonTints = new ArrayList<>();
    private boolean showing;
    private int captureTicket;

    HomeMenu(Activity host, String title, String description, Layout layout, boolean animations,
             boolean reducedMotion, Runnable dismissed) {
        this.host = host; this.layout = layout;
        this.animations = animations; this.reducedMotion = reducedMotion;
        home = host.findViewById(R.id.root);
        desiredWidth = layout == Layout.LIBRARY ? 672 : 294;
        columns = layout == Layout.INPUTS ? 4 : layout == Layout.RECENT ? 4 : 2;
        tileWidth = layout == Layout.INPUTS ? 132 : layout == Layout.RECENT ? 148 : layout == Layout.SETTINGS ? 128 : 132;
        tileHeight = layout == Layout.INPUTS ? 96 : layout == Layout.RECENT ? 122 : layout == Layout.SETTINGS ? 68 : 80;
        panel = new LinearLayout(host); panel.setId(R.id.homeMenuPanel);
        panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(10), dp(8), dp(10), dp(10));
        panel.setClipChildren(false); panel.setClipToPadding(false);
        heading = new LinearLayout(host); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 23, Color.WHITE); titleView.setId(R.id.homeMenuTitle);
        heading.addView(titleView, new LinearLayout.LayoutParams(0, dp(38), 1));
        panel.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        subtitle = text(description, 12, 0xBFFFFFFF);
        subtitle.setPadding(0, dp(2), 0, dp(9));
        panel.addView(subtitle, new LinearLayout.LayoutParams(-1, -2));
        body = new LinearLayout(host); body.setOrientation(LinearLayout.VERTICAL);
        body.setClipChildren(false); body.setClipToPadding(false);
        panel.addView(body, new LinearLayout.LayoutParams(-1, -2));
        footer = text("", 11, 0xA8FFFFFF); footer.setPadding(0, dp(10), 0, 0);
        panel.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        popup = new PopupWindow(panel, dp(desiredWidth), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0); popup.setElevation(0f);
        popup.setOnDismissListener(() -> {
            showing = false; ++captureTicket; backdrop = null;
            panel.animate().cancel();
            if (home != null) {
                home.setForeground(homeForeground);
                if (host.findViewById(R.id.homeBrowseHint) != null) host.findViewById(R.id.homeBrowseHint).setVisibility(homeHintVisibility);
                if (layout == Layout.LIBRARY && host.findViewById(R.id.grid) != null)
                    host.findViewById(R.id.grid).setAlpha(homeGridAlpha);
            }
            for (int i = 0; i < quietButtons.size(); i++) {
                View button = quietButtons.get(i); button.setBackground(buttonBackgrounds.get(i));
                if (button instanceof TextView) {
                    ((TextView) button).setTextColor(buttonColors.get(i));
                    ((TextView) button).setCompoundDrawableTintList(buttonTints.get(i));
                }
            }
            quietButtons.clear(); buttonBackgrounds.clear(); buttonColors.clear(); buttonTints.clear();
            dismissed.run();
        });
    }

    LinearLayout rows() {
        if (rows != null) return rows;
        scroll = new ScrollView(host); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        scroll.setFocusable(false); scroll.setVerticalScrollBarEnabled(false); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows = new LinearLayout(host); rows.setId(R.id.rows); rows.setTag(this);
        rows.setOrientation(LinearLayout.VERTICAL); rows.setClipChildren(false); rows.setClipToPadding(false);
        rows.setPadding(dp(3), dp(5), dp(3), dp(5));
        scroll.addView(rows, new ScrollView.LayoutParams(-1, -2));
        body.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        scroll.getViewTreeObserver().addOnScrollChangedListener(this::refreshGlass);
        return rows;
    }

    View addRow(int icon, String label, String status, Runnable action) {
        if (layout == Layout.INPUTS || layout == Layout.NETWORK) return addTile(icon, label, status, null, action);
        LiquidGlassAction row = action(icon, label, false, action);
        row.setStatus(status); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.bottomMargin = dp(10); rows().addView(row, lp); return row;
    }

    View addTile(int icon, String label, String status, Drawable artwork, Runnable action) {
        rows();
        if (tileCount % columns == 0) {
            tileRow = new LinearLayout(host); tileRow.setClipChildren(false); tileRow.setClipToPadding(false);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.bottomMargin = dp(10);
            rows.addView(tileRow, rp);
        }
        LiquidGlassAction tile = action(icon, label, true, action); tile.setStatus(status);
        if (artwork != null) tile.artwork(artwork, tileWidth - 20, (tileWidth - 20) * 9 / 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(tileWidth), dp(tileHeight));
        if (tileCount % columns != 0) lp.leftMargin = dp(12);
        tileRow.addView(tile, lp); tileCount++;
        if (layout == Layout.INPUTS || layout == Layout.RECENT || layout == Layout.NETWORK)
            desiredWidth = Math.max(layout == Layout.RECENT ? 320 : 158,
                    Math.min(columns, tileCount) * (tileWidth + 12) - 12 + 26);
        return tile;
    }

    void headerAction(int icon, String label, Runnable callback) {
        LiquidGlassAction button = action(icon, label, false, callback);
        button.label.setTextSize(13);
        heading.addView(button, new LinearLayout.LayoutParams(dp(180), dp(36)));
    }

    void empty(String message) {
        TextView label = text(message, 14, 0xBBFFFFFF); label.setPadding(dp(3), dp(18), 0, dp(20));
        body.addView(label, new LinearLayout.LayoutParams(-1, -2));
    }

    private LiquidGlassAction action(int icon, String label, boolean vertical, Runnable callback) {
        LiquidGlassAction action = new LiquidGlassAction(host, icon, label, vertical, animations && !reducedMotion);
        action.setId(View.generateViewId()); action.setOnClickListener(v -> { popup.dismiss(); callback.run(); });
        constrain(action, panel); return action;
    }

    static void constrain(View action, ViewGroup boundary) {
        action.setOnKeyListener((v, key, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            int direction;
            if (key == KeyEvent.KEYCODE_DPAD_UP) direction = View.FOCUS_UP;
            else if (key == KeyEvent.KEYCODE_DPAD_DOWN) direction = View.FOCUS_DOWN;
            else if (key == KeyEvent.KEYCODE_DPAD_LEFT) direction = View.FOCUS_LEFT;
            else if (key == KeyEvent.KEYCODE_DPAD_RIGHT) direction = View.FOCUS_RIGHT;
            else return false;
            View next = v.focusSearch(direction);
            if (next == null) return true;
            for (android.view.ViewParent parent = next.getParent(); parent != null; parent = parent.getParent())
                if (parent == boundary) return false;
            return true;
        });
    }

    /** Compact contextual action also shares the optical component. */
    static View row(Activity host, LinearLayout parent, int icon, String label, String status, Runnable action) {
        LiquidGlassAction row = new LiquidGlassAction(host, icon, label, false, false);
        row.setStatus(status); row.setLayoutParams(new LinearLayout.LayoutParams(-1, Math.round(48 * host.getResources().getDisplayMetrics().density)));
        row.setOnClickListener(v -> action.run()); constrain(row, parent); return row;
    }

    void show() {
        View decor = host.getWindow().getDecorView();
        subtitle.setVisibility(TextUtils.isEmpty(subtitle.getText()) ? View.GONE : View.VISIBLE);
        footer.setVisibility(TextUtils.isEmpty(footer.getText()) ? View.GONE : View.VISIBLE);
        int width = Math.min(dp(desiredWidth), decor.getWidth() - dp(72));
        popup.setWidth(width);
        int top = dp(layout == Layout.LIBRARY ? 108 : 112), maxHeight = Math.max(dp(160), decor.getHeight() - top - dp(28));
        panel.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (panel.getMeasuredHeight() > maxHeight && scroll != null) {
            scroll.getLayoutParams().height = Math.max(dp(90), maxHeight - (panel.getMeasuredHeight() - scroll.getMeasuredHeight()));
        }
        showing = true;
        if (home != null) {
            homeForeground = home.getForeground(); home.setForeground(new ColorDrawable(0x40050910));
            View hint = host.findViewById(R.id.homeBrowseHint);
            if (hint != null) { homeHintVisibility = hint.getVisibility(); hint.setVisibility(View.INVISIBLE); }
            if (layout == Layout.LIBRARY && host.findViewById(R.id.grid) != null) {
                View grid = host.findViewById(R.id.grid); homeGridAlpha = grid.getAlpha(); grid.setAlpha(.12f);
            }
            ViewGroup buttons = host.findViewById(R.id.topButtons);
            if (buttons != null) for (int i = 0; i < buttons.getChildCount(); i++) {
                View button = buttons.getChildAt(i); quietButtons.add(button); buttonBackgrounds.add(button.getBackground()); button.setBackground(null);
                TextView label = button instanceof TextView ? (TextView) button : null;
                buttonColors.add(label == null ? null : label.getTextColors());
                buttonTints.add(label == null ? null : label.getCompoundDrawableTintList());
                if (label != null) { label.setTextColor(0xBFFFFFFF); label.setCompoundDrawableTintList(ColorStateList.valueOf(0xBFFFFFFF)); }
            }
        }
        popup.showAtLocation(decor, Gravity.TOP | Gravity.RIGHT, dp(46), top); immersive(panel);
        panel.setAlpha(animations && !reducedMotion ? 0 : 1);
        if (animations && !reducedMotion) panel.animate().alpha(1).setDuration(180).start();
        panel.post(() -> {
            if (!showing) return;
            View first = firstAction(body);
            if (first == null) first = firstAction(heading);
            if (first != null) first.requestFocus();
            capture();
        });
        // The top-bar backdrop may still be completing its existing blur transition.
        panel.postDelayed(() -> { if (showing) capture(); }, 360);
    }

    private View firstAction(View view) {
        if (view instanceof LiquidGlassAction) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = firstAction(group.getChildAt(i)); if (found != null) return found;
            }
        }
        return null;
    }

    private void capture() {
        if (home == null) return;
        int ticket = ++captureTicket;
        LiquidGlassBackdrop.capture(home, value -> {
            if (!showing || ticket != captureTicket) return;
            backdrop = value; refreshGlass();
        });
    }
    void refreshGlass() { if (backdrop != null && showing) bind(panel); }
    private void bind(View view) {
        if (view instanceof LiquidGlassAction) ((LiquidGlassAction) view).setBackdrop(backdrop);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) bind(group.getChildAt(i));
        }
    }
    static void immersive(View view) {
        view.getRootView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (Build.VERSION.SDK_INT >= 30 && view.getWindowInsetsController() != null) {
            WindowInsetsController controller = view.getWindowInsetsController();
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsets.Type.systemBars());
        }
    }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(host); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(UiFont.get(host, R.string.font_medium)); v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END);
        v.setIncludeFontPadding(false); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
