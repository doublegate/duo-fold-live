package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class IncomingHoldPolicyTest {
 @Test public void closingCapturesBeforeAppFirstReturn(){assertTrue(IncomingHoldPolicy.capture(true,60,172));assertTrue(IncomingHoldPolicy.capture(true,16,172));assertFalse(IncomingHoldPolicy.capture(true,15,172));assertFalse(IncomingHoldPolicy.capture(true,0,172));assertFalse(IncomingHoldPolicy.capture(true,61,172));}
 @Test public void openingUsesConfiguredEndpoint(){assertFalse(IncomingHoldPolicy.capture(false,139,172));assertTrue(IncomingHoldPolicy.capture(false,140,172));assertFalse(IncomingHoldPolicy.capture(false,172,172));assertFalse(IncomingHoldPolicy.capture(false,130,160));assertTrue(IncomingHoldPolicy.capture(false,133,160));assertFalse(IncomingHoldPolicy.capture(false,Float.NaN,172));}
}
