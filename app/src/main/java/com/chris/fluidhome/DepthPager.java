package com.chris.fluidhome;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;

/** Native perspective paging; Android transforms hit-testing along with the views. */
public final class DepthPager extends ViewGroup implements Choreographer.FrameCallback {
    public interface Listener{void changed(float position,int selected);}
    private final Spring spring=new Spring(0);private final int slop;private final boolean drawer;
    private float downX,downY,start,tiltX,tiltY;
    private boolean dragging,vertical,widgetTouch,scheduled,reduced;
    private long lastFrame;private VelocityTracker velocity;private Listener listener;
    public DepthPager(Context c,boolean drawer){super(c);this.drawer=drawer;slop=ViewConfiguration.get(c).getScaledTouchSlop();setClipChildren(false);setClipToPadding(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    public void setListener(Listener l){listener=l;}
    public int selected(){return Math.max(0,Math.min(getChildCount()-1,Math.round(spring.target)));}
    public void setMotionReduced(boolean value){reduced=value;if(value)goTo(selected(),false);}
    public void setTilt(float x,float y){tiltX=x;tiltY=y;transform();}
    public void goTo(int index,boolean animated){
        if(getChildCount()==0)return;spring.target=Math.max(0,Math.min(getChildCount()-1,index));
        if(!animated||reduced||!ValueAnimator.areAnimatorsEnabled()){spring.snap(spring.target);stop();transform();}
        else if(!scheduled){scheduled=true;lastFrame=0;Choreographer.getInstance().postFrameCallback(this);}
    }
    private void stop(){scheduled=false;Choreographer.getInstance().removeFrameCallback(this);}
    @Override public void doFrame(long nanos){if(!scheduled)return;float dt=lastFrame==0?1f/120f:(nanos-lastFrame)/1e9f;lastFrame=nanos;boolean more=spring.step(dt);transform();if(more)Choreographer.getInstance().postFrameCallback(this);else scheduled=false;}
    @Override protected void onMeasure(int width,int height){int w=MeasureSpec.getSize(width),h=MeasureSpec.getSize(height);setMeasuredDimension(w,h);for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(h,MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount();i++)getChildAt(i).layout(0,0,r-l,b-t);transform();}
    private void transform(){
        int w=getWidth();if(w==0)return;int current=Math.max(0,Math.min(getChildCount()-1,Math.round(spring.value)));
        for(int i=0;i<getChildCount();i++){
            View page=getChildAt(i);float d=i-spring.value,a=Math.abs(d);
            page.setVisibility(a>1.4f?INVISIBLE:VISIBLE);page.setImportantForAccessibility(i==current?IMPORTANT_FOR_ACCESSIBILITY_AUTO:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            page.setCameraDistance(w*8f);page.setPivotX(w*.5f);page.setPivotY(getHeight()*.45f);
            page.setRotationY(reduced?0:d*(drawer?-52f:-24f)+tiltX*1.3f);page.setRotationX(reduced?0:tiltY*-1.2f);
            page.setTranslationX(d*w*(drawer&&!reduced?.84f:1));page.setTranslationY(reduced?0:a*Ui.dp(getContext(),drawer?28:8));
            page.setTranslationZ(-Math.min(a,2)*Ui.dp(getContext(),100));float scale=reduced?1:Math.max(.76f,1-a*(drawer?.13f:.045f));page.setScaleX(scale);page.setScaleY(scale);page.setAlpha(Math.max(0,1-a*(drawer?.48f:.32f)));
        }
        if(listener!=null)listener.changed(spring.value,current);
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();start=spring.value;dragging=vertical=false;widgetTouch=hitsWidget(this,downX,downY);break;
            case MotionEvent.ACTION_MOVE:
                float dx=e.getX()-downX,dy=e.getY()-downY;
                if(!dragging&&!vertical&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx))vertical=true;
                if(!vertical&&!widgetTouch&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.25f){dragging=true;stop();getParent().requestDisallowInterceptTouchEvent(true);return true;}break;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:dragging=vertical=false;break;
        }return dragging;
    }
    private static boolean hitsWidget(View view,float x,float y){
        if(view instanceof android.appwidget.AppWidgetHostView)return true;if(!(view instanceof ViewGroup))return false;ViewGroup group=(ViewGroup)view;
        for(int i=group.getChildCount()-1;i>=0;i--){View child=group.getChildAt(i);if(child.getVisibility()!=VISIBLE)continue;float[] point={x+group.getScrollX()-child.getLeft(),y+group.getScrollY()-child.getTop()};android.graphics.Matrix inverse=new android.graphics.Matrix();if(!child.getMatrix().invert(inverse))continue;inverse.mapPoints(point);if(point[0]>=0&&point[0]<child.getWidth()&&point[1]>=0&&point[1]<child.getHeight()&&hitsWidget(child,point[0],point[1]))return true;}return false;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(getChildCount()==0)return false;if(velocity==null)velocity=VelocityTracker.obtain();velocity.addMovement(e);
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();start=spring.value;stop();return true;
            case MotionEvent.ACTION_MOVE:spring.value=Spring.clamp(start-(e.getX()-downX)/Math.max(1,getWidth()),0,getChildCount()-1);spring.velocity=0;transform();return true;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                velocity.computeCurrentVelocity(1000);float v=e.getActionMasked()==MotionEvent.ACTION_CANCEL?0:-velocity.getXVelocity()/Math.max(1,getWidth());spring.velocity=Spring.clamp(v,-5,5);goTo(Spring.settle(spring.value,v,getChildCount()),true);velocity.recycle();velocity=null;dragging=vertical=false;getParent().requestDisallowInterceptTouchEvent(false);return true;
            default:return true;
        }
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);if(selected()>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);if(selected()<getChildCount()-1)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);}
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD){goTo(selected()+1,true);return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){goTo(selected()-1,true);return true;}return super.performAccessibilityAction(action,args);}
    @Override protected void onDetachedFromWindow(){stop();if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
}
