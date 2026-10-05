package org.duofold.live;
/**
 * When to drop the cover-concurrent override (CONCURRENT_OUTER_DEFAULT) on the way closed. Releasing it while
 * the base state is still HALF_OPENED takes the device 5 -> 2 -> 0, and Samsung treats 2 -> 0 as a sleep
 * transition (sleepDevice=true): the screen starts to turn off and the cover blanks. Measured on SM-F966U1:
 * Duo's 2 deg threshold fires 9-103 ms before the hinge reports CLOSED, and a stale angle near closed released
 * it 1-3.5 s early. Releasing once the base state is CLOSED goes 5 -> 0 with no sleep.
 */
final class CloseReleaseGate {
 private CloseReleaseGate(){}
 static final long MAX_DEFER_MS=3000;
 static boolean allow(boolean baseClosed,boolean interactive,boolean reopening,long deferredMs){
  return baseClosed||reopening||!interactive||deferredMs>=MAX_DEFER_MS;
 }
 /** Whether a DirectHandoffPolicy action releases the cover override, and so must pass allow() first. */
 static boolean gates(int directAction,boolean innerHeld){
  return directAction==DirectHandoffPolicy.RELEASE&&!innerHeld;
 }
}
