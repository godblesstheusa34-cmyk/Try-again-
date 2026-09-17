package com.chris.fluidhome;
import android.animation.ValueAnimator;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.*;
import android.net.Uri;
import android.os.*;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.window.*;
import android.widget.*;
import java.util.*;

/** S24 Ultra Home application: native controls over a lit 3D scene. */
public final class MainActivity extends Activity implements SensorEventListener {
    private HomeStore store;private AppCatalog catalog;private WidgetController widgets;private SceneSurface scene;
    private FrameLayout safe;private LinearLayout home;private DepthPager pager;private AppDrawer drawer;
    private final TextView[] tabs=new TextView[3];private final Map<String,AppCatalog.Entry> byKey=new HashMap<>();
    private List<AppCatalog.Entry> apps=Collections.emptyList();
    private SensorManager sensors;private Sensor rotation;private final float[] rotationMatrix=new float[9],orientation=new float[3];
    private float tiltX,tiltY,baselineRoll,baselinePitch;private boolean calibrated,resumed;private int choosingSlot=-1;
    private final OnBackInvokedCallback back=()->{if(drawer!=null&&drawer.isOpen())drawer.hide();else if(pager!=null)pager.goTo(1,true);};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setDecorFitsSystemWindows(false);getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);getWindow().setNavigationBarContrastEnforced(false);
        getWindow().getInsetsController().setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        store=new HomeStore(this);sensors=getSystemService(SensorManager.class);rotation=sensors.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        widgets=new WidgetController(this,store,()->{if(home!=null)buildHome(0);});
        FrameLayout root=new FrameLayout(this);scene=new SceneSurface(this);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));safe=new FrameLayout(this);root.addView(safe,new FrameLayout.LayoutParams(-1,-1));
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()),ime=insets.getInsets(WindowInsets.Type.ime());safe.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));return insets;});
        setContentView(root);root.requestApplyInsets();buildHome(state==null?1:state.getInt("page",1));
        catalog=new AppCatalog(this,loaded->{apps=loaded;byKey.clear();for(AppCatalog.Entry app:loaded)byKey.put(app.key,app);seedOnce();refreshHomeContent(pager==null?1:pager.selected());if(drawer!=null)drawer.setApps(apps);});
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,back);
        if(store.recoveredLayout)toast("The saved layout couldn't be read. A recovery copy was kept.");
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("page",pager.selected());super.onSaveInstanceState(out);}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(drawer!=null)drawer.hide();if(pager!=null)pager.goTo(1,true);}
    @Override protected void onStart(){super.onStart();widgets.start();}
    @Override protected void onResume(){super.onResume();resumed=true;requestRefreshRate();scene.resumeScene();applyMotion();catalog.refresh();}
    @Override protected void onPause(){resumed=false;sensors.unregisterListener(this);scene.pauseScene();super.onPause();}
    @Override protected void onStop(){widgets.stop();super.onStop();}
    @Override protected void onDestroy(){sensors.unregisterListener(this);catalog.close();getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(back);super.onDestroy();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);widgets.result(request,result,data);}
    private boolean reduced(){return store.reducedMotion()||!ValueAnimator.areAnimatorsEnabled()||getSystemService(AccessibilityManager.class).isTouchExplorationEnabled();}
    private void applyMotion(){
        sensors.unregisterListener(this);calibrated=false;tiltX=tiltY=0;boolean r=reduced();pager.setMotionReduced(r);drawer.setReduced(r);pager.setTilt(0,0);scene.setTilt(0,0);scene.setAccent(store.accent());scene.setMotion(store.ambient(),r);
        if(resumed&&store.tilt()&&!r&&rotation!=null)sensors.registerListener(this,rotation,SensorManager.SENSOR_DELAY_GAME);
    }
    private void requestRefreshRate(){
        Display display=getDisplay();if(display==null)return;Display.Mode current=display.getMode();float best=0;
        for(Display.Mode mode:display.getSupportedModes())if(mode.getPhysicalWidth()==current.getPhysicalWidth()&&mode.getPhysicalHeight()==current.getPhysicalHeight()&&mode.getRefreshRate()<=120.5f)best=Math.max(best,mode.getRefreshRate());
        WindowManager.LayoutParams p=getWindow().getAttributes();p.preferredRefreshRate=best;getWindow().setAttributes(p);
    }
    @Override public void onSensorChanged(SensorEvent event){
        SensorManager.getRotationMatrixFromVector(rotationMatrix,event.values);SensorManager.getOrientation(rotationMatrix,orientation);
        if(!calibrated){baselineRoll=orientation[2];baselinePitch=orientation[1];calibrated=true;}
        float x=Spring.clamp(angleDelta(orientation[2],baselineRoll)*3f,-1,1),y=Spring.clamp(angleDelta(orientation[1],baselinePitch)*3f,-1,1);tiltX+=(x-tiltX)*.1f;tiltY+=(y-tiltY)*.1f;scene.setTilt(tiltX,tiltY);pager.setTilt(tiltX,tiltY);
    }
    private static float angleDelta(float a,float b){return (float)Math.atan2(Math.sin(a-b),Math.cos(a-b));}
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}

    private void buildHome(int selected){
        if(drawer!=null){drawer.hide();safe.removeView(drawer);}refreshHomeContent(selected);
        drawer=new AppDrawer(this,store.accent(),new AppDrawer.Listener(){
            public void open(AppCatalog.Entry app){if(choosingSlot>=0){int slot=choosingSlot;choosingSlot=-1;drawer.hide();putApp(slot,app);}else{launch(app);drawer.hide();}}
            public void menu(AppCatalog.Entry app){appMenu(app);}
            public void closed(){choosingSlot=-1;if(home!=null)home.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);}
        });drawer.setApps(apps);safe.addView(drawer,new FrameLayout.LayoutParams(-1,-1));applyMotion();
    }
    private void refreshHomeContent(int selected){
        if(home!=null)safe.removeView(home);home=Ui.column(this);home.setClipChildren(false);home.setClipToPadding(false);safe.addView(home,0,new FrameLayout.LayoutParams(-1,-1));
        if(drawer!=null&&drawer.isOpen())home.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(Ui.dp(this,24),0,Ui.dp(this,24),0);TextView mark=Ui.text(this,"F L U I D",12,store.accent());mark.setGravity(Gravity.CENTER_VERTICAL);bar.addView(mark,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));
        TextView settings=Ui.text(this,"Settings",13,Ui.MUTED);settings.setGravity(Gravity.CENTER);settings.setMinWidth(Ui.dp(this,72));settings.setMinHeight(Ui.dp(this,48));settings.setFocusable(true);settings.setOnClickListener(v->settings());bar.addView(settings);home.addView(bar);
        pager=new DepthPager(this,false);pager.addView(spacePage());pager.addView(centerPage());pager.addView(toolsPage());home.addView(pager,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout pageTabs=new LinearLayout(this);pageTabs.setGravity(Gravity.CENTER);String[] names={"Space","Home","Tools"};
        for(int i=0;i<3;i++){
            final int index=i;TextView tab=Ui.text(this,names[i],12,Ui.MUTED);tabs[i]=tab;tab.setGravity(Gravity.CENTER);tab.setFocusable(true);tab.setOnClickListener(v->pager.goTo(index,true));pageTabs.addView(tab,new LinearLayout.LayoutParams(Ui.dp(this,80),Ui.dp(this,48)));
            Runnable turn=()->pager.goTo(index,true);tab.setOnDragListener((v,e)->{if(!(e.getLocalState() instanceof Integer))return false;if(e.getAction()==DragEvent.ACTION_DRAG_ENTERED)v.postDelayed(turn,500);if(e.getAction()==DragEvent.ACTION_DRAG_EXITED||e.getAction()==DragEvent.ACTION_DRAG_ENDED)v.removeCallbacks(turn);return e.getAction()!=DragEvent.ACTION_DROP;});
        }home.addView(pageTabs);
        LinearLayout dock=new LinearLayout(this);dock.setGravity(Gravity.CENTER);dock.setPadding(Ui.dp(this,10),Ui.dp(this,3),Ui.dp(this,10),Ui.dp(this,3));Ui.material(dock,store.accent(),28,true);
        for(int i=0;i<5;i++){View tile;if(i==2){tile=Ui.button(this,"Apps",store.accent(),()->openDrawer(-1));tile.setContentDescription("Open all apps");}else tile=slotTile(36+(i<2?i:i-1),true);dock.addView(tile,new LinearLayout.LayoutParams(0,Ui.dp(this,70),1));}
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,Ui.dp(this,78));dp.setMargins(Ui.dp(this,16),0,Ui.dp(this,16),Ui.dp(this,9));home.addView(dock,dp);
        pager.setListener((position,current)->{scene.setPosition(position);for(int i=0;i<tabs.length;i++)if(tabs[i]!=null){tabs[i].setTextColor(i==current?store.accent():Ui.MUTED);tabs[i].setSelected(i==current);}});pager.setMotionReduced(reduced());pager.goTo(selected,false);
    }
    private ScrollView page(LinearLayout body){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);body.setPadding(Ui.dp(this,24),Ui.dp(this,18),Ui.dp(this,24),Ui.dp(this,20));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));return scroll;}
    private View centerPage(){
        LinearLayout body=Ui.column(this);TextView tag=Ui.text(this,"MAKE ROOM FOR YOUR DAY",10,store.accent());tag.setLetterSpacing(.15f);body.addView(tag,Ui.spaced(this,-2,16));
        TextClock clock=new TextClock(this);clock.setFormat12Hour("h:mm");clock.setFormat24Hour("HH:mm");clock.setTextColor(Ui.INK);clock.setTextSize(78);clock.setIncludeFontPadding(false);clock.setTypeface(Typeface.create("sans-serif-thin",Typeface.NORMAL));clock.setFontFeatureSettings("tnum");clock.setOnClickListener(v->system(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));body.addView(clock,Ui.spaced(this,-2,16));
        TextClock date=new TextClock(this);date.setFormat12Hour("EEEE, MMMM d");date.setFormat24Hour("EEEE, MMMM d");date.setTextColor(Ui.MUTED);date.setTextSize(15);body.addView(date,Ui.spaced(this,-2,6));
        LinearLayout quick=new LinearLayout(this);quick.addView(Ui.button(this,"Search apps",store.accent(),()->openDrawer(-1)),new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));TextView battery=Ui.button(this,batteryLabel(),store.accent(),()->system(new Intent(Intent.ACTION_POWER_USAGE_SUMMARY)));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);bp.leftMargin=Ui.dp(this,10);quick.addView(battery,bp);body.addView(quick,Ui.spaced(this,-2,32));
        section(body,"YOUR APPS",30);body.addView(grid(1));body.addView(Ui.text(this,"Hold an app to move it, group it, or open its tools.",12,Ui.MUTED),Ui.spaced(this,-2,14));return page(body);
    }
    private View spacePage(){LinearLayout body=Ui.column(this);Ui.heading(body,"Your space","01 / Widgets",store.accent());body.addView(Ui.button(this,"+ Add a widget",store.accent(),widgets::choose));LinearLayout rack=Ui.column(this);body.addView(rack);widgets.render(rack);section(body,"MORE APPS",30);body.addView(grid(0));return page(body);}
    private View toolsPage(){
        LinearLayout body=Ui.column(this);Ui.heading(body,"App tools","03 / Your phone",store.accent());body.addView(Ui.button(this,"Search installed apps",store.accent(),()->openDrawer(-1)));
        body.addView(Ui.button(this,"Set Fluid Home as default",store.accent(),this::requestHome),Ui.spaced(this,52,12));
        body.addView(Ui.button(this,"Choose your default Home app",store.accent(),()->system(new Intent(Settings.ACTION_HOME_SETTINGS))),Ui.spaced(this,52,12));
        body.addView(Ui.button(this,"System settings",store.accent(),()->system(new Intent(Settings.ACTION_SETTINGS))),Ui.spaced(this,52,12));body.addView(Ui.button(this,"Display settings",store.accent(),()->system(new Intent(Settings.ACTION_DISPLAY_SETTINGS))),Ui.spaced(this,52,12));
        body.addView(Ui.button(this,"Phone and build information",store.accent(),this::deviceInfo),Ui.spaced(this,52,12));section(body,"MORE APPS",28);body.addView(grid(2));return page(body);
    }
    private void section(LinearLayout body,String label,int margin){TextView t=Ui.text(this,label,10,Ui.MUTED);t.setLetterSpacing(.16f);body.addView(t,Ui.spaced(this,-2,margin));}
    private View grid(int page){LinearLayout grid=Ui.column(this);for(int row=0;row<3;row++){LinearLayout line=new LinearLayout(this);for(int col=0;col<4;col++)line.addView(slotTile(page*12+row*4+col,false),new LinearLayout.LayoutParams(0,Ui.dp(this,100),1));grid.addView(line);}return grid;}
    private AppTile slotTile(int slot,boolean compact){
        Workspace.Item item=store.workspace.get(slot);List<Drawable> icons=new ArrayList<>();if(item!=null)for(String key:item.apps){AppCatalog.Entry app=byKey.get(key);icons.add(app==null?getPackageManager().getDefaultActivityIcon():app.icon);}
        AppTile tile=new AppTile(this,label(item),icons,store.accent(),compact);tile.setHapticFeedbackEnabled(store.haptics());
        tile.setOnClickListener(v->{if(store.haptics())v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);if(item==null)openDrawer(slot);else if(item.folder())folder(slot);else launchKey(item.apps.get(0));});
        tile.setOnLongClickListener(v->{if(item==null)openDrawer(slot);else slotMenu(slot,tile);return true;});
        tile.setOnDragListener((v,e)->{if(!(e.getLocalState() instanceof Integer))return false;switch(e.getAction()){
            case DragEvent.ACTION_DRAG_ENTERED:v.setAlpha(.5f);return true;
            case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:v.setAlpha(1);return true;
            case DragEvent.ACTION_DROP:v.setAlpha(1);int from=(Integer)e.getLocalState();if(from==slot)return false;v.post(()->drop(from,slot));return true;
            default:return true;
        }});return tile;
    }
    private String label(Workspace.Item item){if(item==null)return "Add";if(item.folder())return item.title;AppCatalog.Entry app=byKey.get(item.apps.get(0));return app==null?"Unavailable":app.label;}
    private void openDrawer(int slot){choosingSlot=slot;home.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);drawer.show(slot>=0);}
    private void launchKey(String key){AppCatalog.Entry app=byKey.get(key);if(app==null){toast("This app isn't available. Its profile may be locked or it may have been removed.");return;}launch(app);}
    private void launch(AppCatalog.Entry app){try{catalog.launcher.startMainActivity(app.component,app.user,null,null);}catch(ActivityNotFoundException|SecurityException|IllegalStateException e){toast("Android couldn't open "+app.label+". Its profile may be paused or locked.");catalog.refresh();}}
    private void system(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException|SecurityException e){toast("That action isn't available on this phone.");}}
    private void requestHome(){RoleManager manager=getSystemService(RoleManager.class);if(manager.isRoleHeld(RoleManager.ROLE_HOME)){toast("Fluid Home is already your default Home app.");return;}if(manager.isRoleAvailable(RoleManager.ROLE_HOME))system(manager.createRequestRoleIntent(RoleManager.ROLE_HOME));else system(new Intent(Settings.ACTION_HOME_SETTINGS));}
    private void seedOnce(){
        if(apps.isEmpty()||store.prefs.getBoolean("seeded",false)||store.recoveredLayout)return;
        for(int i=0;i<Math.min(8,apps.size());i++)store.workspace.put(12+i,Workspace.Item.app(apps.get(i).key));
        String[][] preferred={{"com.samsung.android.dialer","com.google.android.dialer"},{"com.google.android.apps.messaging","com.samsung.android.messaging"},{"com.android.chrome","com.sec.android.app.sbrowser"},{"com.sec.android.app.camera","com.google.android.GoogleCamera"}};
        for(int i=0;i<4;i++){boolean found=false;for(String p:preferred[i]){for(AppCatalog.Entry app:apps)if(app.component.getPackageName().equals(p)&&app.user.equals(android.os.Process.myUserHandle())){store.workspace.put(36+i,Workspace.Item.app(app.key));found=true;break;}if(found)break;}}
        store.save();store.prefs.edit().putBoolean("seeded",true).apply();
    }
    private void saveAndRefresh(){store.save();refreshHomeContent(pager.selected());}
    private void putApp(int slot,AppCatalog.Entry app){Runnable put=()->{store.workspace.put(slot,Workspace.Item.app(app.key));saveAndRefresh();};if(store.workspace.get(slot)==null)put.run();else new AlertDialog.Builder(this).setTitle("Replace "+label(store.workspace.get(slot))+"?").setPositiveButton("Replace",(d,w)->put.run()).setNegativeButton("Cancel",null).show();}
    private void appMenu(AppCatalog.Entry app){
        new AlertDialog.Builder(this).setTitle(app.label).setItems(new String[]{"Open","Pin to a home page","Pin to dock","App information","Uninstall"},(d,which)->{
            switch(which){
                case 0:launch(app);break;
                case 1:pickPage(page->{int slot=store.workspace.firstFree(page);if(slot<0)toast("This page is full. Remove or move an app first.");else{drawer.hide();putApp(slot,app);pager.goTo(page,true);}});break;
                case 2:pickSlot(36,4,slot->{drawer.hide();putApp(slot,app);});break;
                case 3:appInfo(app);break;
                case 4:if(!app.user.equals(android.os.Process.myUserHandle()))appInfo(app);else system(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+app.component.getPackageName())));break;
            }
        }).setNegativeButton("Cancel",null).show();
    }
    private void appInfo(AppCatalog.Entry app){try{catalog.launcher.startAppDetailsActivity(app.component,app.user,null,null);}catch(ActivityNotFoundException|SecurityException e){toast("App information is unavailable.");}}
    private interface Choice{void picked(int index);}
    private void pickPage(Choice choice){new AlertDialog.Builder(this).setTitle("Choose a page").setItems(new String[]{"Space","Home","Tools"},(d,w)->choice.picked(w)).setNegativeButton("Cancel",null).show();}
    private void pickSlot(int first,int count,Choice choice){String[] labels=new String[count];for(int i=0;i<count;i++)labels[i]=(i+1)+" · "+label(store.workspace.get(first+i));new AlertDialog.Builder(this).setTitle("Choose a position").setItems(labels,(d,w)->choice.picked(first+w)).setNegativeButton("Cancel",null).show();}
    private void slotMenu(int slot,View tile){
        Workspace.Item item=store.workspace.get(slot);if(item==null)return;List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        labels.add("Move or group by dragging");actions.add(()->tile.post(()->tile.startDragAndDrop(ClipData.newPlainText("Fluid Home position",Integer.toString(slot)),new View.DragShadowBuilder(tile),slot,0)));
        labels.add("Move to another position");actions.add(()->new AlertDialog.Builder(this).setTitle("Move to").setItems(new String[]{"Space","Home","Tools","Dock"},(d,which)->pickSlot(which==3?36:which*12,which==3?4:12,to->{store.workspace.swap(slot,to);saveAndRefresh();if(which<3)pager.goTo(which,true);})).setNegativeButton("Cancel",null).show());
        if(item.folder()){labels.add("Rename folder");actions.add(()->rename(slot));labels.add("Open folder");actions.add(()->folder(slot));}
        else{AppCatalog.Entry app=byKey.get(item.apps.get(0));if(app!=null){labels.add("App tools");actions.add(()->appMenu(app));}}
        labels.add("Replace with another app");actions.add(()->openDrawer(slot));labels.add("Remove from home");actions.add(()->{store.workspace.put(slot,null);saveAndRefresh();});
        new AlertDialog.Builder(this).setTitle(label(item)).setItems(labels.toArray(new String[0]),(d,w)->actions.get(w).run()).setNegativeButton("Cancel",null).show();
    }
    private void drop(int from,int to){
        if(store.workspace.get(from)==null)return;if(store.workspace.get(to)==null){store.workspace.swap(from,to);saveAndRefresh();return;}
        new AlertDialog.Builder(this).setTitle("Place apps together?").setItems(new String[]{"Create or add to a folder","Swap positions"},(d,w)->{if(w==0&&!store.workspace.merge(from,to)){toast("A folder holds up to 32 apps.");return;}if(w==1)store.workspace.swap(from,to);saveAndRefresh();}).setNegativeButton("Cancel",null).show();
    }
    private void rename(int slot){Workspace.Item item=store.workspace.get(slot);if(item==null)return;EditText field=new EditText(this);field.setSingleLine(true);field.setText(item.title);field.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(40)});new AlertDialog.Builder(this).setTitle("Folder name").setView(field).setPositiveButton("Save",(d,w)->{store.workspace.put(slot,new Workspace.Item(field.getText().toString(),item.apps));saveAndRefresh();}).setNegativeButton("Cancel",null).show();}
    private void folder(int slot){
        Workspace.Item item=store.workspace.get(slot);if(item==null)return;String[] labels=new String[item.apps.size()];for(int i=0;i<labels.length;i++){AppCatalog.Entry app=byKey.get(item.apps.get(i));labels[i]=app==null?"Unavailable":app.label;}
        new AlertDialog.Builder(this).setTitle(item.title).setItems(labels,(d,w)->launchKey(item.apps.get(w))).setNeutralButton("Edit apps",(d,w)->new AlertDialog.Builder(this).setTitle("Remove from folder").setItems(labels,(d2,i)->{store.workspace.removeFromFolder(slot,item.apps.get(i));saveAndRefresh();}).setNegativeButton("Cancel",null).show()).setNegativeButton("Close",null).show();
    }
    private void settings(){
        LinearLayout body=Ui.column(this);body.setPadding(Ui.dp(this,22),Ui.dp(this,10),Ui.dp(this,22),Ui.dp(this,12));switchSetting(body,"Phone tilt","tilt",store.tilt());switchSetting(body,"Ambient 3D motion","ambient",store.ambient());switchSetting(body,"Reduced motion","reduced_motion",store.reducedMotion());switchSetting(body,"Touch feedback","haptics",store.haptics());
        body.addView(Ui.button(this,"Accent color",store.accent(),()->{String[] names={"Sea glass","Glacier","Ember","Orchid"};int[] colors={0xff8fe6d5,0xff91c6ff,0xffefb480,0xffc1adfa};new AlertDialog.Builder(this).setTitle("Accent color").setItems(names,(d,w)->store.prefs.edit().putInt("accent",colors[w]).apply()).show();}),Ui.spaced(this,50,12));
        body.addView(Ui.button(this,"Set as default Home",store.accent(),this::requestHome),Ui.spaced(this,50,12));ScrollView scroll=new ScrollView(this);scroll.addView(body);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Fluid Home").setView(scroll).setPositiveButton("Done",null).create();dialog.setOnDismissListener(d->buildHome(pager.selected()));dialog.show();
    }
    private void switchSetting(LinearLayout body,String text,String key,boolean initial){Switch toggle=new Switch(this);toggle.setText(text);toggle.setTextSize(15);toggle.setChecked(initial);toggle.setMinHeight(Ui.dp(this,54));toggle.setOnCheckedChangeListener((v,checked)->store.prefs.edit().putBoolean(key,checked).apply());body.addView(toggle,new LinearLayout.LayoutParams(-1,-2));}
    private String batteryLabel(){int level=getSystemService(BatteryManager.class).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);return level>=0&&level<=100?"Battery  "+level+"%":"Battery";}
    private void deviceInfo(){
        ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();getSystemService(ActivityManager.class).getMemoryInfo(memory);String version;
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException e){version="Unknown";}
        String info="Fluid Home "+version+"\n\n"+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+"\n"+batteryLabel()+"\n"+String.format(Locale.getDefault(),"%.1f GB reported RAM\n%.1f Hz current display mode",memory.totalMem/1073741824d,getDisplay()==null?0:getDisplay().getMode().getRefreshRate());
        new AlertDialog.Builder(this).setTitle("Phone and build").setMessage(info).setPositiveButton("Close",null).show();
    }
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
}
