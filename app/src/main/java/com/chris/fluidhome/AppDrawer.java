package com.chris.fluidhome;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public final class AppDrawer extends FrameLayout {
    public interface Listener{void open(AppCatalog.Entry app);void menu(AppCatalog.Entry app);void closed();}
    private final EditText search;private final DepthPager pager;private final TextView count,title;private final Listener listener;private final int accent;
    private List<AppCatalog.Entry> all=Collections.emptyList();private boolean reduced;
    public AppDrawer(Context c,int accent,Listener listener){
        super(c);this.accent=accent;this.listener=listener;setBackgroundColor(0xf5080e18);setVisibility(GONE);setClickable(true);setFocusableInTouchMode(true);
        LinearLayout content=Ui.column(c);content.setPadding(Ui.dp(c,20),Ui.dp(c,12),Ui.dp(c,20),Ui.dp(c,8));addView(content,new LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(c);top.setGravity(Gravity.CENTER_VERTICAL);title=Ui.text(c,"All apps",30,Ui.INK);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));top.addView(Ui.button(c,"Close",accent,this::hide));content.addView(top);
        search=new EditText(c);search.setTextSize(16);search.setTextColor(Ui.INK);search.setHintTextColor(Ui.MUTED);search.setHint("Search your apps");search.setSingleLine(true);search.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);search.setPadding(Ui.dp(c,18),0,Ui.dp(c,18),0);Ui.material(search,accent,18,true);content.addView(search,Ui.spaced(c,54,16));
        pager=new DepthPager(c,true);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1);pp.topMargin=Ui.dp(c,18);content.addView(pager,pp);
        LinearLayout nav=new LinearLayout(c);nav.setGravity(Gravity.CENTER_VERTICAL);nav.addView(Ui.button(c,"Previous",accent,()->pager.goTo(pager.selected()-1,true)));count=Ui.text(c,"",12,Ui.MUTED);count.setGravity(Gravity.CENTER);nav.addView(count,new LinearLayout.LayoutParams(0,Ui.dp(c,48),1));nav.addView(Ui.button(c,"Next",accent,()->pager.goTo(pager.selected()+1,true)));content.addView(nav);
        pager.setListener((p,s)->count.setText((s+1)+" / "+Math.max(1,pager.getChildCount())));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){}public void onTextChanged(CharSequence s,int a,int b,int n){populate();}public void afterTextChanged(Editable e){}});
        search.setOnEditorActionListener((v,a,e)->{keyboardOff();search.clearFocus();return true;});
    }
    public void setApps(List<AppCatalog.Entry> apps){all=apps;populate();}
    public void setReduced(boolean value){reduced=value;pager.setMotionReduced(value);}
    public boolean isOpen(){return getVisibility()==VISIBLE;}
    public void show(boolean choose){
        animate().cancel();title.setText(choose?"Choose an app":"All apps");search.setText("");setVisibility(VISIBLE);requestFocus();
        if(!reduced&&ValueAnimator.areAnimatorsEnabled()){setPivotY(getHeight());setCameraDistance(Ui.dp(getContext(),3000));setAlpha(0);setTranslationY(Ui.dp(getContext(),90));setRotationX(-9);setScaleX(.96f);setScaleY(.96f);animate().alpha(1).translationY(0).rotationX(0).scaleX(1).scaleY(1).setDuration(330).setInterpolator(new android.view.animation.DecelerateInterpolator(1.6f)).start();}
        else{setAlpha(1);setTranslationY(0);setRotationX(0);setScaleX(1);setScaleY(1);}
    }
    public void hide(){if(!isOpen())return;animate().cancel();keyboardOff();search.clearFocus();setVisibility(GONE);listener.closed();}
    private void keyboardOff(){((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);}
    private void populate(){
        String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);List<AppCatalog.Entry> filtered=new ArrayList<>();for(AppCatalog.Entry app:all)if(app.label.toLowerCase(Locale.ROOT).contains(query))filtered.add(app);pager.removeAllViews();Context c=getContext();
        for(int page=0;page<Math.max(1,(filtered.size()+19)/20);page++){
            ScrollView scroll=new ScrollView(c);LinearLayout slab=Ui.column(c);Ui.material(slab,accent,24,true);slab.setPadding(Ui.dp(c,10),Ui.dp(c,20),Ui.dp(c,10),Ui.dp(c,20));TextView section=Ui.text(c,filtered.isEmpty()?"No matching apps":filtered.get(page*20).label.substring(0,1).toUpperCase(Locale.getDefault())+" / "+filtered.size()+" apps",12,accent);section.setPadding(Ui.dp(c,8),0,0,Ui.dp(c,12));slab.addView(section);
            for(int row=0;row<5;row++){LinearLayout line=new LinearLayout(c);for(int col=0;col<4;col++){int at=page*20+row*4+col;if(at>=filtered.size()){line.addView(new View(c),new LinearLayout.LayoutParams(0,Ui.dp(c,92),1));continue;}AppCatalog.Entry app=filtered.get(at);AppTile tile=new AppTile(c,app.label,Collections.singletonList(app.icon),accent,false);tile.setOnClickListener(v->listener.open(app));tile.setOnLongClickListener(v->{listener.menu(app);return true;});line.addView(tile,new LinearLayout.LayoutParams(0,Ui.dp(c,96),1));}slab.addView(line);if((row+1)*4+page*20>=filtered.size())break;}
            scroll.addView(slab,new ScrollView.LayoutParams(-1,-2));pager.addView(scroll);
        }pager.goTo(0,false);
    }
}
