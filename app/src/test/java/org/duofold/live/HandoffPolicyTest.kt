package org.duofold.live
import org.junit.Assert.*
import org.junit.Test
class HandoffPolicyTest {
 @Test fun coverStartRequiresNoFullOpenCycle(){val p=HandoffPolicy();assertEquals(0,p.update(0f,true,true));assertEquals(1,p.update(2f,true,true));assertEquals(0,p.update(90f,true,true));assertEquals(-1,p.update(98f,true,true))}
 @Test fun partialStartWorks(){assertEquals(1,HandoffPolicy().update(45f,true,true))}
 @Test fun directionAndJitter(){val p=HandoffPolicy();assertEquals(0,p.update(180f,true,true));assertEquals(0,p.update(95f,true,true));assertEquals(1,p.update(94f,true,true));for(a in listOf(93f,95f,97f))assertEquals(0,p.update(a,true,true));assertEquals(-1,p.update(98f,true,true));assertEquals(1,p.update(94f,true,true))}
 @Test fun closedAndStaleRelease(){for(end in listOf(0f,45f)){val p=HandoffPolicy();p.update(45f,true,true);assertEquals(-1,p.update(end,end==0f,true));assertEquals(1,p.update(3f,true,true))}}
 // After a system cancel of the cover request the policy must request again on the next sample.
 @Test fun systemCancelAllowsReRequest(){val p=HandoffPolicy();assertEquals(1,p.update(45f,true,true));p.onCanceled();assertEquals(1,p.update(44f,true,true))}
 // One source for the hysteresis; the release gate's "reopening", DirectHandoffPolicy and the probe use it.
 @Test fun hysteresisConstantsArePinned(){assertEquals(94f,HandoffPolicy.HOLD_ANGLE,0f);assertEquals(98f,HandoffPolicy.RELEASE_ANGLE,0f)
  assertEquals(DirectHandoffPolicy.INNER,DirectHandoffPolicy.next(false,HandoffPolicy.RELEASE_ANGLE,true,true,true,172f))
  assertEquals(DirectHandoffPolicy.COVER,DirectHandoffPolicy.next(true,HandoffPolicy.HOLD_ANGLE,true,true,true,172f))}
}
