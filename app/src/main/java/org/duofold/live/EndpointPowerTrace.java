package org.duofold.live;
import android.os.*;
import java.util.*;
/** Independent ring so task dumps cannot evict endpoint power evidence. */
final class EndpointPowerTrace {
 private final ArrayDeque<String> events=new ArrayDeque<>();
 private Handler handler;
 private final EndpointWindowTrace appWindows=new EndpointWindowTrace();
 private String observedPrimary="";private int watchedTask=-1;
 private String coverPhysical="",innerPhysical="";
 private int coverShort,coverLong,innerShort,innerLong,targetShort,targetLong;
 private long endpointAt;private EndpointTiming timing;
 private final ArrayDeque<String> summaries=new ArrayDeque<>();
 private synchronized void finishTiming(){if(timing!=null){summaries.addLast(timing.report());while(summaries.size()>6)summaries.removeFirst();timing=null;}}
 private synchronized void recordTiming(long now,String[] ids,int[] states,int[] widths,int[] heights,boolean interactive){
  if(timing==null)return;int match=-1,count=0;
  for(int i=0;i<2;i++)if(!timing.target.isEmpty()&&timing.target.equals(ids[i])){match=i;count++;}
  timing.sample(now,count==1?states[match]:-1,count==1&&match==0,count==1&&Math.min(widths[match],heights[match])==targetShort&&Math.max(widths[match],heights[match])==targetLong,interactive);
 }

 private long until,lastSample,maxGap;private String previous="";private volatile String apis="Not sampled";
 private Object dm,power;private Class<?> powerApi;private boolean inspected;
 private synchronized void add(String text){events.addLast(SystemClock.elapsedRealtime()+": "+text);while(events.size()>100)events.removeFirst();}
 void arm(String reason){queue(reason,3500,null);}
 void motion(String reason){queue(reason,30000,-2);}
 private synchronized void queue(String reason,long duration,Integer task){
  final long requestedAt=SystemClock.elapsedRealtime();
  if(handler==null){HandlerThread thread=new HandlerThread("Duo-endpoint-trace");thread.start();handler=new Handler(thread.getLooper());}
  handler.post(()->{until=SystemClock.elapsedRealtime()+duration;if(task!=null){
    finishTiming();lastSample=0;maxGap=0;endpointAt=0;coverPhysical="";innerPhysical="";watchedTask=-1;observedPrimary="";
    try{FixedTaskRoute observer=new FixedTaskRoute();add("TRACE ONLY: "+observer.prepare());watchedTask=observer.selectedTask();}
    catch(Exception e){add("Trace task selection unavailable: "+e.getClass().getSimpleName());}
   }
   if(reason.startsWith("ENDPOINT angle=")){
    finishTiming();endpointAt=requestedAt;
    boolean closing=reason.startsWith("ENDPOINT angle=0.0");
    targetShort=closing?coverShort:innerShort;targetLong=closing?coverLong:innerLong;
    synchronized(this){timing=new EndpointTiming(endpointAt,closing?coverPhysical:innerPhysical,(closing?"CLOSING":"OPENING")+" "+reason);}
   }
   previous="";add(reason+"; event="+requestedAt+"; observer queue="+(SystemClock.elapsedRealtime()-requestedAt)+" ms");handler.removeCallbacks(poll);handler.post(poll);});
 }
 private final Runnable poll=new Runnable(){public void run(){if(SystemClock.elapsedRealtime()>until){finishTiming();add("Sampling completed; maximum gap="+maxGap+" ms");return;}sample();handler.postDelayed(this,25);}};
 void sample(){
  long now=SystemClock.elapsedRealtime();if(now>until||now-lastSample<25)return;if(lastSample>0)maxGap=Math.max(maxGap,now-lastSample);lastSample=now;
  try{
   if(dm==null)dm=Class.forName("android.hardware.display.DisplayManagerGlobal").getMethod("getInstance").invoke(null);
   if(power==null){powerApi=Class.forName("android.os.IPowerManager");IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"power");power=Class.forName("android.os.IPowerManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);}
   if(!inspected){
    inspected=true;StringBuilder methods=new StringBuilder();
    for(String name:new String[]{"android.os.IPowerManager","android.hardware.display.IDisplayManager"})for(java.lang.reflect.Method method:Class.forName(name).getMethods()){
     String n=method.getName().toLowerCase(Locale.ROOT);
     if(n.contains("sleep")||n.contains("wake")||n.contains("fold")||n.contains("displaypower")||n.contains("displaystate")||n.contains("stayon"))methods.append(method.toGenericString()).append("; ");
    }
    apis=methods.toString();
   }
   boolean interactive=(Boolean)powerApi.getMethod("isInteractive").invoke(power);
   String[] ids=new String[2];int[] states={-1,-1},widths=new int[2],heights=new int[2];
   StringBuilder line=new StringBuilder("interactive=").append(interactive);
   try{line.append(" lastSleepReason=").append(powerApi.getMethod("getLastSleepReason").invoke(power));}catch(Exception unavailable){line.append(" lastSleepReason=unavailable");}
   for(int id=0;id<2;id++){
    Object d=dm.getClass().getMethod("getDisplayInfo",int.class).invoke(dm,id);
    line.append(" D").append(id).append('=');
    if(d==null){line.append("missing");continue;}
    Class<?> c=d.getClass();
    ids[id]=String.valueOf(c.getField("uniqueId").get(d));states[id]=c.getField("state").getInt(d);
    int width=c.getField("logicalWidth").getInt(d),height=c.getField("logicalHeight").getInt(d);
    widths[id]=width;heights[id]=height;

    if(id==0){String physical=String.valueOf(c.getField("uniqueId").get(d));if(!observedPrimary.isEmpty()&&!physical.equals(observedPrimary)){add("PRIMARY REMAP "+observedPrimary+" -> "+physical);appWindows.start(watchedTask,SystemClock.elapsedRealtime(),endpointAt);}observedPrimary=physical;}
    line.append(c.getField("uniqueId").get(d)).append(" state=").append(c.getField("state").get(d)).append(' ').append(c.getField("logicalWidth").get(d)).append('x').append(c.getField("logicalHeight").get(d));
   }
   // Learn only from a distinct, dimensionally consistent pair before remapping.
   if(endpointAt==0&&coverPhysical.isEmpty()&&ids[0]!=null&&ids[1]!=null&&!ids[0].equals(ids[1])&&widths[0]>0&&widths[1]>0&&heights[0]>0&&heights[1]>0){
    boolean a=Math.min(widths[0],heights[0])/(float)Math.max(widths[0],heights[0])>.7f;
    boolean b=Math.min(widths[1],heights[1])/(float)Math.max(widths[1],heights[1])>.7f;
    if(a!=b){int inner=a?0:1,cover=1-inner;coverPhysical=ids[cover];innerPhysical=ids[inner];
     coverShort=Math.min(widths[cover],heights[cover]);coverLong=Math.max(widths[cover],heights[cover]);
     innerShort=Math.min(widths[inner],heights[inner]);innerLong=Math.max(widths[inner],heights[inner]);
     add("TIMING identity cover="+coverPhysical+" inner="+innerPhysical);}
   }
   recordTiming(SystemClock.elapsedRealtime(),ids,states,widths,heights,interactive);
   String next=line.toString();if(!next.equals(previous)){previous=next;add(next);sampleDeviceState();}
  }catch(Exception e){synchronized(this){if(timing!=null)timing.sample(SystemClock.elapsedRealtime(),-1,false,false,true);}
   String next="Endpoint sample unavailable: "+e;if(!next.equals(previous)){previous=next;add(next);}}
 }
 private void sampleDeviceState(){
  try{
   IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"device_state");
   Object service=Class.forName("android.hardware.devicestate.IDeviceStateManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
   Object info=Class.forName("android.hardware.devicestate.IDeviceStateManager").getMethod("getDeviceStateInfo").invoke(service);
   Object current=info.getClass().getField("currentState").get(info),base=info.getClass().getField("baseState").get(info);
   String state="Device state current="+current.getClass().getMethod("getIdentifier").invoke(current)+" base="+base.getClass().getMethod("getIdentifier").invoke(base);
   if(!state.equals(lastDeviceState)){lastDeviceState=state;add(state);}
  }catch(Exception e){String state="Device state unavailable: "+e.getClass().getSimpleName();if(!state.equals(lastDeviceState)){lastDeviceState=state;add(state);}}
 }
 private String lastDeviceState="";
 synchronized String report(){return "Endpoint timing summaries (software observations; OFF bounds are sampling bounds, not optical blackout):\n"+String.join("\n",summaries)+(timing==null?"":"\nIN PROGRESS: "+timing.report())+"\nEndpoint power trace (25 ms target; polling gaps possible; software states):\n"+String.join("\n",events)+"\nAvailable power APIs (inspection only): "+apis+"\n"+appWindows.report();}
}
