package com.chris.fluidhome;
/** Damped spring with bounded integration steps, independent of display refresh rate. */
public final class Spring {
    public float value,target,velocity;
    public Spring(float value){snap(value);}
    public void snap(float value){this.value=target=value;velocity=0;}
    public boolean step(float elapsed){
        float remaining=Math.max(0,Math.min(elapsed,.05f));
        while(remaining>0){float dt=Math.min(remaining,1f/240f);velocity+=((target-value)*280f-velocity*29f)*dt;value+=velocity*dt;remaining-=dt;}
        if(Math.abs(target-value)<.0005f&&Math.abs(velocity)<.005f){value=target;velocity=0;return false;}
        return true;
    }
    public static float clamp(float value,float lo,float hi){return Math.max(lo,Math.min(hi,value));}
    public static int settle(float position,float velocity,int count){return Math.max(0,Math.min(count-1,Math.round(position+clamp(velocity*.12f,-.65f,.65f))));}
}
