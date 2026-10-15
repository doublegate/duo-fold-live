package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class AnimationModePolicyTest {
 @Test public void plainFadeUsesBlackWithoutAnyMirrors(){assertEquals("Plain Fade",AnimationModePolicy.label("plain_fade"));assertTrue(AnimationModePolicy.blackFade("plain_fade",false));assertFalse(AnimationModePolicy.mirrors("plain_fade"));assertFalse(AnimationModePolicy.blackFade("windowed",false));assertFalse(AnimationModePolicy.blackFade("duo_classic",false));assertTrue(AnimationModePolicy.blackFade("windowed",true));}
 @Test public void migrationPreservesDirectionAndClassic(){
  assertEquals("fold_only",AnimationModePolicy.initialBehavior(null,"fold_only"));
  assertEquals("unfold_only",AnimationModePolicy.initialBehavior(null,"unfold_only"));
  assertEquals("windowed",AnimationModePolicy.style("fold_only"));
  assertEquals("duo_classic",AnimationModePolicy.style("duo_classic"));
  assertEquals("both",AnimationModePolicy.initialBehavior(null,"duo_classic"));
  assertEquals("both",AnimationModePolicy.initialBehavior("both","fold_only"));
 }
 @Test public void behaviorWorksIndependentlyForEachStyle(){
  for(String style:new String[]{"windowed","duo_classic","plain_fade"}){
   assertEquals(style,AnimationModePolicy.style(style));
   for(String behavior:new String[]{"both","fold_only","unfold_only"}){
    AnimationModePolicy policy=new AnimationModePolicy();
    policy.update(behavior,0,true);
    assertEquals(!"fold_only".equals(behavior),policy.update(behavior,60,true));
    policy.update(behavior,172,true);
    assertEquals(!"unfold_only".equals(behavior),policy.update(behavior,120,true));
   }
  }
 }

 @Test public void transportIsNotAnAnimationStyle(){assertEquals("windowed",AnimationModePolicy.sanitize("screenshot"));assertEquals("Windowed Glass",AnimationModePolicy.label("screenshot"));}
 @Test public void defaultNeverChangesExistingDirectionBehavior(){AnimationModePolicy p=new AnimationModePolicy();for(float a:new float[]{0,60,98,179,130,94,0,Float.NaN})assertTrue(p.update("windowed",a,true));assertTrue(AnimationModePolicy.mirrors("windowed"));assertEquals("windowed",AnimationModePolicy.sanitize(null));}
 @Test public void classicEnablesWindowedGlassPreviewRoute(){AnimationModePolicy p=new AnimationModePolicy();assertTrue(p.update("duo_classic",110,true));assertTrue(AnimationModePolicy.mirrors("duo_classic"));}
 @Test public void foldOnlyTracksClosingAndIgnoresSmallJitter(){AnimationModePolicy p=new AnimationModePolicy();assertFalse(p.update("fold_only",0,true));assertFalse(p.update("fold_only",100,true));assertTrue(p.update("fold_only",179,true));assertTrue(p.update("fold_only",120,true));assertTrue(p.update("fold_only",121,true));assertFalse(p.update("fold_only",123,true));}
 @Test public void unfoldOnlyAndStaleSamples(){AnimationModePolicy p=new AnimationModePolicy();assertTrue(p.update("unfold_only",0,true));assertTrue(p.update("unfold_only",98,true));assertTrue(p.update("unfold_only",40,false));assertFalse(p.update("unfold_only",94,true));assertTrue(p.update("unfold_only",0,true));}
}
