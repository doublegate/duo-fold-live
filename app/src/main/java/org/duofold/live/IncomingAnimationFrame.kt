package org.duofold.live

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.runtime.*

/** Capture the exact incoming app before its animation exists; never recapture that effect. */
internal object IncomingAnimationFrame {
 var frame by mutableStateOf<GlassFrame?>(null);private set
 var status="Incoming animation idle";private set
 private var session=0L
 private var generation=0
 private var pending=false
 private var retryAt=0L
 private var started=0L
 private var physical=""
 @JvmStatic fun update(context:Context,info:Bundle?,fresh:Boolean,allowed:Boolean){
  val prefs=context.getSharedPreferences("standalone",0)
  val next=info?.getLong("session")?:0L
  val valid=fresh&&allowed&&prefs.getBoolean("enabled",false)&&prefs.getBoolean("dual",false)&&prefs.getBoolean("screenshot_camera_startup",false)&&info?.getBoolean("active")==true&&next>0L&&context.getSystemService(PowerManager::class.java).isInteractive&&!context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
  val target=info?.getString("physical")?:""
  if(!valid||next!=session||target!=physical){
   generation++;frame=null;pending=false;retryAt=0L;started=SystemClock.elapsedRealtime()
   session=if(valid)next else 0L;physical=if(valid)target else ""
   status=if(valid)"Waiting for exact incoming app" else "Incoming animation idle"
  }
  if(!valid||pending||frame!=null||target.isEmpty())return
  val now=SystemClock.elapsedRealtime()
  if(now<retryAt||now-started>2500L)return
  val task=info?.getInt("task",-1)?:-1
  if(task<0)return
  pending=true;val epoch=generation
  GlassFrames.incoming(task,target){captured,note->
   if(epoch!=generation)return@incoming
   pending=false
   if(captured!=null&&captured.physicalId==physical){
    frame=captured
    status="Clean incoming app frozen before animation; task=$task; D1; stamp=${captured.stamp}"
    RecoveryLog.add(status)
   }else{status="Incoming capture waiting: $note";retryAt=SystemClock.elapsedRealtime()+100L}
  }
 }
}
