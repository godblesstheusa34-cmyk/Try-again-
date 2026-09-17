package com.chris.fluidhome;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppCatalog {
    public static final class Entry {
        public final String key,label;public final ComponentName component;public final UserHandle user;public final Drawable icon;
        Entry(String k,String l,ComponentName c,UserHandle u,Drawable i){key=k;label=l;component=c;user=u;icon=i;}
    }
    public interface Listener{void loaded(List<Entry> apps);}
    public final LauncherApps launcher;
    private final Context context;private final UserManager users;private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private int generation;private boolean closed;
    private final LauncherApps.Callback callback=new LauncherApps.Callback(){
        public void onPackageRemoved(String p,UserHandle u){refresh();}
        public void onPackageAdded(String p,UserHandle u){refresh();}
        public void onPackageChanged(String p,UserHandle u){refresh();}
        public void onPackagesAvailable(String[] p,UserHandle u,boolean r){refresh();}
        public void onPackagesUnavailable(String[] p,UserHandle u,boolean r){refresh();}
        public void onPackagesSuspended(String[] p,UserHandle u){refresh();}
        public void onPackagesUnsuspended(String[] p,UserHandle u){refresh();}
    };
    public AppCatalog(Context c,Listener l){context=c.getApplicationContext();listener=l;launcher=c.getSystemService(LauncherApps.class);users=c.getSystemService(UserManager.class);launcher.registerCallback(callback,main);}
    public void refresh(){
        if(closed)return;int request=++generation;
        worker.execute(()->{
            ArrayList<Entry> result=new ArrayList<>();
            for(UserHandle user:users.getUserProfiles())try{
                for(LauncherActivityInfo info:launcher.getActivityList(null,user)){
                    if(info.getComponentName().getPackageName().equals(context.getPackageName()))continue;
                    String label=info.getLabel().toString();if(label.trim().isEmpty())label=info.getComponentName().getPackageName();
                    if(!user.equals(android.os.Process.myUserHandle()))label+=" · Profile";
                    String key=users.getSerialNumberForUser(user)+"|"+info.getComponentName().flattenToString();Drawable icon;
                    try{icon=info.getBadgedIcon(context.getResources().getDisplayMetrics().densityDpi);}catch(RuntimeException e){icon=context.getPackageManager().getDefaultActivityIcon();}
                    result.add(new Entry(key,label,info.getComponentName(),user,icon));
                }
            }catch(SecurityException|IllegalStateException ignored){/* A locked profile must not break Home. */}
            Collator collator=Collator.getInstance();result.sort((a,b)->collator.compare(a.label,b.label));
            main.post(()->{if(!closed&&request==generation)listener.loaded(Collections.unmodifiableList(result));});
        });
    }
    public void close(){closed=true;generation++;launcher.unregisterCallback(callback);worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
