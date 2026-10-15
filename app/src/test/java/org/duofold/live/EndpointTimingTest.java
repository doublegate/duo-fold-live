package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class EndpointTimingTest {
 @Test public void followsPanelAcrossRemapAndSeparatesSizeReadiness(){
  EndpointTiming t=new EndpointTiming(100,"inner","OPENING");
  t.sample(100,2,false,true,true);t.sample(125,1,false,true,true);
  t.sample(150,1,true,false,true);t.sample(175,2,true,false,true);t.sample(200,2,true,true,true);
  String s=t.report();assertTrue(s.contains("=25..75 ms"));assertTrue(s.contains("on D0=150 (+50 ms)"));assertTrue(s.contains("destination dimensions=200 (+100 ms)"));assertTrue(s.contains("noninteractive observed=false"));
 }
 @Test public void unknownIdentityDoesNotInventAnOffDuration(){
  EndpointTiming t=new EndpointTiming(100,"cover","CLOSING");
  t.sample(100,2,false,true,true);t.sample(125,-1,false,false,true);t.sample(150,1,true,true,false);t.sample(175,2,true,true,true);
  assertTrue(t.report().contains("bounds unavailable"));assertTrue(t.report().contains("noninteractive observed=true"));
 }
 @Test public void noOffSampleIsNotProofOfContinuity(){
  EndpointTiming t=new EndpointTiming(100,"inner","OPENING");t.sample(100,2,false,true,true);t.sample(150,2,true,true,true);
  assertTrue(t.report().contains("short gaps can be missed"));assertTrue(t.report().contains("gap=50 ms"));
 }
 @Test public void multipleOffEpisodesDoNotProduceInvalidBounds(){
  EndpointTiming t=new EndpointTiming(100,"inner","OPENING");t.sample(100,2,false,true,true);t.sample(125,1,false,true,true);t.sample(150,2,true,true,true);t.sample(175,1,true,true,true);
  assertTrue(t.report().contains("additional OFF episode=true"));assertTrue(t.report().contains("bounds unavailable"));
 }
}
