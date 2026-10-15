package org.duofold.live;
/** Begin early enough to capture D1 before the closing app-first return at 15 degrees. */
final class IncomingHoldPolicy {
 static boolean capture(boolean closing,float angle,float open){return Float.isFinite(angle)&&(closing?angle>15f&&angle<=60f:angle>=Math.max(4f,FoldThreshold.sanitize(open)-32f)&&angle<FoldThreshold.sanitize(open));}
}
