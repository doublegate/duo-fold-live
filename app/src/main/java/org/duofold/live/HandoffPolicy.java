package org.duofold.live;
/** Works from either initial posture. Hysteresis prevents chatter around the handoff. */
final class HandoffPolicy {
 /** Cover hold requested at or below HOLD_ANGLE while closing, released at or above RELEASE_ANGLE: the
  *  single source for every site that must agree with this hysteresis, e.g. CloseReleaseGate's "reopening". */
 static final float HOLD_ANGLE=94,RELEASE_ANGLE=98;
 boolean cover=false;
 int update(float angle,boolean fresh,boolean interactive){
  if(!fresh||!interactive||!Float.isFinite(angle)){if(cover){cover=false;return -1;}return 0;}
  if(cover&&(angle>=RELEASE_ANGLE||angle<=0)){cover=false;return -1;}
  if(!cover&&angle>0&&angle<=HOLD_ANGLE){cover=true;return 1;}
  return 0;
 }
 void reset(){cover=false;}
 /** The system canceled our cover request: forget it so the next sample can request it again. */
 void onCanceled(){reset();}
}
