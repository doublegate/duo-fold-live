package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class EndpointFadePolicyTest {
 @Test public void preparesBeforeEitherEndpoint(){assertFalse(EndpointFadePolicy.mask(true,16,172));assertTrue(EndpointFadePolicy.mask(true,15,172));assertFalse(EndpointFadePolicy.mask(false,163,172));assertTrue(EndpointFadePolicy.mask(false,164,172));}
 @Test public void invalidAngleDoesNotMask(){assertFalse(EndpointFadePolicy.mask(true,Float.NaN,172));assertFalse(EndpointFadePolicy.mask(false,Float.POSITIVE_INFINITY,172));}
}
