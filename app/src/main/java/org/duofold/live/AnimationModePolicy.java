package org.duofold.live;
/** Additive mode gate. Windowed Glass always takes the established rendering path. */
final class AnimationModePolicy {
 static final String DEFAULT="windowed";
 static final String BOTH="both";
 static String style(String value){return "duo_classic".equals(value)||"plain_fade".equals(value)?value:DEFAULT;}
 static String behavior(String value){return "fold_only".equals(value)||"unfold_only".equals(value)?value:BOTH;}
 static String initialBehavior(String saved,String oldMode){return behavior(saved==null?oldMode:saved);}
 static String behaviorLabel(String value){switch(behavior(value)){case "fold_only":return "Fold-Only";case "unfold_only":return "Unfold-Only";default:return "Fold & Unfold";}}
 private float anchor=Float.NaN;
 private int direction;
 static String sanitize(String value){return "plain_fade".equals(value)||"duo_classic".equals(value)||"fold_only".equals(value)||"unfold_only".equals(value)?value:DEFAULT;}
 static boolean mirrors(String mode){return !"plain_fade".equals(mode);}
 static boolean blackFade(String mode,boolean debug){return "plain_fade".equals(mode)||debug;}
 static String label(String mode){switch(sanitize(mode)){case "plain_fade":return "Plain Fade";case "duo_classic":return "Duo Classic";case "fold_only":return "Fold-Only";case "unfold_only":return "Unfold-Only";default:return "Windowed Glass";}}
 boolean update(String mode,float angle,boolean fresh){
  mode=sanitize(mode);
  if(fresh&&Float.isFinite(angle)){
   if(!Float.isFinite(anchor))anchor=angle;
   if(angle-anchor>=2){direction=1;anchor=angle;}else if(angle-anchor<=-2){direction=-1;anchor=angle;}
   if(direction==1)anchor=Math.max(anchor,angle);else if(direction==-1)anchor=Math.min(anchor,angle);
   if(angle<=2){direction=1;anchor=angle;}else if(angle>=172){direction=-1;anchor=angle;}
  }
  if("fold_only".equals(mode))return direction==-1;
  if("unfold_only".equals(mode))return direction==1;
  return true;
 }
}
