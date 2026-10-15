package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class IncomingHoldLeaseTest {
 @Test public void slowCloseDoesNotSpendHandoffBudget(){IncomingHoldLease l=new IncomingHoldLease(100);assertFalse(l.expired(2100));assertTrue(l.handoff(2100));assertEquals(3900,l.deadline());assertFalse(l.expired(3899));assertTrue(l.expired(3900));}
 @Test public void repeatedEndpointCannotRenewHold(){IncomingHoldLease l=new IncomingHoldLease(0);assertTrue(l.handoff(500));assertFalse(l.handoff(2000));assertEquals(2300,l.deadline());}
 @Test public void abandonedPreparationExpires(){IncomingHoldLease l=new IncomingHoldLease(0);assertFalse(l.expired(9999));assertTrue(l.expired(10000));assertFalse(l.handoff(10000));}
 @Test public void lastMomentHandoffStillHasBound(){IncomingHoldLease l=new IncomingHoldLease(0);assertTrue(l.handoff(9999));assertEquals(11799,l.deadline());assertTrue(l.expired(11800));}
}
