package org.duofold.live;
import android.app.Application;
import android.content.SharedPreferences;
/** Applies the release's requested mode once; later user selections remain intact. */
public final class DuoApplication extends Application {
 @Override public void onCreate(){
  super.onCreate();
  FoldAwakeDefault.persist(this);
  SharedPreferences prefs=getSharedPreferences("standalone",MODE_PRIVATE);
  if(!prefs.getBoolean("defaults_170_applied",false)){
   prefs.edit().putBoolean("cover_preview",true).putBoolean("dual",false)
    .putBoolean("defaults_170_applied",true).apply();
  }
  // Owner-requested 3.5.1 upgrade: apply only these two values once.
  // Runs before activities/services read their settings; later edits persist.
  if(!prefs.getBoolean("stretch_glass_defaults_351",false)){
   prefs.edit().putFloat("end_stretch",1.25f).putFloat("intensity",1f)
    .putBoolean("stretch_glass_defaults_351",true).apply();
  }
  // Set once; never overwrite a saved mode or the user's fade tuning on updates.
  if(!prefs.contains("animation_mode"))prefs.edit().putString("animation_mode",AnimationModePolicy.DEFAULT).apply();
  if(!prefs.getBoolean("preview_default_203",false))prefs.edit().putBoolean("cover_preview",true).putBoolean("preview_default_203",true).apply();
  new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> org.duofold.live.wallpaperlayer.WallpaperRestore.resume(this));
 }
}
