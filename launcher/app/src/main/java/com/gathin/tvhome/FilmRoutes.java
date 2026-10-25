package com.gathin.tvhome;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.widget.Toast;
import java.util.*;

/** Exported Tencent OpenJump contract, traced in the official APK. No private account data. */
final class FilmRoutes {
    private final Activity host;
    FilmRoutes(Activity a){host=a;}
    static String uri(String id){
        switch(id){
            case "warriors":case "nba":return "tenvideo2://?action=3&channel_code=nba&channel_name=nba";
            case "variety":return "tenvideo2://?action=3&channel_code=variety&channel_name=variety";
            case "history":return "tenvideo2://?action=10";
            case "warriors_search":return "tenvideo2://?action=59&search_keyword=勇士";
            case "pokemon":return "tenvideo2://?action=59&search_keyword=宝可梦XY";
            case "spongebob":return "tenvideo2://?action=59&search_keyword=海绵宝宝";
            default:return null;
        }
    }
    String label(String id){switch(id){case "warriors":case "nba":return "NBA";case "warriors_search":return "金州勇士";case "pokemon":return "宝可梦";case "spongebob":return "海绵宝宝";case "variety":return "综艺";default:return "极光观看历史";}}
    void open(String id){
        String url=uri(id);if(url==null)return;
        Intent i=new Intent("com.tencent.qqlivetv.open",Uri.parse(url)).setPackage("com.ktcp.video");
        try{host.startActivity(i);if(!id.equals("history"))remember(id);}
        catch(ActivityNotFoundException|SecurityException e){Toast.makeText(host,"当前极光版本无法打开此入口，请在应用内查找",Toast.LENGTH_LONG).show();app("com.ktcp.video");}
    }
    void app(String pkg){Intent i=host.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=host.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){Toast.makeText(host,"当前设备未安装这个应用",Toast.LENGTH_SHORT).show();return;}try{host.startActivity(i);}catch(ActivityNotFoundException|SecurityException e){Toast.makeText(host,"暂时无法打开应用",Toast.LENGTH_SHORT).show();}}
    /** Native public home route: the installed Aurora implementation disables splash requests here. */
    static Intent preferredAppIntent(Context context, String pkg, Intent fallback) {
        if (!"com.ktcp.video".equals(pkg)) return fallback;
        Intent direct = new Intent("com.tencent.qqlivetv.open", Uri.parse("tenvideo2://?action=4"))
                .setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return context.getPackageManager().resolveActivity(direct, 0) == null ? fallback : direct;
    }
    String[] recent(){String s=host.getSharedPreferences("family_home",0).getString("recent","");return s.isEmpty()?new String[0]:s.split(",");}
    private void remember(String id){LinkedHashSet<String> ids=new LinkedHashSet<>();ids.add(id);ids.addAll(Arrays.asList(recent()));StringBuilder out=new StringBuilder();int n=0;for(String x:ids){if(n++>=8)break;if(out.length()>0)out.append(',');out.append(x);}host.getSharedPreferences("family_home",0).edit().putString("recent",out.toString()).apply();}
}
