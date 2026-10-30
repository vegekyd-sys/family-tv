package com.gathin.tvhome;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;

/** Preloaded media feedback for the explicit Home gesture, bounded by its owner. */
final class StartupCue {
    private final Handler handler;
    private AudioTrack track;
    private int generation;
    StartupCue(Handler handler) {
        this.handler=handler;
        try {
            int rate=48000, count=rate*3/5;
            short[] pcm=new short[count];
            for(int n=0;n<count;n++) {
                double t=n/(double)rate;
                double envelope=Math.min(1,Math.min(t/.02,(count-n)/(rate*.04)));
                pcm[n]=(short)(32767*.09*envelope*(Math.sin(2*Math.PI*440*t)+Math.sin(2*Math.PI*480*t)));
            }
            track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(count*2).build();
            if(track.write(pcm,0,count)!=count)throw new IllegalStateException("incomplete cue buffer");
        } catch(RuntimeException error) {release();Log.w("VStartupCue","media preparation failed",error);}
    }
    boolean play() {
        if(track==null)return false;
        try {
            track.stop();track.reloadStaticData();track.play();
            final int token=++generation;
            handler.postDelayed(()->{if(token==generation&&track!=null)
                Log.i("VStartupCue","media frames="+track.getPlaybackHeadPosition()+" elapsed="+android.os.SystemClock.elapsedRealtime());},200);
            return true;
        } catch(RuntimeException error){Log.w("VStartupCue","media playback failed",error);return false;}
    }
    void stop(){generation++;if(track!=null)try{track.pause();track.flush();track.stop();}catch(IllegalStateException ignored){}}
    void release(){stop();if(track!=null){track.release();track=null;}}
}
