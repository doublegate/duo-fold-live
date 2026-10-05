package org.duofold.live;
import android.view.SurfaceControl;
import java.util.ArrayList;
import java.util.List;
/**
 * Diagnostics: make Duo's own overlay layers visible to screen recordings so a transition can be
 * reviewed frame by frame as the user sees it. Normally they are setSkipScreenshot(true), which keeps
 * them out of Duo's own glass captures but also out of screenrecord. With
 * `adb shell setprop debug.duofold.record_visible 1` they are left visible and registered here instead,
 * and GlassCapture excludes them explicitly, so the glass still never samples its own overlays.
 * Lives in the Shizuku user-service process (HandoffFade, PreviewExpansion, InnerLiveMirror, GlassCapture).
 */
final class RecordVisible {
 private RecordVisible(){}
 private static final List<SurfaceControl> layers=new ArrayList<>();
 private static long readAt;private static boolean cached;
 /** Cached for 1 s: called from per-frame transaction paths. */
 static synchronized boolean enabled(){
  long now=android.os.SystemClock.uptimeMillis();
  if(now-readAt>=1000){readAt=now;
   try{cached="1".equals(Class.forName("android.os.SystemProperties").getMethod("get",String.class,String.class).invoke(null,"debug.duofold.record_visible",""));}
   catch(Exception e){cached=false;}}
  return cached;
 }
 /**
  * Replaces setSkipScreenshot(sc,true). Always sets the flag explicitly, so calling it on every update makes
  * a property change take effect on existing layers without restarting Duo (a force-stop would also remove
  * Duo from enabled_accessibility_services). Enabled: visible to recordings, excluded from glass capture.
  */
 static void hide(SurfaceControl.Transaction t,SurfaceControl sc)throws Exception{
  boolean record=enabled();
  SurfaceControl.Transaction.class.getMethod("setSkipScreenshot",SurfaceControl.class,boolean.class).invoke(t,sc,!record);
  synchronized(layers){if(record){if(!layers.contains(sc))layers.add(sc);}else layers.remove(sc);}
 }
 /** Valid registered layers to add to a capture's exclusion list. */
 static SurfaceControl[] exclusions(){
  synchronized(layers){layers.removeIf(sc->sc==null||!sc.isValid());return layers.toArray(new SurfaceControl[0]);}
 }
}
