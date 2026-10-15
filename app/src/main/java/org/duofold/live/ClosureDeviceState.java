package org.duofold.live;
import android.os.*;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
/** Read-only, throttled shell-side base/current state snapshots. */
final class ClosureDeviceState {
 private Object service;private Method read;private long next,lastSample;private String previous="";
 private final ArrayDeque<String> events=new ArrayDeque<>();
 void sample(float raw,float filtered,boolean fresh,boolean dual,boolean unlocked){
  long now=SystemClock.elapsedRealtime();if(now<next)return;next=now+(fresh&&raw<=20?100:500);
  long gap=lastSample==0?0:now-lastSample;lastSample=now;
  long identity=Binder.clearCallingIdentity();
  try{
   if(service==null){IBinder b=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"device_state");service=Class.forName("android.hardware.devicestate.IDeviceStateManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,b);read=Class.forName("android.hardware.devicestate.IDeviceStateManager").getMethod("getDeviceStateInfo");}
   Object info=read.invoke(service);String state="route="+(dual?"screenshot":"normal")+" base="+label(info.getClass().getField("baseState").get(info))+" current="+label(info.getClass().getField("currentState").get(info))+" unlocked="+unlocked;
   if(!state.equals(previous)||gap>1000){previous=state;add(now+" ms DEVICE_STATE "+state+" raw="+raw+" filtered="+filtered+" fresh="+fresh+" sampleGapMs="+gap);}
  }catch(Exception e){String error="DEVICE_STATE_UNAVAILABLE "+e.getClass().getSimpleName();if(!error.equals(previous)){previous=error;add(now+" ms "+error);}next=now+5000;}
  finally{Binder.restoreCallingIdentity(identity);}
 }
 private String label(Object state)throws Exception{if(state instanceof Number)return state.toString();return state.getClass().getMethod("getIdentifier").invoke(state)+":"+state.getClass().getMethod("getName").invoke(state);}
 private synchronized void add(String s){events.addLast(s);while(events.size()>80)events.removeFirst();}
 synchronized String report(){return "Base/current fold state (sampled 100 ms near close, 500 ms otherwise; gaps are not exact transition times):\n"+String.join("\n",events);}
}
