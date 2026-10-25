package com.gathin.tvhome;

import android.graphics.Bitmap;

/** Separable Gaussian on a small background-only image; works on API 26 TVs as well as API 31+. */
final class BackdropBlur {
    static Bitmap gaussian(Bitmap source) {
        int w=Math.min(384,source.getWidth()),h=Math.max(1,Math.round(source.getHeight()*(w/(float)source.getWidth())));
        Bitmap small=Bitmap.createScaledBitmap(source,w,h,true);
        int[] input=new int[w*h],temp=new int[w*h],output=new int[w*h];small.getPixels(input,0,w,0,0,w,h);
        if(small!=source)small.recycle();
        int radius=12;float sigma=5f;float[] kernel=new float[radius*2+1];float sum=0;
        for(int i=-radius;i<=radius;i++){float v=(float)Math.exp(-(i*i)/(2*sigma*sigma));kernel[i+radius]=v;sum+=v;}
        for(int i=0;i<kernel.length;i++)kernel[i]/=sum;
        pass(input,temp,w,h,kernel,radius,true);pass(temp,output,w,h,kernel,radius,false);
        return Bitmap.createBitmap(output,w,h,Bitmap.Config.ARGB_8888);
    }
    private static void pass(int[] src,int[] dst,int w,int h,float[] kernel,int radius,boolean horizontal){
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            float r=0,g=0,b=0;
            for(int k=-radius;k<=radius;k++){
                int xx=horizontal?Math.max(0,Math.min(w-1,x+k)):x;
                int yy=horizontal?y:Math.max(0,Math.min(h-1,y+k));int c=src[yy*w+xx];float f=kernel[k+radius];
                r+=((c>>16)&255)*f;g+=((c>>8)&255)*f;b+=(c&255)*f;
            }
            dst[y*w+x]=0xff000000|(Math.round(r)<<16)|(Math.round(g)<<8)|Math.round(b);
        }
    }
    private BackdropBlur(){}
}
