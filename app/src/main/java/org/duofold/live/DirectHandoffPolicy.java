package org.duofold.live;
/** Direct-state experiment only: preserve the existing 98/94 degree hysteresis. */
final class DirectHandoffPolicy {
 static final int HOLD=0,INNER=1,COVER=2,RELEASE=3;
 static int next(boolean innerHeld,float angle,boolean fresh,boolean interactive,boolean enabled,float open){
  // An inner hold that reaches closed without passing 94 deg (sample gap) hands over to the cover hold,
  // whose release CloseReleaseGate defers until the base state is CLOSED; releasing 4 here went 4->2->0.
  if(innerHeld&&enabled&&fresh&&interactive&&Float.isFinite(angle)&&angle<=0)return COVER;
  if(!fresh||!interactive||!Float.isFinite(angle)||angle<=0||angle>=FoldThreshold.sanitize(open))return RELEASE;
  if(!enabled)return innerHeld?RELEASE:HOLD;
  if(innerHeld)return angle<=94?COVER:HOLD;
  return angle>=98?INNER:HOLD;
 }
}
