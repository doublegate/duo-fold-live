package org.duofold.live;
import android.content.SharedPreferences;
/** Local testing override, deliberately separate from legacy dual-screen settings. */
final class ScreenshotModeAccess {
 static final String KEY="screenshot_testing_unlocked_353_30";
 static final long HOLD_MS=10000L;
 static boolean unlocked(SharedPreferences prefs){return prefs.getBoolean(KEY,false);}
 static void enforce(SharedPreferences prefs){
  if(!unlocked(prefs)&&prefs.getBoolean("dual",false))prefs.edit()
   .putBoolean("dual",false).putBoolean("cover_preview",true).commit();
 }
}
