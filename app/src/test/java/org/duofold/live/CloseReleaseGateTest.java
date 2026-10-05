package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloseReleaseGateTest {
 // Releasing the cover-concurrent override while the base state is still HALF_OPENED makes the device go
 // 5 -> 2 -> 0, and 2 -> 0 is a sleep transition (sleepDevice=true): the cover blanks. Release only once
 // the base state is CLOSED (5 -> 0, no sleep), when re-opening, or when the screen is already off.
 @Test public void releasesWhenBaseStateIsClosed(){
  assertTrue(CloseReleaseGate.allow(true,true,false,0));
 }
 @Test public void defersWhileHingeIsStillHalfOpen(){
  assertFalse(CloseReleaseGate.allow(false,true,false,0));        // Duo's 2 deg threshold hit first
  assertFalse(CloseReleaseGate.allow(false,true,false,2999));
 }
 @Test public void escapesAfterBoundedDefer(){
  assertTrue(CloseReleaseGate.allow(false,true,false,CloseReleaseGate.MAX_DEFER_MS));
 }
 @Test public void reopeningOrScreenOffReleaseImmediately(){
  assertTrue(CloseReleaseGate.allow(false,true,true,0));          // reopened past the handoff
  assertTrue(CloseReleaseGate.allow(false,false,false,0));        // not interactive: nothing to blank
 }
 // 2026-10-04 capture: the only sleep close released through DirectHandoffPolicy's angle<=0 RELEASE, which
 // bypassed the gate (0 defers logged all run). Every release of the cover override must be gated.
 @Test public void directReleaseOfCoverOverrideIsGated(){
  assertTrue(CloseReleaseGate.gates(DirectHandoffPolicy.RELEASE,false));
 }
 @Test public void innerOverrideAndNonReleaseActionsAreNotGated(){
  assertFalse(CloseReleaseGate.gates(DirectHandoffPolicy.RELEASE,true));
  assertFalse(CloseReleaseGate.gates(DirectHandoffPolicy.HOLD,false));
  assertFalse(CloseReleaseGate.gates(DirectHandoffPolicy.INNER,false));
 }
}
