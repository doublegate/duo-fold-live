package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class PollCadenceTest {
 @Test public void fastModeYieldsAfterEachCompletedPoll(){assertEquals(1,PollCadence.delay(true,4,false));assertEquals(4,PollCadence.delay(true,0,false));assertEquals(2,PollCadence.delay(true,2,false));}
 @Test public void slowCallsNeverCreateCatchupBursts(){assertEquals(1,PollCadence.delay(true,100,false));}
 @Test public void capturedFrameCanTriggerImmediateHandoff(){assertEquals(0,PollCadence.delay(true,4,true));}
 @Test public void screenOffUsesSlowCadence(){assertEquals(496,PollCadence.delay(false,4,true));}
 @Test public void stillAtAnEndpointPollsSlowly(){assertEquals(33,PollCadence.delay(true,0,false,5000,180f));assertEquals(33,PollCadence.delay(true,0,false,5000,0f));}
 @Test public void recentMotionKeepsFastCadence(){assertEquals(4,PollCadence.delay(true,0,false,100,180f));}
 @Test public void nearTheSwitchAnglesStaysFastEvenWhenStill(){assertEquals(4,PollCadence.delay(true,0,false,5000,95f));}
 @Test public void stillHalfOpenAwayFromSwitchPollsSlowly(){assertEquals(33,PollCadence.delay(true,0,false,5000,60f));}
 @Test public void urgentStillWinsWhenStill(){assertEquals(0,PollCadence.delay(true,0,true,5000,180f));}
 @Test public void unknownAngleStaysFast(){assertEquals(4,PollCadence.delay(true,0,false,5000,Float.NaN));}
}
