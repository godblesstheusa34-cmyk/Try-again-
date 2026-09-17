package com.chris.fluidhome;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.*;
import java.util.List;

public final class AppTile extends LinearLayout {
    public AppTile(Context c,String label,List<Drawable> icons,int accent,boolean compact){
        super(c);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setPadding(Ui.dp(c,2),Ui.dp(c,7),Ui.dp(c,2),Ui.dp(c,7));
        FrameLayout face=new FrameLayout(c);Ui.material(face,accent,17,false);int size=Ui.dp(c,compact?50:58);addView(face,new LayoutParams(size,size));
        if(icons.isEmpty()){TextView plus=Ui.text(c,"+",25,Ui.MUTED);plus.setGravity(Gravity.CENTER);face.addView(plus,new FrameLayout.LayoutParams(-1,-1));}
        else if(icons.size()==1){ImageView image=new ImageView(c);image.setImageDrawable(copy(icons.get(0)));image.setScaleType(ImageView.ScaleType.FIT_CENTER);int p=Ui.dp(c,8);image.setPadding(p,p,p,p);face.addView(image,new FrameLayout.LayoutParams(-1,-1));}
        else {int s=Ui.dp(c,compact?18:21),inset=Ui.dp(c,8);for(int i=0;i<Math.min(4,icons.size());i++){ImageView image=new ImageView(c);image.setImageDrawable(copy(icons.get(i)));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(s,s);p.leftMargin=inset+(i%2)*(s+Ui.dp(c,1));p.topMargin=inset+(i/2)*(s+Ui.dp(c,1));face.addView(image,p);}}
        if(!compact){TextView name=Ui.text(c,label,11,Ui.INK);name.setGravity(Gravity.CENTER);name.setSingleLine();name.setEllipsize(TextUtils.TruncateAt.END);name.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LayoutParams p=new LayoutParams(-1,Ui.dp(c,20));p.topMargin=Ui.dp(c,6);addView(name,p);}
        setContentDescription(label);setFocusable(true);setClickable(true);
    }
    private static Drawable copy(Drawable d){return d.getConstantState()==null?d:d.getConstantState().newDrawable().mutate();}
    @Override public void setPressed(boolean p){super.setPressed(p);if(ValueAnimator.areAnimatorsEnabled())animate().scaleX(p?.93f:1).scaleY(p?.93f:1).setDuration(130).start();}
}
