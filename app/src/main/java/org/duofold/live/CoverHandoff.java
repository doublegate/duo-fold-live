package org.duofold.live;
import android.os.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.Executor;
/** A process-owned request: Android releases it if this Shizuku process dies. */
final class CoverHandoff {
 private Object manager,owned; private Method cancel,request; private Class<?> requestType,callbackType;
 private int coverId=-1,innerId=-1;private long deferSince=0; private boolean innerHeld=false;
 // Display power only (plan A2); "unlocked" still decides whether to request at all.
 private volatile boolean screenOn=true;
 void screen(boolean on){screenOn=on;} private final HandoffPolicy policy=new HandoffPolicy();
 private final NativeContinuityProbe nativeProbe=new NativeContinuityProbe();
 synchronized boolean probeNative(){return nativeProbe.nativeVisible();}
 private final ContinuityProbePolicy probe=new ContinuityProbePolicy();
 synchronized boolean probeHolding(){return probe.holding();}
 synchronized String probeStatus(){return probe.status+"\n"+nativeProbe.report();}
 String status="Normal display control";
 private void init()throws Exception{
  if(manager!=null)return;
  DeviceCompatibility.requireEligible(Build.MODEL, Build.VERSION.SDK_INT);
  Class<?> type=Class.forName("android.hardware.devicestate.DeviceStateManager");
  Object candidate=type.getConstructor().newInstance();
  for(Object state:(List<?>)type.getMethod("getSupportedDeviceStates").invoke(candidate)){
   if("CONCURRENT_INNER_DEFAULT".equals(state.getClass().getMethod("getName").invoke(state)))innerId=(int)state.getClass().getMethod("getIdentifier").invoke(state);
   if("CONCURRENT_OUTER_DEFAULT".equals(state.getClass().getMethod("getName").invoke(state)))coverId=(int)state.getClass().getMethod("getIdentifier").invoke(state);
  }
  if(coverId<0)throw new IllegalStateException("Cover state missing");
  requestType=Class.forName("android.hardware.devicestate.DeviceStateRequest");
  callbackType=Class.forName("android.hardware.devicestate.DeviceStateRequest$Callback");
  request=type.getMethod("requestState",requestType,Executor.class,callbackType);cancel=type.getMethod("cancelStateRequest");manager=candidate;
 }
 synchronized void update(float angle,boolean fresh,boolean interactive,boolean direct,float openThreshold,long probeRequest){
  int test=probe.update(SystemClock.elapsedRealtime(),probeRequest,angle,fresh,interactive&&direct,owned!=null&&!innerHeld);
  nativeProbe.update(test==ContinuityProbePolicy.HOLD,angle,interactive);
  if(test==ContinuityProbePolicy.HOLD)return;
  if(test==ContinuityProbePolicy.FINISH){if(gatedRelease(angle,fresh))return;releaseOwned();return;}
  if(owned!=null){
   int next=DirectHandoffPolicy.next(innerHeld,angle,fresh,interactive,direct,openThreshold);
   if(next==DirectHandoffPolicy.RELEASE){if(CloseReleaseGate.gates(next,innerHeld)&&gatedRelease(angle,fresh))return;releaseOwned();return;}
   if(next==DirectHandoffPolicy.INNER){changeState(true);policy.reset();return;}
   if(next==DirectHandoffPolicy.COVER){changeState(false);policy.cover=owned!=null;return;}
   if(innerHeld)return;
  }
  if(!fresh||!interactive){if(gatedRelease(angle,fresh))return;releaseOwned();return;}
  int action=policy.update(angle,fresh,interactive);
  if(action<0){if(gatedRelease(angle,fresh)){policy.cover=true;return;}releaseOwned();return;}
  deferSince=0;
  if(action!=1)return;
  changeState(false);
 }
 private void changeState(boolean toInner){
  long identity=Binder.clearCallingIdentity();Object previous=owned;boolean previousInner=innerHeld;
  try{
   init();
   if(toInner && innerId<0)throw new IllegalStateException("Inner concurrent state missing");
   IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"device_state");
   Object service=Class.forName("android.hardware.devicestate.IDeviceStateManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
   Object info=Class.forName("android.hardware.devicestate.IDeviceStateManager").getMethod("getDeviceStateInfo").invoke(service);
   Object current=info.getClass().getField("currentState").get(info),base=info.getClass().getField("baseState").get(info);
   if(owned==null && !current.getClass().getMethod("getIdentifier").invoke(current).equals(base.getClass().getMethod("getIdentifier").invoke(base)))throw new IllegalStateException("Another display override is active");
   Object builder=requestType.getMethod("newBuilder",int.class).invoke(null,toInner?innerId:coverId);
   Object next=builder.getClass().getMethod("build").invoke(builder);
   Object callback=Proxy.newProxyInstance(callbackType.getClassLoader(),new Class<?>[]{callbackType},(proxy,m,args)->{
    if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);
    if(m.getName().equals("equals"))return proxy==args[0];
    if(m.getName().equals("toString"))return "DuoCoverHandoff";
    if(m.getName().equals("onRequestCanceled")){if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoState","request canceled by system");synchronized(this){if(owned==next){owned=null;innerHeld=false;policy.onCanceled();deferSince=0;status="Concurrent handoff request canceled by system";}}}return null;
   });
   owned=next;innerHeld=toInner;request.invoke(manager,next,(Executor)Runnable::run,callback);
   if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoState","request "+(toInner?"inner":"cover")+" concurrent state");
   status=toInner?"Direct concurrent handoff: inner primary; holding until fully open":"Cover held below handoff; switch at 98° or closed";
  }catch(Exception e){owned=previous;innerHeld=previousInner;releaseOwned();Throwable root=e;while(root.getCause()!=null)root=root.getCause();status="Handoff unavailable: "+root.getClass().getSimpleName()+": "+root.getMessage();}
  finally{Binder.restoreCallingIdentity(identity);}
 }
 /** True = keep the cover override for now (CloseReleaseGate). Only applies to the cover override. */
 private boolean gatedRelease(float angle,boolean fresh){
  if(owned==null||innerHeld)return false;
  long now=SystemClock.elapsedRealtime();
  boolean reopening=fresh&&Float.isFinite(angle)&&angle>=98;
  long deferred=deferSince==0?0:now-deferSince;
  // Cheap escapes first: the base-state query is a system_server call on the 4 ms poll path (plan C3).
  if(CloseReleaseGate.allow(false,screenOn,reopening,deferred)||!CloseReleaseGate.defer(true,BaseDeviceState.closed(),screenOn,reopening,deferred)){deferSince=0;return false;}
  if(deferSince==0){deferSince=now;if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoState","defer release: base state not CLOSED yet (angle="+angle+", fresh="+fresh+", screenOn="+screenOn+")");}
  return true;
 }
 /** External release (effect disallowed by the mode, dual mode toggled): gated like every other release (plan A1). */
 synchronized void releaseGated(float angle,boolean fresh){
  if(gatedRelease(angle,fresh))return;
  release();
 }
 /** Reader stop / Shizuku destroy / restart: wait, bounded, for CLOSED before cancelling (plan A3). */
 synchronized void releaseForTeardown(){
  long start=SystemClock.elapsedRealtime();
  while(CloseReleaseGate.waitBeforeTeardown(owned!=null&&!innerHeld,BaseDeviceState.closed(),screenOn,SystemClock.elapsedRealtime()-start)){
   try{Thread.sleep(BaseDeviceState.CACHE_MS);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}
  }
  if(BuildConfig.DIAGNOSTICS&&owned!=null)android.util.Log.i("DuoState","teardown release after "+(SystemClock.elapsedRealtime()-start)+" ms");
  release();
 }
 synchronized boolean active(){return owned!=null && !innerHeld;}
 synchronized void release(){probe.abort();nativeProbe.update(false,0,false);releaseOwned();}
 private void releaseOwned(){
  policy.reset();if(owned==null)return;long identity=Binder.clearCallingIdentity();
  try{if(BuildConfig.DIAGNOSTICS)android.util.Log.i("DuoState","cancel concurrent state request");cancel.invoke(manager);owned=null;innerHeld=false;status="Normal display control restored";}catch(Exception e){status="Display release pending";}finally{Binder.restoreCallingIdentity(identity);}
 }
}
