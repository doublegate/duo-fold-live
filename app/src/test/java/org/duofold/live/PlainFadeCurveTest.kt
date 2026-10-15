package org.duofold.live
import org.junit.Assert.*
import org.junit.Test
class PlainFadeCurveTest {
 @Test fun movingBoundaryTraversesEntireRegion(){
  val t=PlainFadeTuning()
  for(p in listOf(.05f,.25f,.5f,.75f,.95f)){
   assertEquals(p,PlainFadeCurve.front(p,t),.0001f)
   assertEquals(1f,PlainFadeCurve.wipe(p,p-.045f,t),.0001f)
   assertEquals(0f,PlainFadeCurve.wipe(p,p+.045f,t),.0001f)
  }
  assertEquals(0f,PlainFadeCurve.wipe(0f,0f,t),0f)
  assertEquals(1f,PlainFadeCurve.wipe(1f,1f,t),0f)
  assertEquals(0f,PlainFadeCurve.wipe(.5f,1.1f,t),0f)
 }
 @Test fun controlsAreIndependent(){
  val t=PlainFadeTuning(start=.1f,end=.9f,begin=.2f,finish=.8f,opacity=.4f,brightEnd=.7f)
  assertEquals(.1f,PlainFadeCurve.front(.1f,t),.0001f)
  assertEquals(.9f,PlainFadeCurve.front(.9f,t),.0001f)
  assertEquals(.4f,PlainFadeCurve.wipe(.9f,.5f,t),.0001f)
  assertEquals(.3f,PlainFadeCurve.dim(1f,t),.0001f)
  assertTrue(PlainFadeCurve.front(.25f,PlainFadeTuning(easing=.5f))>.25f)
 }
 @Test fun defaultDimmingAndHandoffPreserved(){
  val t=PlainFadeTuning()
  assertEquals(.5f,PlainFadeCurve.dim(.25f,t),.0001f)
  for(inner in listOf(false,true)){
   assertEquals(1f,PlainFadeCurve.progress(85f,inner,172f,85f),0f)
   assertEquals(0f,PlainFadeCurve.progress(if(inner)172f else 0f,inner,172f,85f),0f)
  }
 }
}
