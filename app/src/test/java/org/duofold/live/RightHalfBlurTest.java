package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class RightHalfBlurTest {
 // 2026-10-04 capture: the inner right half was the cover mirror blurred to 56 px at the 101 deg switch, then the
 // native inner screen at 0 px right after it (the glass shader leaves the inner right half transparent).
 @Test public void matchesTheMirrorAtTheSwitch(){assertEquals(56f,RightHalfBlur.radius(101,101,172,56),0.01f);}
 @Test public void maxBetweenTheCloseAndOpenSwitchAngles(){assertEquals(56f,RightHalfBlur.radius(95,101,172,56),0.01f);}
 @Test public void clearAtFullyOpen(){assertEquals(0f,RightHalfBlur.radius(172,101,172,56),0.01f);assertEquals(0f,RightHalfBlur.radius(180,101,172,56),0.01f);}
 @Test public void easesMonotonicallyInBetween(){
  float prev=Float.MAX_VALUE;
  for(int a=101;a<=172;a++){float r=RightHalfBlur.radius(a,101,172,56);assertTrue(r<=prev+1e-4f);prev=r;}
  float mid=RightHalfBlur.radius(136.5f,101,172,56);assertTrue(mid>20&&mid<36);
 }
 @Test public void invalidInputsDisableBlur(){
  assertEquals(0f,RightHalfBlur.radius(Float.NaN,101,172,56),0);
  assertEquals(0f,RightHalfBlur.radius(120,101,172,0),0);
  assertEquals(0f,RightHalfBlur.radius(120,172,101,56),0);
 }
}
