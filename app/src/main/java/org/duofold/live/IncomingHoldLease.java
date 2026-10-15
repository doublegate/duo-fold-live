package org.duofold.live;
/** One preparation budget and one non-renewable handoff budget. */
final class IncomingHoldLease {
 private final long prepared;private long endpoint=-1;
 IncomingHoldLease(long now){prepared=now;}
 boolean handoff(long now){if(endpoint>=0||expired(now))return false;endpoint=now;return true;}
 boolean started(){return endpoint>=0;}
 long deadline(){return endpoint<0?prepared+10000:Math.min(endpoint+1800,prepared+11800);}
 boolean expired(long now){return now>=deadline();}
}
