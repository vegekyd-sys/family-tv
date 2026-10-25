package com.gathin.tvhome;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Extends the installed icon's own edge colours; the original mark is never redrawn. */
final class OriginalIconArt {
    static Bitmap card(Drawable icon) {
        int iw = Math.max(1, icon.getIntrinsicWidth()), ih = Math.max(1, icon.getIntrinsicHeight());
        float scale = Math.min(1f, 768f / Math.max(iw, ih));
        Bitmap source = Bitmap.createBitmap(Math.max(1, Math.round(iw * scale)), Math.max(1, Math.round(ih * scale)), Bitmap.Config.ARGB_8888);
        icon.setBounds(0,0,source.getWidth(),source.getHeight()); icon.draw(new Canvas(source));
        int w=source.getWidth(),h=source.getHeight();
        int[] pixels=new int[w*h];source.getPixels(pixels,0,w,0,0,w,h);
        int top=source.getPixel(Math.min(2,w-1),Math.min(2,h-1));
        int bottom=source.getPixel(Math.min(2,w-1),Math.max(0,h-3));
        boolean transparent=Color.alpha(top)<200;
        // Rounded-square app artwork has transparent corners but opaque edges. Extend its
        // existing background instead of placing the whole square on another dark card.
        boolean roundedBackground=transparent
                && Color.alpha(pixels[(h/2)*w])>220 && Color.alpha(pixels[(h/2)*w+w-1])>220
                && Color.alpha(pixels[w/2])>220 && Color.alpha(pixels[(h-1)*w+w/2])>220;
        if(roundedBackground){
            transparent=false;top=pixels[w/2];bottom=pixels[(h-1)*w+w/2];
        }
        if(transparent){
            int[] edges={pixels[(h/2)*w],pixels[(h/2)*w+w-1],pixels[w/2],pixels[(h-1)*w+w/2]};
            int solid=0,reference=0;
            for(int c:edges)if(Color.alpha(c)>220){if(solid==0)reference=c;if(distance(c,reference)<12)solid++;}
            if(solid>=3){transparent=false;top=reference;bottom=reference;}
        }
        if(transparent){top=0xff141b26;bottom=0xff0b1019;}
        Rect bounds=new Rect(0,0,w,h);
        // Trim only empty alpha or a uniform edge. Gradient/illustrated icons keep their original extent.
        if(transparent || distance(top,bottom)<12){
            int l=w,t=h,r=-1,b=-1;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                int c=pixels[y*w+x];
                boolean mark=transparent?Color.alpha(c)>32:(Color.alpha(c)>32 && distance(c,top)>55);
                if(mark){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
            }
            if(r>l && b>t) bounds.set(Math.max(0,l-2),Math.max(0,t-2),Math.min(w,r+3),Math.min(h,b+3));
        }
        Bitmap result=Bitmap.createBitmap(768,432,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        paint.setShader(new LinearGradient(0,0,0,432,top,bottom,Shader.TileMode.CLAMP));canvas.drawRect(0,0,768,432,paint);paint.setShader(null);
        boolean fullBackground=!transparent && distance(top,bottom)>=12;
        float fit=Math.min(720f/bounds.width(),(fullBackground?432f:384f)/bounds.height());float tw=bounds.width()*fit,th=bounds.height()*fit;
        if(!transparent){
            for(int y=0;y<432;y++){
                int sy=Math.max(0,Math.min(h-1,Math.round(bounds.top+(y-(432-th)/2)/fit)));
                int edge=0;while(edge<w/2 && Color.alpha(pixels[sy*w+edge])<220)edge++;
                int c=pixels[sy*w+edge];if(Color.alpha(c)<200)c=top;
                paint.setColor(c);canvas.drawRect(0,y,768,y+1,paint);
            }
        }
        canvas.drawBitmap(source,bounds,new RectF((768-tw)/2,(432-th)/2,(768+tw)/2,(432+th)/2),paint);
        source.recycle();return result;
    }
    private static int distance(int a,int b){return Math.max(Math.abs(Color.red(a)-Color.red(b)),Math.max(Math.abs(Color.green(a)-Color.green(b)),Math.abs(Color.blue(a)-Color.blue(b))));}
    private OriginalIconArt(){}
}
