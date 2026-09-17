package com.chris.fluidhome;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int INK=0xfff0f5fb,MUTED=0xffacbbc9;
    private Ui(){}
    public static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    public static TextView text(Context c,String value,float sp,int color){TextView v=new TextView(c);v.setText(value);v.setTextSize(sp);v.setTextColor(color);v.setFontFeatureSettings("tnum");v.setIncludeFontPadding(false);return v;}
    public static TextView button(Context c,String label,int accent,Runnable action){
        TextView b=text(c,label,14,INK);b.setGravity(Gravity.CENTER);b.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));b.setMinHeight(dp(c,48));
        b.setBackground(new GlassDrawable(c.getResources().getDisplayMetrics().density,16,accent,true));b.setOnClickListener(v->action.run());b.setFocusable(true);
        b.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});return b;
    }
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static void heading(LinearLayout into,String title,String sub,int accent){
        Context c=into.getContext();TextView eyebrow=text(c,sub.toUpperCase(java.util.Locale.getDefault()),11,accent);eyebrow.setLetterSpacing(.16f);into.addView(eyebrow);
        TextView heading=text(c,title,32,INK);heading.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,8);p.bottomMargin=dp(c,24);into.addView(heading,p);
    }
    public static LinearLayout.LayoutParams spaced(Context c,int height,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,height<0?height:dp(c,height));p.topMargin=dp(c,top);return p;}
    public static void material(View v,int accent,int radius,boolean solid){v.setBackground(new GlassDrawable(v.getResources().getDisplayMetrics().density,radius,accent,solid));v.setElevation(dp(v.getContext(),10));v.setOutlineAmbientShadowColor(0xff05090f);v.setOutlineSpotShadowColor(0xff05090f);}
}
