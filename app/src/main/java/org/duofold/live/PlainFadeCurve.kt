package org.duofold.live
import kotlin.math.pow

internal data class PlainFadeTuning(
 val start:Float=0f,val end:Float=1f,val begin:Float=0f,val finish:Float=1f,
 val easing:Float=1f,val softness:Float=.08f,val opacity:Float=1f,
 val brightStart:Float=1f,val brightEnd:Float=0f,val dimEasing:Float=.5f
)
/** Positions are distance from the wipe's origin; inner travel ends at the hinge. */
internal object PlainFadeCurve {
 fun progress(angle:Float,inner:Boolean,open:Float,handoff:Float):Float {
  if(!angle.isFinite())return 0f
  val end=FoldThreshold.sanitize(open);val split=handoff.coerceIn(1f,end-1f)
  return (if(inner)(end-angle)/(end-split) else angle/split).coerceIn(0f,1f)
 }
 fun front(p:Float,t:PlainFadeTuning):Float {
  val travel=((p-t.begin)/(t.finish-t.begin).coerceAtLeast(.01f)).coerceIn(0f,1f).pow(t.easing)
  return t.start+(t.end-t.start)*travel
 }
 fun wipe(p:Float,position:Float,t:PlainFadeTuning):Float {
  if(p<=0f||position<0f||position>1f)return 0f
  val f=front(p,t)
  // Fully clear at the starting edge and fully covered at the opposite edge.
  if(f<=0f)return 0f
  if(f>=1f)return t.opacity
  val x=((f-position)/t.softness.coerceAtLeast(.001f)+.5f).coerceIn(0f,1f)
  return x*x*(3f-2f*x)*t.opacity
 }
 fun dim(p:Float,t:PlainFadeTuning):Float {
  if(p<=0f)return 0f
  val brightness=t.brightStart+(t.brightEnd-t.brightStart)*p.coerceIn(0f,1f).pow(t.dimEasing)
  return 1f-brightness
 }
}
