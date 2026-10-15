package org.duofold.live;
final class EndpointFadePolicy {
 static boolean mask(boolean closing,float angle,float open){return Float.isFinite(angle)&&(closing?angle<=15f:angle>=FoldThreshold.sanitize(open)-8f);}
}
