package org.duofold.live

import android.app.*
import android.content.*
import android.os.*
import android.widget.*
import org.duofold.live.wallpaperlayer.WallpaperRestore

/** Opt-in diagnostic. The system picker controls application and destination. */
class WallpaperTestActivity:Activity(){
 private val main=Handler(Looper.getMainLooper())
 private lateinit var status:TextView
 private val prefs by lazy{getSharedPreferences("wallpaper_test",0)}
 private val ticker=object:Runnable{override fun run(){
  val component=WallpaperManager.getInstance(this@WallpaperTestActivity).wallpaperInfo?.component
  status.text="Primary wallpaper: ${component?.flattenToShortString()?:"Static / unavailable"}\nHinge replies: ${if(LiveAngles.fresh())"FRESH" else "STALE"}\nReply age: ${LiveAngles.ageMs()} ms\n${LiveAngles.status}\n\nFresh replies alone are insufficient: fold slowly and confirm the reported angle changes."
  main.postDelayed(this,500)
 }}
 override fun onCreate(state:Bundle?){super.onCreate(state)
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,32,32,32)}
  fun text(value:String){box.addView(TextView(this).apply{this.text=value;setPadding(0,12,0,12)})}
  fun button(label:String,action:()->Unit){box.addView(Button(this).apply{text=label;setOnClickListener{try{action()}catch(e:Exception){Toast.makeText(this@WallpaperTestActivity,e.message?:"Test unavailable",Toast.LENGTH_LONG).show()}}})}
  text("Photo wallpaper · experimental")
  text("This test may stop fold animations if Samsung’s hinge engine becomes inactive. Use your saved photo as a real wallpaper. In the system picker choose HOME ONLY; cancel if that choice is unavailable. Test both displays. No lock-screen changes are requested by this screen.")
  button("Preview / apply photo wallpaper"){
   val intent=Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,ComponentName(this,PhotoWallpaperService::class.java))
   if(!prefs.getBoolean("pending",false))prefs.edit().putBoolean("restore_layer",WallpaperRestore.enabled(this)).putBoolean("pending",true).commit()
   WallpaperRestore.setEnabled(this,false)
   RecoveryLog.add("Wallpaper experiment: old photo layer disabled; opening system picker")
   startActivity(intent)
  }
  text("After applying: return Home, slowly fold/unfold, then sleep and unlock on each display. Return here and copy the report. A successful test needs changing, fresh angles on Home—not merely a working preview.")
  status=TextView(this);box.addView(status)
  button("Copy test report"){
   val report=status.text.toString()+"\n"+(StandaloneService.instance?.report(includeHealthHistory=false)?:"Overlay disconnected")
   getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Wallpaper test",report))
   Toast.makeText(this,"Test report copied",Toast.LENGTH_SHORT).show()
  }
  button("1. Restore Samsung Home wallpapers"){
   startActivity(Intent(this,SetupActivity::class.java).putExtra("repair",true))
  }
  button("2. Restore previous photo layer"){
   val component=WallpaperManager.getInstance(this).wallpaperInfo?.component
   check(component?.packageName=="com.samsung.android.wallpaper.live"){"Restore Samsung wallpapers first"}
   if(prefs.getBoolean("pending",false)){
    WallpaperRestore.setEnabled(this,prefs.getBoolean("restore_layer",false));WallpaperRestore.resume(this)
    prefs.edit().remove("pending").remove("restore_layer").apply()
   }
   Toast.makeText(this,"Previous photo-layer preference restored",Toast.LENGTH_SHORT).show()
  }
  setContentView(ScrollView(this).apply{addView(box)})
 }
 override fun onResume(){super.onResume();main.post(ticker)}
 override fun onPause(){main.removeCallbacks(ticker);super.onPause()}
}
