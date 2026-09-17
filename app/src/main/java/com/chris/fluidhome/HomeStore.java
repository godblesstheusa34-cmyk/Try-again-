package com.chris.fluidhome;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class HomeStore {
    public final SharedPreferences prefs;
    public final Workspace workspace=new Workspace();
    public final ArrayList<WidgetSpec> widgets=new ArrayList<>();
    public boolean recoveredLayout;
    public static final class WidgetSpec {public final int id;public int height;public WidgetSpec(int id,int height){this.id=id;this.height=height;}}
    public HomeStore(Context context){
        prefs=context.getSharedPreferences("fluid_home_v1",Context.MODE_PRIVATE);
        String raw=prefs.getString("layout","");if(raw.isEmpty())return;
        try{
            JSONObject root=new JSONObject(raw);if(root.getInt("schema")!=1)throw new JSONException("Unknown schema");
            Workspace decoded=new Workspace();ArrayList<WidgetSpec> decodedWidgets=new ArrayList<>();
            JSONArray items=root.getJSONArray("slots");
            for(int i=0;i<Math.min(items.length(),Workspace.SIZE);i++){
                if(items.isNull(i))continue;JSONObject item=items.getJSONObject(i);JSONArray keys=item.getJSONArray("apps");List<String> apps=new ArrayList<>();
                for(int j=0;j<keys.length();j++)apps.add(keys.getString(j));decoded.put(i,new Workspace.Item(item.optString("title","Folder"),apps));
            }
            JSONArray saved=root.optJSONArray("widgets");
            if(saved!=null)for(int i=0;i<saved.length();i++){JSONObject w=saved.getJSONObject(i);int id=w.getInt("id");if(id>0)decodedWidgets.add(new WidgetSpec(id,(int)Spring.clamp(w.optInt("height",220),80,900)));}
            for(int i=0;i<Workspace.SIZE;i++)workspace.put(i,decoded.get(i));widgets.addAll(decodedWidgets);
        }catch(JSONException|IllegalArgumentException e){prefs.edit().putString("layout_recovery",raw).apply();recoveredLayout=true;}
    }
    public void save(){
        try{
            JSONObject root=new JSONObject().put("schema",1);JSONArray slots=new JSONArray(),widgetArray=new JSONArray();
            for(int i=0;i<Workspace.SIZE;i++){Workspace.Item item=workspace.get(i);slots.put(item==null?JSONObject.NULL:new JSONObject().put("title",item.title).put("apps",new JSONArray(item.apps)));}
            for(WidgetSpec w:widgets)widgetArray.put(new JSONObject().put("id",w.id).put("height",w.height));
            root.put("slots",slots).put("widgets",widgetArray);prefs.edit().putString("layout",root.toString()).apply();
        }catch(JSONException e){throw new IllegalStateException("Cannot serialize layout",e);}
    }
    public int accent(){return prefs.getInt("accent",0xff8fe6d5);}
    public boolean tilt(){return prefs.getBoolean("tilt",true);}
    public boolean reducedMotion(){return prefs.getBoolean("reduced_motion",false);}
    public boolean ambient(){return prefs.getBoolean("ambient",true);}
    public boolean haptics(){return prefs.getBoolean("haptics",true);}
}
