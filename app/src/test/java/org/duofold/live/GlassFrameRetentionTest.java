package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class GlassFrameRetentionTest {
 // 2026-10-04 capture: 21 fallback frames were drawn AFTER the reveal because one failed capture
 // ("Panel changed during capture" on the 4->3 state step, "Screen off", 250 ms timeouts) cleared a good frame.
 @Test public void failedCaptureKeepsRecentGoodFrame(){
  assertTrue(GlassFramePolicy.keepOnFailure(true,0));
  assertTrue(GlassFramePolicy.keepOnFailure(true,GlassFramePolicy.KEEP_ON_FAILURE_MS));
 }
 @Test public void staleFrameIsClearedSoFailureStaysVisible(){
  assertFalse(GlassFramePolicy.keepOnFailure(true,GlassFramePolicy.KEEP_ON_FAILURE_MS+1));
  assertFalse(GlassFramePolicy.keepOnFailure(true,-1));
 }
 @Test public void nothingToKeepWithoutAFrame(){assertFalse(GlassFramePolicy.keepOnFailure(false,0));}
 @Test public void keptFrameRetriesQuickly(){assertTrue(GlassFramePolicy.RETRY_WITH_FRAME_MS<=50);}
 @Test public void fold7KeptFrameStaysRenderable(){
  long max=GlassFramePolicy.maxAgeFor("SM-F966U1");
  assertEquals(GlassFramePolicy.KEEP_ON_FAILURE_MS,max);
  assertTrue(GlassFramePolicy.usable(1000,1000+max,1248,1972,1248,1972,max));
  assertFalse(GlassFramePolicy.usable(1000,1001+max,1248,1972,1248,1972,max));
 }
 @Test public void otherDevicesKeepUpstreamWindow(){
  assertEquals(350,GlassFramePolicy.maxAgeFor("SM-F956U"));
  assertFalse(GlassFramePolicy.usable(1000,1100,1248,1972,2448,1848,1500));  // size still checked
 }
}
