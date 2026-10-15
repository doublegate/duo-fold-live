package org.duofold.live;
/** Fixed, non-renewable endpoint delay. Identity prevents an old timer releasing a new request. */
final class ClosingReleaseDelay {
 static final long DURATION_MS=1000;
 private Object owner,used;
 private long start;
 boolean arm(Object request,long now){
  if(request==null || request==used || owner!=null)return false;
  used=request;owner=request;start=now;return true;
 }
 boolean active(){return owner!=null;}
 boolean matches(Object request){return owner!=null && owner==request;}
 boolean hold(Object request,long now){return matches(request)&&now>=start&&now<deadline();}
 long started(){return start;}
 long deadline(){return active()?start+DURATION_MS:0;}
 void clear(){owner=null;start=0;}
}
