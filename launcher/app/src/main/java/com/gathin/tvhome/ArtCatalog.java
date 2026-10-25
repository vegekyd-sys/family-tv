package com.gathin.tvhome;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import java.io.InputStream;
import java.io.IOException;

/** Original icon cards and independent scene backgrounds have separate asset paths and caches. */
final class ArtCatalog {
    private static final LruCache<String, Bitmap> thumbs = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    static String keyForPackage(String pkg) {
        if (pkg == null) return "aurora";
        switch (pkg) {
            case "com.ktcp.video": return "aurora";
            case "com.netease.filmlytv": return "filmly";
            case "com.xiaodianshi.tv.yst": return "bili";
            case "com.tencent.qqmusictv": return "music";
            case "org.smarttube.stable": case "com.google.android.youtube.tv": case "live.tianyi.youtube.tv": return "smarttube";
            case "live.tianyi.vvoice.tv": return "voice";
            case "com.android.chrome": case "com.phlox.tvwebbrowser": return "chrome";
            case "com.r3studio.monoproxy": return "cloud";
            default: return "app:" + pkg;
        }
    }
    static Drawable appBanner(Context context, String pkg) {
        String id="icon:"+pkg;Bitmap bitmap;
        synchronized(thumbs){bitmap=thumbs.get(id);if(bitmap==null){
            Drawable icon;
            try{icon=context.getPackageManager().getApplicationIcon(pkg);}
            catch(android.content.pm.PackageManager.NameNotFoundException missing){icon=context.getDrawable(R.drawable.ic_apps);}
            if(pkg.equals(context.getPackageName()))icon=context.getDrawable(R.drawable.ic_home_settings);
            bitmap=OriginalIconArt.card(icon);thumbs.put(id,bitmap);
        }}
        return new BitmapDrawable(context.getResources(),bitmap);
    }
    static Drawable banner(Context context, String key) {
        Bitmap bitmap;
        synchronized (thumbs) {
            bitmap = thumbs.get(key);
            if (bitmap == null) { if(key.equals("nba")){
                    try(InputStream in=context.getAssets().open("card/nba.png")){bitmap=OriginalIconArt.card(Drawable.createFromStream(in,"nba"));}
                    catch(IOException error){bitmap=decode(context,key,true);}
                }else bitmap = decode(context, key, true); thumbs.put(key, bitmap); }
        }
        return new BitmapDrawable(context.getResources(), bitmap);
    }
    static Bitmap backdrop(Context context,String key,boolean gaussian){
        Bitmap bitmap=null;
        try(InputStream in=context.getAssets().open("backdrops/"+key+".jpg")){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=gaussian?4:1;
            bitmap=BitmapFactory.decodeStream(in,null,options);
        }catch(IOException ignored){}
        if(bitmap==null){
            bitmap=Bitmap.createBitmap(gaussian?384:1920,gaussian?216:1080,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(bitmap);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            float hue=(key.hashCode()&0xffff)%360;
            paint.setShader(new RadialGradient(bitmap.getWidth()*.5f,bitmap.getHeight()*.18f,bitmap.getWidth()*.55f,
                    Color.HSVToColor(new float[]{hue,.55f,.27f}),0xff080c15,Shader.TileMode.CLAMP));
            canvas.drawRect(0,0,bitmap.getWidth(),bitmap.getHeight(),paint);
        }
        if(!gaussian)return bitmap;
        Bitmap blurred=BackdropBlur.gaussian(bitmap);bitmap.recycle();return blurred;
    }
    static Bitmap decode(Context context, String key, boolean thumbnail) {
        if (!key.startsWith("app:")) {
            try (InputStream in = context.getAssets().open("art/" + key + ".jpg")) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = thumbnail ? (key.equals("spongebob") ? 8 : 4) : (key.equals("spongebob") ? 2 : 1);
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
                if (bitmap != null) return bitmap;
            } catch (IOException ignored) { }
        }
        // New/unmapped applications still get a full-size card with a large recognizable glyph.
        int w = thumbnail ? 512 : 960, h = w * 9 / 16;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0, 0, w, h, 0xff243d57, 0xff101522, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        Drawable icon = null;
        try { if (key.startsWith("app:")) icon = context.getPackageManager().getApplicationIcon(key.substring(4)); }
        catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
        if (icon == null || key.equals("app:" + context.getPackageName())) icon = context.getDrawable(R.drawable.ic_home_settings);
        if (icon != null) { int side = h * 3 / 4; icon.setBounds((w-side)/2,(h-side)/2,(w+side)/2,(h+side)/2); icon.draw(canvas); }
        return bitmap;
    }
    private ArtCatalog() { }
}
