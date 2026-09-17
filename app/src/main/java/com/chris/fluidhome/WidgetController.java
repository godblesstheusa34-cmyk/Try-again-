package com.chris.fluidhome;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

/** Real widget binding/configuration; pending IDs survive process recreation. */
public final class WidgetController {
    public static final int REQUEST_BIND=201,REQUEST_CONFIGURE=202;
    private final Activity activity;private final HomeStore store;private final Runnable changed;
    private final AppWidgetHost host;private final AppWidgetManager manager;private boolean listening;
    public WidgetController(Activity a,HomeStore s,Runnable changed){
        activity=a;store=s;this.changed=changed;host=new AppWidgetHost(a,0x464c5549);manager=AppWidgetManager.getInstance(a);
        Set<Integer> owned=new HashSet<>();for(HomeStore.WidgetSpec w:s.widgets)owned.add(w.id);owned.add(pending());
        if(!s.recoveredLayout)for(int id:host.getAppWidgetIds())if(!owned.contains(id))host.deleteAppWidgetId(id);
    }
    public void start(){if(!listening){host.startListening();listening=true;}}
    public void stop(){if(listening){host.stopListening();listening=false;}}
    private int pending(){return store.prefs.getInt("pending_widget_id",-1);}
    public void choose(){
        if(pending()>0){pendingMenu();return;}List<AppWidgetProviderInfo> providers=new ArrayList<>(manager.getInstalledProviders());
        providers.sort((a,b)->a.loadLabel(activity.getPackageManager()).compareToIgnoreCase(b.loadLabel(activity.getPackageManager())));
        String[] labels=new String[providers.size()];for(int i=0;i<labels.length;i++)labels[i]=providers.get(i).loadLabel(activity.getPackageManager())+" · "+providers.get(i).provider.getPackageName();
        if(labels.length==0){toast("No widget providers are available in this profile.");return;}
        new AlertDialog.Builder(activity).setTitle("Add a widget").setItems(labels,(d,w)->bind(providers.get(w))).setNegativeButton("Cancel",null).show();
    }
    private void bind(AppWidgetProviderInfo info){
        int width=(int)(activity.getResources().getDisplayMetrics().widthPixels/activity.getResources().getDisplayMetrics().density)-80;
        int minWidth=(info.resizeMode&AppWidgetProviderInfo.RESIZE_HORIZONTAL)!=0&&info.minResizeWidth>0?Math.min(info.minWidth,info.minResizeWidth):info.minWidth;
        if(minWidth>width||info.minHeight>900){toast("This widget needs more room than this portrait layout provides.");return;}
        int id=host.allocateAppWidgetId();
        // Commit before another activity is launched, so process death cannot lose this ID.
        store.prefs.edit().putInt("pending_widget_id",id).putInt("pending_widget_height",Math.max(180,info.minHeight)).commit();
        Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        try{
            if(manager.bindAppWidgetIdIfAllowed(id,info.getProfile(),info.provider,options))configure();
            else activity.startActivityForResult(new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider).putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.getProfile()).putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options),REQUEST_BIND);
        }catch(SecurityException|ActivityNotFoundException e){cancel();toast("Android couldn't start widget setup.");}
    }
    private void configure(){
        int id=pending();AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);if(info==null){cancel();return;}
        boolean optional=(info.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL)!=0&&(info.widgetFeatures&AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE)!=0;
        if(info.configure!=null&&!optional){try{host.startAppWidgetConfigureActivityForResult(activity,id,0,REQUEST_CONFIGURE,null);}catch(ActivityNotFoundException|SecurityException e){cancel();toast("This widget's setup activity isn't available.");}}
        else finish();
    }
    public boolean result(int request,int result,Intent data){
        if(request!=REQUEST_BIND&&request!=REQUEST_CONFIGURE)return false;int id=pending();if(id<1)return true;
        int returned=data==null?id:data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
        if(result!=Activity.RESULT_OK||returned!=id)cancel();else if(request==REQUEST_BIND)configure();else finish();return true;
    }
    private void finish(){
        int id=pending();if(id<=0||manager.getAppWidgetInfo(id)==null){cancel();return;}boolean exists=false;for(HomeStore.WidgetSpec w:store.widgets)if(w.id==id)exists=true;
        if(!exists)store.widgets.add(new HomeStore.WidgetSpec(id,store.prefs.getInt("pending_widget_height",220)));store.save();clearPending();changed.run();
    }
    private void clearPending(){store.prefs.edit().remove("pending_widget_id").remove("pending_widget_height").apply();}
    private void cancel(){if(pending()>0)host.deleteAppWidgetId(pending());clearPending();changed.run();}
    private void pendingMenu(){new AlertDialog.Builder(activity).setTitle("Finish adding your widget").setMessage("A widget setup was interrupted.").setPositiveButton("Continue",(d,w)->configure()).setNegativeButton("Discard",(d,w)->cancel()).show();}
    public void render(LinearLayout rack){
        rack.removeAllViews();if(pending()>0)rack.addView(Ui.button(activity,"Finish widget setup",store.accent(),this::pendingMenu),Ui.spaced(activity,48,10));
        if(store.widgets.isEmpty()){TextView empty=Ui.text(activity,"Add a calendar, music player, weather, or another widget from your installed apps.",14,Ui.MUTED);empty.setLineSpacing(Ui.dp(activity,4),1);rack.addView(empty,Ui.spaced(activity,-2,20));}
        for(HomeStore.WidgetSpec spec:new ArrayList<>(store.widgets)){
            AppWidgetProviderInfo info=manager.getAppWidgetInfo(spec.id);LinearLayout card=Ui.column(activity);Ui.material(card,store.accent(),22,true);card.setPadding(Ui.dp(activity,12),Ui.dp(activity,10),Ui.dp(activity,12),Ui.dp(activity,12));
            LinearLayout bar=new LinearLayout(activity);bar.setGravity(android.view.Gravity.CENTER_VERTICAL);TextView name=Ui.text(activity,info==null?"Widget unavailable":info.loadLabel(activity.getPackageManager()),13,Ui.INK);name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(name,new LinearLayout.LayoutParams(0,-2,1));bar.addView(Ui.button(activity,"Size",store.accent(),()->resize(spec,info)));
            bar.addView(Ui.button(activity,"Remove",store.accent(),()->new AlertDialog.Builder(activity).setTitle("Remove this widget?").setPositiveButton("Remove",(d,w)->{host.deleteAppWidgetId(spec.id);store.widgets.remove(spec);store.save();changed.run();}).setNegativeButton("Keep",null).show()));card.addView(bar);
            if(info!=null){AppWidgetHostView view=host.createView(activity,spec.id,info);card.addView(view,new LinearLayout.LayoutParams(-1,Ui.dp(activity,spec.height)));view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot){float density=activity.getResources().getDisplayMetrics().density;int w=(int)((r-l)/density),h=(int)((b-t)/density);view.updateAppWidgetSize(null,w,h,w,h);}});}
            rack.addView(card,Ui.spaced(activity,-2,18));
        }
    }
    private void resize(HomeStore.WidgetSpec spec,AppWidgetProviderInfo info){
        if(info==null)return;if((info.resizeMode&AppWidgetProviderInfo.RESIZE_VERTICAL)==0){toast("This widget has a fixed height.");return;}
        int min=info.minResizeHeight>0?info.minResizeHeight:info.minHeight,max=info.maxResizeHeight>0?info.maxResizeHeight:900;
        List<Integer> sizes=new ArrayList<>();if(min>0&&min<=max)sizes.add(min);for(int s:new int[]{140,180,240,320,420,560,720,900})if(s>=min&&s<=max&&!sizes.contains(s))sizes.add(s);sizes.sort(Integer::compareTo);
        String[] labels=new String[sizes.size()];for(int i=0;i<labels.length;i++)labels[i]="Height "+sizes.get(i)+" dp";
        new AlertDialog.Builder(activity).setTitle("Widget height").setItems(labels,(d,w)->{spec.height=sizes.get(w);store.save();changed.run();}).setNegativeButton("Cancel",null).show();
    }
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
}
