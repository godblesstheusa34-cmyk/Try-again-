package com.chris.fluidhome;
import android.content.Context;
import android.opengl.*;
import android.os.SystemClock;
import android.util.Log;
import android.view.Choreographer;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Actual depth-buffered geometry behind native controls. No rendering while paused. */
public final class SceneSurface extends GLSurfaceView implements Choreographer.FrameCallback {
    private final SceneRenderer scene;private boolean active,ambient=true,reduced;private long lastDraw;
    public SceneSurface(Context c){super(c);setEGLContextClientVersion(3);setEGLConfigChooser(8,8,8,0,24,0);setPreserveEGLContextOnPause(true);scene=new SceneRenderer(c);setRenderer(scene);setRenderMode(RENDERMODE_WHEN_DIRTY);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    public void setAccent(int color){scene.accent=color;requestRender();}
    public void setPosition(float value){scene.page=value-1;if(active)requestRender();}
    public void setTilt(float x,float y){scene.tiltX=x;scene.tiltY=y;if(active)requestRender();}
    public void setMotion(boolean a,boolean r){ambient=a;reduced=r;scene.animate=a&&!r;requestRender();schedule();}
    public void resumeScene(){onResume();active=true;lastDraw=0;requestRender();schedule();}
    public void pauseScene(){active=false;Choreographer.getInstance().removeFrameCallback(this);onPause();}
    private void schedule(){Choreographer.getInstance().removeFrameCallback(this);if(active&&ambient&&!reduced)Choreographer.getInstance().postFrameCallback(this);}
    @Override public void doFrame(long time){if(!active||!ambient||reduced)return;if(time-lastDraw>=15_000_000L){lastDraw=time;requestRender();}Choreographer.getInstance().postFrameCallback(this);}
    private static final class SceneRenderer implements Renderer {
        private final Context context;private final float[] projection=new float[16],view=new float[16],vp=new float[16],model=new float[16];
        private int vertices,program,vertexArray,uModel,uVP,uAccent,uVariant;private long start;
        volatile int accent=0xff8fe6d5;volatile float page,tiltX,tiltY;volatile boolean animate=true;
        SceneRenderer(Context c){context=c.getApplicationContext();}
        @Override public void onSurfaceCreated(GL10 ignored,EGLConfig config){
            GLES30.glClearColor(.025f,.042f,.067f,1);GLES30.glEnable(GLES30.GL_DEPTH_TEST);GLES30.glEnable(GLES30.GL_CULL_FACE);start=SystemClock.uptimeMillis();
            try{
                int v=compile(GLES30.GL_VERTEX_SHADER,asset("scene.vert")),f=compile(GLES30.GL_FRAGMENT_SHADER,asset("scene.frag"));program=GLES30.glCreateProgram();GLES30.glAttachShader(program,v);GLES30.glAttachShader(program,f);GLES30.glLinkProgram(program);
                int[] status=new int[1];GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,status,0);GLES30.glDeleteShader(v);GLES30.glDeleteShader(f);if(status[0]==0)throw new IllegalStateException(GLES30.glGetProgramInfoLog(program));
                uModel=GLES30.glGetUniformLocation(program,"uModel");uVP=GLES30.glGetUniformLocation(program,"uViewProjection");uAccent=GLES30.glGetUniformLocation(program,"uAccent");uVariant=GLES30.glGetUniformLocation(program,"uVariant");
                FloatBuffer mesh=ribbon(128,20);vertices=mesh.capacity()/6;int[] handles=new int[1];GLES30.glGenVertexArrays(1,handles,0);vertexArray=handles[0];GLES30.glBindVertexArray(vertexArray);GLES30.glGenBuffers(1,handles,0);GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,handles[0]);GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,mesh.capacity()*4,mesh,GLES30.GL_STATIC_DRAW);
                GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,24,0);GLES30.glVertexAttribPointer(1,3,GLES30.GL_FLOAT,false,24,12);GLES30.glEnableVertexAttribArray(0);GLES30.glEnableVertexAttribArray(1);GLES30.glBindVertexArray(0);
            }catch(IOException|RuntimeException e){program=0;Log.e("FluidScene","3D scene unavailable; launcher controls remain usable",e);}
        }
        @Override public void onSurfaceChanged(GL10 ignored,int width,int height){GLES30.glViewport(0,0,width,height);Matrix.perspectiveM(projection,0,43f,width/(float)Math.max(1,height),.1f,40f);Matrix.setLookAtM(view,0,0,0,7,0,0,0,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);}
        @Override public void onDrawFrame(GL10 ignored){
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT|GLES30.GL_DEPTH_BUFFER_BIT);if(program==0)return;GLES30.glUseProgram(program);GLES30.glBindVertexArray(vertexArray);GLES30.glUniformMatrix4fv(uVP,1,false,vp,0);
            int color=accent;GLES30.glUniform3f(uAccent,((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f);float t=animate?(SystemClock.uptimeMillis()-start)*.001f:0;
            for(int i=0;i<3;i++){Matrix.setIdentityM(model,0);Matrix.translateM(model,0,.65f-page*.24f+tiltX*.045f,.95f+tiltY*.055f+(float)Math.sin(t*.19f)*.065f,-1.1f-i*.45f);Matrix.rotateM(model,0,37f+(float)Math.sin(t*.13f)*7f,1,0,0);Matrix.rotateM(model,0,-27f+tiltX*2,0,1,0);Matrix.rotateM(model,0,30f+i*19+t*(i%2==0?1.4f:-1.1f),0,0,1);float scale=1.45f-i*.18f;Matrix.scaleM(model,0,scale,scale,scale);GLES30.glUniformMatrix4fv(uModel,1,false,model,0);GLES30.glUniform1f(uVariant,i==0?.85f:.3f);GLES30.glDrawArrays(GLES30.GL_TRIANGLES,0,vertices);}
        }
        private String asset(String name)throws IOException{try(InputStream in=context.getAssets().open(name)){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
        private static int compile(int kind,String source){int shader=GLES30.glCreateShader(kind);GLES30.glShaderSource(shader,source);GLES30.glCompileShader(shader);int[] s=new int[1];GLES30.glGetShaderiv(shader,GLES30.GL_COMPILE_STATUS,s,0);if(s[0]==0){String error=GLES30.glGetShaderInfoLog(shader);GLES30.glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
        private static FloatBuffer ribbon(int around,int sides){
            float[] data=new float[around*sides*6*6];int at=0;int[][] corners={{0,0},{1,0},{1,1},{0,0},{1,1},{0,1}};
            for(int a=0;a<around;a++)for(int b=0;b<sides;b++)for(int[] corner:corners){double u=(a+corner[0])*Math.PI*2/around,v=(b+corner[1])*Math.PI*2/sides;float nx=(float)(Math.cos(u)*Math.cos(v)),ny=(float)(Math.sin(u)*Math.cos(v)),nz=(float)Math.sin(v),r=1.02f+.06f*(float)Math.cos(3*u);data[at++]=(float)Math.cos(u)*r+nx*.13f;data[at++]=(float)Math.sin(u)*r+ny*.13f;data[at++]=.12f*(float)Math.sin(3*u)+nz*.075f;data[at++]=nx;data[at++]=ny;data[at++]=nz;}
            FloatBuffer buffer=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();buffer.put(data).position(0);return buffer;
        }
    }
}
