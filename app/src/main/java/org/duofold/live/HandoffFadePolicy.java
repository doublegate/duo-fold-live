package org.duofold.live;
/** Visual masking only. Never changes display state, thresholds or the underlying animation. */
final class HandoffFadePolicy {
 static final long REVEAL_MS=180,READY_TIMEOUT_MS=900,ON_SETTLE_MS=32;
 static final int UI_DRAW=0,GLASS_COMMITTED=1,ENDPOINT_COMMITTED=2;
 static final long COMMIT_SETTLE_MS=2;
 private boolean requireInnerGlass,requireCoverGlass;
 /** Fold 7 unified: the closing reveal also waits for a real cover glass frame (else it lands on the dark capture fallback). */
 void coverGlass(boolean require){requireCoverGlass=require;}
 private float openThreshold=172;
 private long readyAt=-1;
 String readiness="idle";
 void renderer(boolean requireGlass,float open){requireInnerGlass=requireGlass;openThreshold=FoldThreshold.sanitize(open);}
 private float smoothing,gradualness,filtered;
 private long filterTime=-1;
 void settings(float smoothing,float gradualness){this.smoothing=FadeSettings.smoothing(smoothing);this.gradualness=FadeSettings.gradualness(gradualness);}
 private float filtered(float target,long now){
  long dt=filterTime<0?0:Math.max(0,now-filterTime);filterTime=now;
  if(smoothing==0||target>=1||dt==0){filtered=target;return target;}
  filtered+=(target-filtered)*(float)(1-Math.exp(-dt/smoothing));
  if(Math.abs(filtered-target)<.001f)filtered=target;
  return filtered;
 }
 private Boolean primary;
 private long switched=-1,reveal=-1,blackSince=-1,onSince=-1;
 private String physical;
 private boolean mappingChanged;
 private float arrival;
 private boolean armed=true,expired=false;
 void reset(){filterTime=-1;filtered=0;primary=null;physical=null;mappingChanged=false;switched=reveal=blackSince=onSince=readyAt=-1;armed=true;expired=false;readiness="idle";}
 void mapping(String id){if(physical!=null&&!physical.equals(id))mappingChanged=true;physical=id;}
 boolean transitioning(){return switched>=0;}
 static float approach(boolean inner,float angle){return approach(inner,angle,0);}
 static float approach(boolean inner,float angle,float gradualness){return approach(inner,angle,gradualness,DEFAULT_INNER_BLACK,DEFAULT_COVER_BLACK);}
 /** innerBlack: angle where a closing inner panel is fully black; coverBlack: same for an opening cover panel. */
 static float approach(boolean inner,float angle,float gradualness,float innerBlack,float coverBlack){
  float width=FadeSettings.span(gradualness);
  float x=Math.max(0,Math.min(1,inner?(innerBlack+width-angle)/width:(angle-(coverBlack-width))/width));
  return x*x*(3-2*x);
 }
 static final float DEFAULT_INNER_BLACK=94,DEFAULT_COVER_BLACK=98;
 /** Samsung switches panels at different hinge angles per generation. Fold7 (SM-F966U1, One UI 8.5) measured 92-94 closing, 101-102 opening. */
 static float[] blackAnglesFor(String model){
  return DeviceCompatibility.isFold7(model)?new float[]{94,101}:new float[]{DEFAULT_INNER_BLACK,DEFAULT_COVER_BLACK};
 }
 /**
  * Upper bound on solid black after the destination panel turns ON. Duo draws only when a new angle
  * arrives, and on the Fold7 the wallpaper sends none for 0.3-0.5 s after a panel switch, so waiting
  * for a confirmed destination draw held black for 190-460 ms. Reveal at ON+80 ms there instead.
  */
 static long readyTimeoutFor(String model){return DeviceCompatibility.isFold7(model)?40:READY_TIMEOUT_MS;}
 /** Reveal (fade-in) length at 0% gradualness. Fold7 uses 100 ms so the switch reads as a short blink. */
 static long revealBaseFor(String model){return DeviceCompatibility.isFold7(model)?100:180;}
 private long revealBase=180;
 void revealBase(long ms){revealBase=ms>=60&&ms<=500?ms:180;}
 long revealMs(float gradualness){return Math.round(revealBase+320*FadeSettings.gradualness(gradualness));}
 /** Re-check cadence while VSYNC may be stopped. Every frame during a Fold7 switch, otherwise the original 80 ms. */
 static long tickDelayMs(boolean transitioning,boolean fastSwitchTicks){return transitioning&&fastSwitchTicks?16:80;}
 private long readyTimeout=READY_TIMEOUT_MS;
 void readyTimeout(long ms){readyTimeout=ms>=ON_SETTLE_MS&&ms<=READY_TIMEOUT_MS?ms:READY_TIMEOUT_MS;}
 /**
  * Opening (destination = inner) may wait longer than closing: on the Fold 7 the first inner glass frame
  * commits ~300-450 ms after the switch, and revealing earlier exposes the mirrored hold, which then cuts
  * over to the full display outside the black. Waiting keeps that cut-over inside the black. -1 = use readyTimeout.
  */
 static long openingReadyTimeoutFor(String model){return DeviceCompatibility.isFold7(model)?600:READY_TIMEOUT_MS;}
 private long openingReadyTimeout=-1;
 void openingReadyTimeout(long ms){openingReadyTimeout=ms>=ON_SETTLE_MS&&ms<=READY_TIMEOUT_MS?ms:-1;}
 private float innerBlack=DEFAULT_INNER_BLACK,coverBlack=DEFAULT_COVER_BLACK;
 void blackAngles(float inner,float cover){
  boolean ok=Float.isFinite(inner)&&Float.isFinite(cover)&&inner>=60&&inner<=130&&cover>=60&&cover<=130;
  innerBlack=ok?inner:DEFAULT_INNER_BLACK;coverBlack=ok?cover:DEFAULT_COVER_BLACK;
 }
 float opacity(long now,boolean inner,float angle,boolean valid,boolean on,long drawn,boolean drawnInner){
  return opacity(now,inner,angle,valid,on,drawn,drawnInner,UI_DRAW,-1);
 }
 float opacity(long now,boolean inner,float angle,boolean valid,boolean on,long drawn,boolean drawnInner,int kind,long captured){
  if(!valid||!Float.isFinite(angle)){reset();return 0;}
  if(primary==null)primary=inner;
  if(primary!=inner||mappingChanged){mappingChanged=false;primary=inner;switched=now;reveal=onSince=readyAt=-1;arrival=angle;armed=false;expired=false;blackSince=-1;readiness="waiting for panel ON";}
  if(switched>=0){
   filterTime=now;filtered=0;
   // A submitted frame can precede the physical ON interval. Do not spend the
   // reveal timer while the panel is OFF, or reuse a pre-ON draw callback.
   if(!on){onSince=reveal=readyAt=-1;readiness="waiting for panel ON";return 1;}
   if(onSince<0)onSince=now;
   if(reveal<0){
    if((inner&&requireInnerGlass)||(!inner&&requireCoverGlass)){
     boolean committed=drawnInner==inner&&drawn>=onSince&&drawn<=now;
     boolean freshContent=committed&&captured>=onSince&&captured<=drawn&&now-captured<=350;
     boolean glass=freshContent&&kind==GLASS_COMMITTED;
     boolean endpoint=inner&&freshContent&&kind==ENDPOINT_COMMITTED&&angle>=openThreshold;
     if(readyAt<0){
      if(glass||endpoint){readyAt=drawn;readiness=glass?"fresh content capture + glass frame committed":"fresh content capture + fully-open clear committed";}
      else readiness="waiting for fresh content capture + glass commit";
     }
     if(readyAt>=0&&now-readyAt>=COMMIT_SETTLE_MS)reveal=now;
    }else if(now-onSince>=ON_SETTLE_MS&&drawnInner==inner&&drawn>=onSince+ON_SETTLE_MS){reveal=now;readiness="destination draw";}
    // Emergency escape is not evidence of readiness. Keep a bounded recovery
    // instead of leaving the user's display black after a renderer failure.
    long limit=openingReadyTimeout>0&&(inner||requireCoverGlass)?openingReadyTimeout:readyTimeout;
    if(reveal<0&&now-onSince>=limit){reveal=now;readiness=limit<READY_TIMEOUT_MS?"reveal cap at ON+"+limit+" ms; destination readiness NOT confirmed":"TIMEOUT recovery; readiness NOT confirmed";}
   }
   if(reveal<0)return 1;
   float x=Math.min(1,(now-reveal)/(float)revealMs(gradualness));
   if(x<1)return 1-x*x*(3-2*x);
   switched=reveal=-1;
  }
  float mid=(innerBlack+coverBlack)/2;
  if(!armed&&(inner?(angle>=mid+FadeSettings.span(gradualness)||angle<=arrival-2):(angle<=mid-FadeSettings.span(gradualness)||angle>=arrival+2)))armed=true;
  float alpha=armed?approach(inner,angle,gradualness,innerBlack,coverBlack):0;
  if(alpha<=0){blackSince=-1;expired=false;}
  if(alpha>=.999f){if(blackSince<0)blackSince=now;if(now-blackSince>1200)expired=true;}else blackSince=-1;
  return expired?0:filtered(alpha,now);
 }
}
