package org.duofold.live;
final class GlassFramePolicy {
 static final long MAX_AGE_MS=350;
 /** Fold 7: one failed capture (panel step, 250 ms timeout) must not drop the glass to its empty fallback. */
 static final long KEEP_ON_FAILURE_MS=1500,RETRY_WITH_FRAME_MS=32;
 static boolean usable(long stamp,long now,int width,int height,int currentWidth,int currentHeight){
  return usable(stamp,now,width,height,currentWidth,currentHeight,MAX_AGE_MS);
 }
 static boolean usable(long stamp,long now,int width,int height,int currentWidth,int currentHeight,long maxAge){
  long age=now-stamp;
  return age>=0 && age<=maxAge && width>0 && height>0 && width==currentWidth && height==currentHeight;
 }
 /** Age a rendered (not readiness-gating) glass frame may reach. Panel switches clear frames explicitly. */
 static long maxAgeFor(String model){return DeviceCompatibility.isFold7(model)?KEEP_ON_FAILURE_MS:MAX_AGE_MS;}
 /** On a failed capture keep the last good same-panel frame for a bounded time instead of clearing it. */
 static boolean keepOnFailure(boolean hasFrame,long ageMs){return hasFrame&&ageMs>=0&&ageMs<=KEEP_ON_FAILURE_MS;}
}
