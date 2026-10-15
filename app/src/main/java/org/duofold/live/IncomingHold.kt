package org.duofold.live

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.*
import android.view.*
import android.widget.ImageView

/** Independent from the main overlay rebuild: follows one physical panel through D1 -> D0. */
internal object IncomingHold {
 // Parked in alpha.29 at the owner's request. No preference or UI can enable
 // the endpoint screenshot/black-mask experiments while this gate is false.
 private val endpointExperimentsEnabled=false
 private val main=Handler(Looper.getMainLooper())
 private val events=ArrayDeque<String>()
 private val windows=ArrayList<HoldWindow>()
 private var generation=0
 private var session=0L
 private var attempted=false
 private var engaged=false
 private var frame:GlassFrame?=null
 private var lease:IncomingHoldLease?=null
 private var blackFade=false
 private var maskActive=false
 private var revealAt=0L
 private var revealDuration=292L
 private var targetWidth=0
 private var targetHeight=0
 private var captureRequested=false
 private var context:Context?=null
 private var lastUpdate=0L
 private var task=-1
 private var physical=""
 private var closing=false
 private var captureAngle=0f
 private var pendingReady=false
 private var readyCount=0
 private var checkAt=0L
 private var lastNote=""
 private fun log(s:String){events.addLast("${SystemClock.elapsedRealtime()}: $s");while(events.size>30)events.removeFirst();RecoveryLog.add("Incoming hold: $s")}
 @JvmStatic fun report()=if(!endpointExperimentsEnabled)"Incoming endpoint transition: disabled internally (black fade and screenshot hold)" else "Incoming endpoint transition (commit/readiness are software evidence):\n"+events.joinToString("\n")
 @JvmStatic fun clear(reason:String){
  generation++;main.removeCallbacks(tick);windows.forEach{it.close()};windows.clear();frame=null;pendingReady=false;readyCount=0;lease=null;captureRequested=false;maskActive=false;revealAt=0
  if(engaged)log("RELEASE $reason; session=$session")
  engaged=false
 }
 @JvmStatic fun update(ctx:Context,info:Bundle?,angle:Float,fresh:Boolean,allowed:Boolean){
  if(!endpointExperimentsEnabled){
   if(engaged||windows.isNotEmpty())clear("endpoint experiments disabled internally")
   return
  }
  val now=SystemClock.elapsedRealtime();lastUpdate=now;context=ctx.applicationContext
  val prefs=ctx.getSharedPreferences("standalone",0)
  if(!allowed||!fresh||!prefs.getBoolean("enabled",false)||!prefs.getBoolean("dual",false)||!prefs.getBoolean("screenshot_camera_startup",false)||(!prefs.getBoolean("incoming_app_hold",true)&&!prefs.getBoolean("screenshot_endpoint_fade",false))||ctx.getSystemService(KeyguardManager::class.java).isKeyguardLocked||!ctx.getSystemService(PowerManager::class.java).isInteractive){clear("disabled, stale, or locked");return}
  val next=info?.getLong("session")?:0L
  if(next!=session){clear("new session");session=next;attempted=false}
  if(engaged&&((closing&&angle>captureAngle+10f)||(!closing&&angle<captureAngle-10f))){clear("direction reversed");return}
  if(engaged&&blackFade)captureAngle=if(closing)minOf(captureAngle,angle) else maxOf(captureAngle,angle)
  if(next==0L||info==null)return
  if(!engaged){
   if(attempted||!info.getBoolean("active"))return
   val selected=info.getInt("task",-1);val target=info.getString("physical")?:""
   if(selected<0||target.isEmpty())return
   attempted=true;task=selected;physical=target;closing=info.getBoolean("closing");captureAngle=angle
   blackFade=prefs.getBoolean("screenshot_endpoint_fade",false);targetWidth=info.getInt("width");targetHeight=info.getInt("height");revealDuration=FadeSettings.reveal(prefs.getFloat("fade_gradualness",FadeSettings.DEFAULT_GRADUALNESS))
   engaged=true;lease=IncomingHoldLease(now);checkAt=0;lastNote="";generation++
   try{
    val dm=ctx.getSystemService(DisplayManager::class.java)
    for(id in 0..1){val d=dm.getDisplay(id)?:throw IllegalStateException("Missing D$id");windows.add(HoldWindow(ctx,d,physical));windows.last().show()}
    log("PREATTACHED transparent hosts; session=$session task=$task; preparation limit=10000 ms")
   }catch(e:Exception){log("ATTACH failed ${e.javaClass.simpleName}");clear("attachment failure");return}
   main.post(tick)
  }
  if((closing&&angle<=0f)||(!closing&&angle>=FoldThreshold.sanitize(prefs.getFloat("open_threshold",172f))))handoff(now,"hinge endpoint")
  else if(!info.getBoolean("active"))handoff(now,"concurrent request ended")
  if(blackFade){
   if(EndpointFadePolicy.mask(closing,angle,prefs.getFloat("open_threshold",172f))||lease?.started()==true)activateMask()
   return
  }
  if(captureRequested||!info.getBoolean("active")||!IncomingHoldPolicy.capture(closing,angle,prefs.getFloat("open_threshold",172f)))return
  captureRequested=true;captureAngle=angle
  val epoch=generation;log("CAPTURE session=$session task=$task physical=$physical angle=$angle; handoff timeout starts at endpoint")
  GlassFrames.incoming(task,physical){captured,note->
   if(epoch!=generation)return@incoming
   if(captured==null||captured.physicalId!=physical||lease?.expired(SystemClock.elapsedRealtime())!=false){log("SKIP $note");clear("capture unavailable/late");return@incoming}
   frame=captured
   windows.forEach{it.prepare(captured)}
   log("BITMAP prepared in existing hosts; capture age=${SystemClock.elapsedRealtime()-captured.stamp} ms")
  }
 }
 private fun activateMask(){if(!maskActive){maskActive=true;windows.forEach{it.black()};log("BLACK mask armed; session=$session")}}
 private fun handoff(now:Long,reason:String){if(lease?.handoff(now)==true){if(blackFade)activateMask();log("HANDOFF $reason; release deadline=${lease?.deadline()}; session=$session")}}

 private val tick=object:Runnable{override fun run(){
  if(!engaged)return
  val ctx=context?:return;val now=SystemClock.elapsedRealtime()
  if(lease?.expired(now)!=false){clear(if(lease?.started()==true)"handoff deadline" else "preparation deadline");return}
  if(now-lastUpdate>750){clear("connection loss");return}
  if(ctx.getSystemService(KeyguardManager::class.java).isKeyguardLocked||!ctx.getSystemService(PowerManager::class.java).isInteractive){clear("locked or asleep");return}
  windows.forEach{it.refresh()}
  val primary=ctx.getSystemService(DisplayManager::class.java).getDisplay(0)
  if(primary!=null&&identity(primary)==physical)handoff(now,"incoming physical panel on D0")
  if(revealAt>0){
   val fraction=((now-revealAt).toFloat()/revealDuration).coerceIn(0f,1f)
   windows.forEach{it.reveal(1f-fraction*fraction*(3f-2f*fraction))}
   if(fraction>=1f){clear("black reveal complete");return}
  }
  val f=frame
  val width=if(blackFade)targetWidth else f?.width?:0
  val height=if(blackFade)targetHeight else f?.height?:0
  if(revealAt==0L&&(f!=null||(blackFade&&maskActive))&&width>0&&height>0&&primary!=null&&identity(primary)==physical&&primary.state==Display.STATE_ON&& !pendingReady && now>=checkAt){
   pendingReady=true;checkAt=now+150;val epoch=generation
   GlassFrames.incomingReady(task,physical,width,height){ready,note->
    if(epoch!=generation)return@incomingReady
    pendingReady=false
    if(note!=lastNote){lastNote=note;log("D0 readiness: $note")}
    readyCount=if(ready)readyCount+1 else 0
    if(readyCount>=2){log("Exact app HAS_DRAWN twice; releasing after 64 ms settle")
     main.postDelayed({if(epoch==generation){if(blackFade){if(revealAt==0L){revealAt=SystemClock.elapsedRealtime();log("BLACK reveal started; duration=$revealDuration ms")}}else clear("app drawn on incoming D0")}},64)
    }
   }
  }
  main.postDelayed(this,16)
 }}
 private fun identity(d:Display?):String=if(d==null)"" else runCatching{Display::class.java.getMethod("getUniqueId").invoke(d).toString()}.getOrDefault("")
 private class HoldWindow(ctx:Context,d:Display,val target:String){
  private var black=false
  private var opacity=1f
  private var captured:GlassFrame?=null
  private val id=d.displayId
  private val dm=ctx.getSystemService(DisplayManager::class.java)
  private val wc=ctx.createDisplayContext(d).createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null)
  private val dialog=Dialog(wc)
  private val image=ImageView(wc)
  private var wasVisible=false
  private var commitPending=false
  private var alive=true
  init{
   image.scaleType=ImageView.ScaleType.FIT_CENTER;image.alpha=1f
   image.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
   image.viewTreeObserver.addOnPreDrawListener{refresh();true}
   image.viewTreeObserver.addOnDrawListener{
    val f=captured
    if(black&&!commitPending){commitPending=true;image.viewTreeObserver.registerFrameCommitCallback{if(alive)log("BLACK COMMIT host D$id; visible=$wasVisible")}}
    if(f!=null&&!commitPending){commitPending=true;val visibleAtSubmit=wasVisible;image.viewTreeObserver.registerFrameCommitCallback{if(alive&&captured===f)log("COMMIT host D$id physical=$target; visibleAtSubmit=$visibleAtSubmit visibleNow=$wasVisible; age=${SystemClock.elapsedRealtime()-f.stamp} ms")}}
   }
   dialog.window?.apply{
    setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);setFormat(PixelFormat.TRANSLUCENT);setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
    attributes=attributes.apply{alpha=0f;windowAnimations=0;dimAmount=0f;setFitInsetsTypes(0);layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;title="Duo incoming app endpoint hold"}
   }
   dialog.setContentView(image)
  }
  fun show(){dialog.show();dialog.window?.setLayout(-1,-1);refresh()}
  fun black(){black=true;image.setBackgroundColor(Color.BLACK);image.invalidate();refresh()}
  fun reveal(alpha:Float){opacity=alpha;dialog.window?.let{w->w.attributes=w.attributes.apply{this.alpha=if(wasVisible)opacity else 0f}}}
  fun prepare(f:GlassFrame){captured=f;commitPending=false;image.setImageBitmap(f.bitmap);image.requestLayout();image.invalidate();refresh()}
  fun refresh(){
   val d=dm.getDisplay(id)
   val visible=(black||captured!=null)&&identity(d)==target
   if(visible!=wasVisible){wasVisible=visible;commitPending=false
    dialog.window?.let{w->w.attributes=w.attributes.apply{alpha=if(visible)opacity else 0f}}
    log("HOST D$id visible=$visible physical=${identity(d)} state=${d?.state}");image.invalidate()}
  }
  fun close(){alive=false;image.alpha=0f;image.setImageDrawable(null);runCatching{dialog.dismiss()}}
 }
}
