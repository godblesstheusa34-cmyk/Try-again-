package com.chris.fluidhome;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Smoked translucent slab with cached gradients and a machined lower edge. */
public final class GlassDrawable extends Drawable {
    private final Paint body=new Paint(3),edge=new Paint(3),lip=new Paint(3);
    private final RectF rect=new RectF();private final float radius,density;private final int accent;private final boolean solid;
    public GlassDrawable(float density,float radius,int accent,boolean solid){
        this.density=density;this.radius=radius*density;this.accent=accent;this.solid=solid;
        edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(density);lip.setColor(0x9904080f);
    }
    @Override protected void onBoundsChange(Rect b){
        rect.set(b.left+density,b.top+density,b.right-density,b.bottom-density*3);
        body.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,solid?new int[]{0xff233142,0xff111a28,0xff182232}:new int[]{0xde293b4e,0xc8101b2b,0xda162333},new float[]{0,.52f,1},Shader.TileMode.CLAMP));
        edge.setShader(new LinearGradient(b.left,b.top,b.right,b.bottom,new int[]{0x77ffffff,0x225e7890,(accent&0xffffff)|0x55000000},new float[]{0,.55f,1},Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas){canvas.save();canvas.translate(0,density*3);canvas.drawRoundRect(rect,radius,radius,lip);canvas.restore();canvas.drawRoundRect(rect,radius,radius,body);canvas.drawRoundRect(rect,radius,radius,edge);}
    @Override public void getOutline(Outline o){o.setRoundRect(getBounds(),radius);}
    @Override public void setAlpha(int a){body.setAlpha(a);edge.setAlpha(a);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter f){body.setColorFilter(f);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
