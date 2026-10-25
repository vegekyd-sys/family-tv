package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import java.util.List;

/** Fixed family entries live in the same adapter as apps; this controller owns only routes/history. */
final class FamilyShelf {
    static final String PINNED_APP = "com.netease.filmlytv";
    static final int COUNT = 4;
    private final Activity host;
    private final FilmRoutes routes;
    private AlertDialog recentDialog;

    FamilyShelf(Activity host) { this.host=host;routes=new FilmRoutes(host); }
    static void appendEntries(Context context,List<AppAdapter.Entry> entries) {
        String[] labels={"NBA","宝可梦","海绵宝宝","网易爆米花"};
        String[] ids={"nba","pokemon","spongebob","filmly"};
        for(int i=0;i<COUNT;i++) entries.add(new AppAdapter.Entry(AppAdapter.Entry.FAMILY,labels[i],
                new ComponentName(context.getPackageName(),"family."+ids[i]),
                i==3?ArtCatalog.appBanner(context,PINNED_APP):ArtCatalog.banner(context,ids[i]),null,null,0));
    }
    static String routeId(AppAdapter.Entry entry) { return entry.component.getClassName().substring(7); }
    void open(String id) { if("filmly".equals(id))routes.app(PINNED_APP);else routes.open(id); }
    void showRecent(){
        String[] ids=routes.recent();String[] labels=new String[ids.length+1];labels[0]="极光观看历史";
        for(int i=0;i<ids.length;i++)labels[i+1]=routes.label(ids[i]);
        recentDialog=new AlertDialog.Builder(host).setTitle("最近打开")
            .setItems(labels,(dialog,index)->routes.open(index==0?"history":ids[index-1]))
            .setNegativeButton("返回",null).create();
        recentDialog.show();
    }
    void close(){if(recentDialog!=null)recentDialog.dismiss();}
}
