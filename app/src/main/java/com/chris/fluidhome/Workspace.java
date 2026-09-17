package com.chris.fluidhome;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure placement rules: a key identifies one launchable activity and its Android user. */
public final class Workspace {
    public static final int PAGES=3, PER_PAGE=12, DOCK_START=36, SIZE=40, MAX_FOLDER_APPS=32;
    public static final class Item {
        public final String title;
        public final List<String> apps;
        public Item(String title,List<String> apps) {
            if(apps.isEmpty()||apps.size()>MAX_FOLDER_APPS) throw new IllegalArgumentException("Invalid folder size");
            ArrayList<String> unique=new ArrayList<>();
            for(String key:apps){
                if(key==null||key.isEmpty())throw new IllegalArgumentException("Empty app key");
                if(!unique.contains(key))unique.add(key);
            }
            this.title=title==null||title.trim().isEmpty()?"Folder":title.trim();
            this.apps=Collections.unmodifiableList(unique);
        }
        public static Item app(String key){return new Item("",Collections.singletonList(key));}
        public boolean folder(){return apps.size()>1;}
    }
    private final Item[] slots=new Item[SIZE];
    public Item get(int slot){check(slot);return slots[slot];}
    public void put(int slot,Item item){check(slot);slots[slot]=item;}
    public int firstFree(int page){
        if(page<0||page>=PAGES)throw new IllegalArgumentException("Invalid page");
        for(int s=page*PER_PAGE;s<(page+1)*PER_PAGE;s++)if(slots[s]==null)return s;
        return -1;
    }
    public void swap(int from,int to){check(from);check(to);Item x=slots[to];slots[to]=slots[from];slots[from]=x;}
    public boolean merge(int from,int to){
        check(from);check(to);if(from==to||slots[from]==null)return false;
        if(slots[to]==null){swap(from,to);return true;}
        List<String> combined=new ArrayList<>(slots[to].apps);
        for(String key:slots[from].apps)if(!combined.contains(key))combined.add(key);
        if(combined.size()>MAX_FOLDER_APPS)return false;
        slots[to]=new Item(slots[to].folder()?slots[to].title:"Folder",combined);slots[from]=null;return true;
    }
    public void removeFromFolder(int slot,String key){
        check(slot);if(slots[slot]==null)return;
        List<String> left=new ArrayList<>(slots[slot].apps);left.remove(key);
        slots[slot]=left.isEmpty()?null:new Item(slots[slot].title,left);
    }
    private static void check(int slot){if(slot<0||slot>=SIZE)throw new IndexOutOfBoundsException("Slot "+slot);}
}
