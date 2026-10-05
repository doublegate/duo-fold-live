package org.duofold.live;
import android.os.*;
import android.graphics.Rect;
import android.view.SurfaceControl;
/** Live inner-to-cover compositor mirror; no bitmap capture, saved screenshots or protected-content overrides. */
final class InnerLiveMirror {
 private volatile SurfaceControl mirror;private int owner;
 // Read lock-free from the angle reader thread: attach() holds the monitor while awaiting its commit.
 private volatile SurfaceControl blur;
 private volatile java.lang.reflect.Method blurRadius;
 private volatile float blurStart=0,blurEnd=101,blurMax=56,blurTau=40,lastBlur=-1;
 private volatile float target=Float.NaN;private float shown=Float.NaN;private long tickAt;
 private final HandlerThread ticker=new HandlerThread("duo-preview-blur");
 private Handler tick;
 InnerLiveMirror(){ticker.start();tick=new Handler(ticker.getLooper());}
 // Ticker-thread only: frames, restarts and blur-layer release are serialized here, so a frame can never
 // set a radius on a layer close() already released. It idles once the glide reaches the hinge angle.
 private volatile boolean ticking;
 private final Runnable frame=new Runnable(){public void run(){
  SurfaceControl layer=blur;if(layer==null){ticking=false;return;}
  long now=SystemClock.uptimeMillis();
  float goal=target;
  shown=PreviewBlurPolicy.glide(shown,goal,now-tickAt,blurTau);tickAt=now;
  apply(layer,shown);
  if(!Float.isFinite(goal)||Math.abs(shown-goal)<.05f){ticking=false;if(Float.compare(goal,target)!=0)wake();return;}
  tick.postDelayed(this,8);
 }};
 private final Runnable restart=()->{tickAt=SystemClock.uptimeMillis()-8;frame.run();};
 private void wake(){if(!ticking&&blur!=null){ticking=true;tick.post(restart);}}
 String status="Cover live mirror idle";
 /** Tuning (max/start/glide) comes from BlurTuning, shared with the hold and the right-half blur. */
 /** Called for every fresh angle; follows the hinge with a compositor background blur over the preview. */
 void blur(float angle){target=angle;wake();}
 private void apply(SurfaceControl layer,float angle){
  java.lang.reflect.Method set=blurRadius;
  if(set==null||!layer.isValid())return;
  float r=PreviewBlurPolicy.radius(angle,blurStart,blurEnd,blurMax);
  if(!PreviewBlurPolicy.changed(lastBlur,r))return;
  try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){set.invoke(t,layer,Math.round(r));t.apply();lastBlur=r;if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoBlur","angle="+angle+" radius="+Math.round(r));}
  catch(Exception e){if(layer==blur){blurRadius=null;status="Preview blur unavailable: "+e.getClass().getSimpleName();}}
 }
 private Object service(String name,String stub)throws Exception{
  IBinder b=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,name);
  return Class.forName(stub).getMethod("asInterface",IBinder.class).invoke(null,b);
 }
 synchronized Bundle attach(int id,SurfaceControl parent,int width,int height,boolean allowed){return attach(id,parent,width,height,allowed,false);}
 /** halfPane (unified renderer, plan B3): fill exactly the right half so the pane boundary matches the post-switch glass. */
 synchronized Bundle attach(int id,SurfaceControl parent,int width,int height,boolean allowed,boolean halfPane){
  long identity=Binder.clearCallingIdentity();Bundle result=new Bundle();attaching.set(true);
  try{
   close();
   if(!allowed||parent==null||!parent.isValid())throw new IllegalStateException("Cover mirror not currently eligible");
   Object dm=service("display","android.hardware.display.IDisplayManager$Stub");
   Class<?> api=Class.forName("android.hardware.display.IDisplayManager");
   Object source=api.getMethod("getDisplayInfo",int.class).invoke(dm,0),dest=api.getMethod("getDisplayInfo",int.class).invoke(dm,1);
   if(source==null||dest==null)throw new IllegalStateException("Both panels must exist");
   int sw=source.getClass().getField("logicalWidth").getInt(source),sh=source.getClass().getField("logicalHeight").getInt(source);
   int dw=dest.getClass().getField("logicalWidth").getInt(dest),dh=dest.getClass().getField("logicalHeight").getInt(dest);
   if(java.util.Objects.equals(source.getClass().getField("uniqueId").get(source),dest.getClass().getField("uniqueId").get(dest)))throw new IllegalStateException("Refusing same-panel feedback mirror");
   Object wm=service("window","android.view.IWindowManager$Stub");
   mirror=SurfaceControl.class.getConstructor().newInstance();
   boolean accepted=(boolean)Class.forName("android.view.IWindowManager").getMethod("mirrorDisplay",int.class,SurfaceControl.class).invoke(wm,0,mirror);
   if(!accepted||!mirror.isValid())throw new IllegalStateException("WindowManager refused live mirror");
   if(Math.min(sw,sh)/(float)Math.max(sw,sh)>.7f || Math.min(dw,dh)/(float)Math.max(dw,dh)<=.7f)throw new IllegalStateException("Cover-to-inner preview only");
   float[] fit=halfPane?LiveMirrorLayout.rightHalf(sw,sh,width,height):LiveMirrorLayout.fit(sw,sh,width,height);
   blurStart=BlurTuning.start();blurMax=BlurTuning.max();
   blurEnd=HandoffFadePolicy.blackAnglesFor(android.os.Build.MODEL)[1];lastBlur=-1;blurTau=BlurTuning.smoothMs();shown=Float.NaN;
   try{
    SurfaceControl.Builder b=new SurfaceControl.Builder().setName("Duo preview progressive blur");
    SurfaceControl.Builder.class.getMethod("setEffectLayer").invoke(b);
    blur=b.build();
    blurRadius=SurfaceControl.Transaction.class.getMethod("setBackgroundBlurRadius",SurfaceControl.class,int.class);
   }catch(Exception e){if(blur!=null){blur.release();blur=null;}blurRadius=null;}
   java.util.concurrent.CountDownLatch committed=new java.util.concurrent.CountDownLatch(1);
   try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){
    SurfaceControl.Transaction.class.getMethod("setMatrix",SurfaceControl.class,float.class,float.class,float.class,float.class).invoke(t,mirror,fit[0],0f,0f,fit[0]);
    t.addTransactionCommittedListener(Runnable::run,committed::countDown);
    if(blur!=null){
     Rect bounds=new Rect(Math.round(fit[1]),Math.round(fit[2]),Math.round(fit[1]+sw*fit[0]),Math.round(fit[2]+sh*fit[0]));
     RecordVisible.hide(t,blur);  // always: never baked into Duo's own glass captures
     t.reparent(blur,parent).setLayer(blur,2).setCrop(blur,bounds).setPosition(blur,0,0).setVisibility(blur,true);
     blurRadius.invoke(t,blur,0);
    }
    t.reparent(mirror,parent).setLayer(mirror,1).setCrop(mirror,new Rect(0,0,sw,sh)).setPosition(mirror,fit[1],fit[2]).setVisibility(mirror,true).apply();
   }
   if(!committed.await(250,java.util.concurrent.TimeUnit.MILLISECONDS))throw new IllegalStateException("Right preview commit not yet confirmed; retrying");
   if(blur!=null){tick.removeCallbacks(frame);ticking=true;tick.post(restart);}
   if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoBlur","attach blur="+(blur!=null)+" setter="+(blurRadius!=null)+" max="+blurMax+" start="+blurStart+" end="+blurEnd+" tau="+blurTau);
   owner=id;status="Live cover → inner preview (right aligned; normal handoff)"+(blur!=null?"; progressive blur "+Math.round(blurMax)+" px from "+Math.round(blurStart)+"° to "+Math.round(blurEnd)+"°":"; progressive blur unavailable");result.putBoolean("ok",true);
  }catch(Exception e){close();Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();status="Cover mirror unavailable: "+cause.getClass().getSimpleName()+": "+cause.getMessage();}
  finally{attaching.set(false);if(parent!=null)parent.release();Binder.restoreCallingIdentity(identity);}
  result.putString("status",status);return result;
 }
 synchronized void detach(int id){if(owner==id)close();}
 boolean attached(){return mirror!=null;}
 private final java.util.concurrent.atomic.AtomicBoolean attaching=new java.util.concurrent.atomic.AtomicBoolean();
 /** From the angle poll: close unless an attach is in flight (the next poll, 4 ms later, closes it then). */
 void revoke(){if(attaching.get())return;if(mirror!=null||blur!=null)close();}
 synchronized void close(){
  if(blur!=null){try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){t.setVisibility(blur,false).reparent(blur,null).apply();}catch(Exception ignored){}SurfaceControl old=blur;blur=null;lastBlur=-1;tick.post(old::release);}
  if(mirror!=null){try(SurfaceControl.Transaction t=new SurfaceControl.Transaction()){t.setVisibility(mirror,false).reparent(mirror,null).apply();}catch(Exception ignored){}mirror.release();mirror=null;status="Cover live mirror released";}
  owner=0;
 }
}
