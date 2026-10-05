package org.duofold.live;
/**
 * Fold 7 unified renderer: compositor blur over the inner panel's right half while the inner panel is primary.
 * Before the switch the right half is the live cover mirror, blurred up to the mirror's maximum at the switch
 * angle (InnerLiveMirror); after it the glass shader leaves the right half transparent, exposing the sharp
 * native screen. This continues the same blur from the switch angle down to 0 at fully open, where the left
 * glass also clears, so the right half stays a flat full pane whose blur never jumps across the switch.
 */
final class RightHalfBlur {
 private RightHalfBlur(){}
 /** Max radius at or below {@code switchAngle}, smoothstep to 0 at {@code clearAngle}. */
 static float radius(float angle,float switchAngle,float clearAngle,float max){
  if(!Float.isFinite(angle)||!Float.isFinite(max)||max<=0||!(clearAngle>switchAngle))return 0;
  float limit=Math.min(max,PreviewBlurPolicy.MAX_RADIUS);
  float t=Math.max(0,Math.min(1,(clearAngle-angle)/(clearAngle-switchAngle)));
  return limit*t*t*(3-2*t);
 }
}
