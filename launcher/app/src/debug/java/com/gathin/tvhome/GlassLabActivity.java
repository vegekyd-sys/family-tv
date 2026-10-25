package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An explicit component specimen, not a working settings screen. Never packaged in release builds. */
public final class GlassLabActivity extends Activity {
    private boolean closed;
    private FrameLayout root;
    private TextView result;
    private LiquidGlassAction[] actions;
    private int activated;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new FrameLayout(this); root.setClipChildren(false); root.setClipToPadding(false);
        FrameLayout content = new FrameLayout(this);
        ImageView scene = new ImageView(this); scene.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String key = getIntent().getStringExtra("backdrop");
        scene.setImageBitmap(ArtCatalog.backdrop(this, key == null ? "nba" : key, false));
        content.addView(scene, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this); shade.setBackgroundColor(0x52060910);
        content.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        TextView clock = text("02:30", 32, 0xBFFFFFFF);
        at(content, clock, 180, 50, 54, 40);
        at(content, text("10月26日 星期一", 14, 0x80FFFFFF), 220, 30, 54, 92);
        String[] ids = {"nba", "pokemon", "spongebob", "filmly"};
        String[] names = {"NBA", "宝可梦", "海绵宝宝", "网易爆米花"};
        for (int i = 0; i < 4; i++) {
            ImageView card = new ImageView(this); card.setScaleType(ImageView.ScaleType.CENTER_CROP);
            card.setImageDrawable(i == 3 ? ArtCatalog.appBanner(this, FamilyShelf.PINNED_APP) : ArtCatalog.banner(this, ids[i]));
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setColor(0xff111111); shape.setCornerRadius(dp(16)); card.setBackground(shape); card.setClipToOutline(true);
            card.setAlpha(.48f); at(content, card, 180, 101, 75 + i * 207, 366);
            TextView name = text(names[i], 16, 0x80FFFFFF); name.setGravity(Gravity.CENTER);
            at(content, name, 180, 30, 75 + i * 207, 474);
        }
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        at(root, text("设置", 23, 0xffffffff), 268, 34, 636, 112);
        boolean motion = !getIntent().getBooleanExtra("reduced_motion", false);
        actions = new LiquidGlassAction[]{
                new LiquidGlassAction(this, R.drawable.ic_settings, "系统设置", false, motion),
                new LiquidGlassAction(this, R.drawable.ic_home_settings_pill, "桌面设置", false, motion),
                new LiquidGlassAction(this, R.drawable.ic_swap_vert, "调整顺序", true, motion),
                new LiquidGlassAction(this, R.drawable.ic_apps, "内容卡片", true, motion)};
        for (int i = 0; i < actions.length; i++) {
            actions[i].setId(View.generateViewId()); final String name = actions[i].label.getText().toString();
            actions[i].setOnClickListener(v -> result.setText("确认键反馈：" + name + " · " + (++activated) + " 次（样板不打开设置）"));
        }
        at(root, actions[0], 268, 44, 636, 152);
        at(root, actions[1], 268, 44, 636, 206);
        at(root, actions[2], 128, 64, 636, 262);
        at(root, actions[3], 128, 64, 776, 262);
        actions[0].setNextFocusUpId(actions[0].getId()); actions[0].setNextFocusDownId(actions[1].getId());
        actions[1].setNextFocusUpId(actions[0].getId()); actions[1].setNextFocusDownId(actions[2].getId());
        actions[2].setNextFocusUpId(actions[1].getId()); actions[2].setNextFocusRightId(actions[3].getId());
        actions[3].setNextFocusUpId(actions[1].getId()); actions[3].setNextFocusLeftId(actions[2].getId());
        for (int i = 0; i < 2; i++) { actions[i].setNextFocusLeftId(actions[i].getId()); actions[i].setNextFocusRightId(actions[i].getId()); }
        for (int i = 2; i < 4; i++) actions[i].setNextFocusDownId(actions[i].getId());
        actions[2].setNextFocusLeftId(actions[2].getId()); actions[3].setNextFocusRightId(actions[3].getId());
        result = text("控件样板 · 方向键切换焦点，确定测试按压，返回退出", 11, 0xBFFFFFFF);
        at(root, result, 850, 24, 54, 513);
        setContentView(root); HomeMenu.immersive(root);
        root.post(() -> LiquidGlassBackdrop.capture(content, backdrop -> {
            if (closed) return;
            for (LiquidGlassAction action : actions) action.setBackdrop(backdrop);
            actions[0].requestFocus();
        }));
    }
    @Override protected void onDestroy() { closed = true; super.onDestroy(); }
    private void at(FrameLayout parent, View child, int w, int h, int x, int y) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(w), dp(h)); lp.leftMargin = dp(x); lp.topMargin = dp(y); parent.addView(child, lp);
    }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(UiFont.get(this, R.string.font_medium)); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
