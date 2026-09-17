package com.chris.fluidhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class SpringTest {
    @Test public void settlesAt60And120Hz(){for(int hz:new int[]{60,120}){Spring s=new Spring(0);s.target=2;for(int i=0;i<hz*4;i++)s.step(1f/hz);assertEquals(2f,s.value,.001f);assertEquals(0f,s.velocity,.001f);}}
    @Test public void resumesAfterStallWithoutExploding(){Spring s=new Spring(0);s.target=2;s.step(5);assertTrue(Float.isFinite(s.value));assertTrue(s.value>=0&&s.value<=2);}
    @Test public void flingNeverSelectsAnAbsentPage(){assertEquals(0,Spring.settle(0,-1000,3));assertEquals(2,Spring.settle(2,1000,3));assertEquals(2,Spring.settle(1.1f,4,3));}
}
