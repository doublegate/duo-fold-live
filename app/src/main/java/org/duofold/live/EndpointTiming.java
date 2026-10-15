package org.duofold.live;
/** Pure sampled-state accounting. Never treats logical display IDs as physical identity. */
final class EndpointTiming {
 final long endpoint;final String target;final String label;
 private long previousAt=-1,offAt=-1,beforeOff=-1,lastOff=-1,onAt=-1,primaryAt=-1,readyAt=-1,maxGap;
 private int previousState=-1;private boolean unknown,slept,repeatedOff;
 EndpointTiming(long endpoint,String target,String label){this.endpoint=endpoint;this.target=target;this.label=label;}
 void sample(long at,int state,boolean primary,boolean sized,boolean interactive){
  if(previousAt>=0)maxGap=Math.max(maxGap,at-previousAt);
  if(!interactive)slept=true;
  if(state<0)unknown=true;
  if(primary&&primaryAt<0&&state>=0)primaryAt=at;
  if(primary&&sized&&state==2&&readyAt<0)readyAt=at;
  if(state==1&&onAt>=0)repeatedOff=true;
  if(state==1&&onAt<0){if(offAt<0){offAt=at;beforeOff=previousState==2?previousAt:-1;}lastOff=at;}
  if(state==2&&offAt>=0&&onAt<0)onAt=at;
  previousAt=at;previousState=state;
 }
 String report(){
  String off=offAt<0?"OFF not observed (short gaps can be missed)":
   "first OFF="+offAt+"; last OFF="+lastOff+"; first subsequent ON="+(onAt<0?"not observed":onAt);
  String bounds=offAt>=0&&onAt>=0&&!unknown&&!repeatedOff&&beforeOff>=0?
   "; first OFF episode estimated bounds (assuming no unsampled transitions)="+(lastOff-offAt)+".."+(onAt-beforeOff)+" ms":"; OFF duration bounds unavailable/incomplete";
  return "endpoint="+endpoint+" "+label+"; incoming physical="+target+"; "+off+bounds+
   "; first observed on D0="+(primaryAt<0?"not observed":primaryAt+" (+"+(primaryAt-endpoint)+" ms)")+
   "; first D0 ON with destination dimensions="+(readyAt<0?"not observed":readyAt+" (+"+(readyAt-endpoint)+" ms)")+
   "; additional OFF episode="+repeatedOff+"; noninteractive observed="+slept+"; missing/ambiguous identity observed="+unknown+"; maximum sample gap="+maxGap+" ms";
 }
}
