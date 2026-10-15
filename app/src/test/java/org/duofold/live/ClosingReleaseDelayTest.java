package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class ClosingReleaseDelayTest {
 @Test public void repeatedClosedSamplesCannotRenewDeadline(){
  ClosingReleaseDelay d=new ClosingReleaseDelay();Object request=new Object();
  assertTrue(d.arm(request,100));
  for(long now=101;now<1100;now++){assertFalse(d.arm(request,now));assertTrue(d.hold(request,now));}
  assertEquals(1100,d.deadline());assertFalse(d.hold(request,1100));
  d.clear();assertFalse(d.arm(request,1200));
 }
 @Test public void canceledTimerCannotMatchReplacementRequest(){
  ClosingReleaseDelay d=new ClosingReleaseDelay();Object old=new Object(),replacement=new Object();
  assertTrue(d.arm(old,100));d.clear();assertFalse(d.hold(old,200));
  assertTrue(d.arm(replacement,200));assertFalse(d.matches(old));assertTrue(d.hold(replacement,201));
 }
 @Test public void missingOwnershipAndReversedClockDoNotHold(){
  ClosingReleaseDelay d=new ClosingReleaseDelay();assertFalse(d.arm(null,100));
  Object request=new Object();assertTrue(d.arm(request,100));assertFalse(d.hold(request,99));
  d.clear();assertFalse(d.active());assertEquals(0,d.deadline());
 }
}
