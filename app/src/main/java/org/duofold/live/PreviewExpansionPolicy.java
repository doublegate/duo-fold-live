package org.duofold.live;
/** Timing is independent of hinge thresholds; it runs only after a physical-panel handoff. */
final class PreviewExpansionPolicy {
 static boolean fresh(long stamp,long now){return stamp>0 && now>=stamp && now-stamp<=500;}
 static float progress(long elapsed){float t=Math.max(0f,Math.min(1f,elapsed/240f));return 1f-(1f-t)*(1f-t)*(1f-t);}
 static float opacity(long elapsed,long readyElapsed){return opacity(elapsed,readyElapsed,120);}
 /** fadeMs 0 drops the hold at readiness (Fold 7 unified: the black reveal starts then, so a cross-fade double-exposes). */
 static float opacity(long elapsed,long readyElapsed,long fadeMs){
  if(elapsed>=1500)return 0f;
  long fadeStart=readyElapsed<0?1200:Math.max(0,readyElapsed);
  if(fadeMs<=0)return elapsed>=fadeStart?0f:1f;
  return Math.max(0f,Math.min(1f,1f-(elapsed-fadeStart)/(float)fadeMs));
 }
}
