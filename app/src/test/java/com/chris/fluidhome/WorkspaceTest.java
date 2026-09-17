package com.chris.fluidhome;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class WorkspaceTest {
    @Test public void mergePreservesAppsAndClearsSource(){Workspace w=new Workspace();w.put(12,Workspace.Item.app("u0|a"));w.put(13,Workspace.Item.app("u0|b"));assertTrue(w.merge(12,13));assertNull(w.get(12));assertEquals(Arrays.asList("u0|b","u0|a"),w.get(13).apps);}
    @Test public void fullFolderCannotDeleteDraggedApp(){Workspace w=new Workspace();List<String> keys=new ArrayList<>();for(int i=0;i<32;i++)keys.add("u0|"+i);w.put(1,new Workspace.Item("Music",keys));w.put(2,Workspace.Item.app("u0|extra"));assertFalse(w.merge(2,1));assertNotNull(w.get(2));assertEquals(32,w.get(1).apps.size());}
    @Test public void mergeDeduplicatesButPreservesSeparateProfiles(){Workspace w=new Workspace();w.put(1,new Workspace.Item("Folder",Arrays.asList("0|mail","1|mail")));w.put(2,Workspace.Item.app("0|mail"));assertTrue(w.merge(2,1));assertEquals(2,w.get(1).apps.size());}
    @Test public void swapAcrossPagesAndDockIsLossless(){Workspace w=new Workspace();w.put(0,Workspace.Item.app("a"));w.put(39,Workspace.Item.app("b"));w.swap(0,39);assertEquals("b",w.get(0).apps.get(0));assertEquals("a",w.get(39).apps.get(0));}
    @Test public void removingFolderMembersCollapsesThenEmpties(){Workspace w=new Workspace();w.put(12,new Workspace.Item("Tools",Arrays.asList("a","b")));w.removeFromFolder(12,"a");assertFalse(w.get(12).folder());w.removeFromFolder(12,"b");assertNull(w.get(12));}
    @Test public void fullPageCannotOverwriteAnother(){Workspace w=new Workspace();for(int i=12;i<24;i++)w.put(i,Workspace.Item.app("a"+i));assertEquals(-1,w.firstFree(1));assertEquals(0,w.firstFree(0));assertEquals(24,w.firstFree(2));}
}
