package org.duofold.live;
/** Content routing is independent of the selected animation style. */
final class ScreenshotRenderPolicy {
 static boolean incoming(String source,String panel,boolean active){return active&&source!=null&&panel!=null&&!panel.isEmpty()&&!panel.equals("unknown")&&!ScreenshotStartupPolicy.ownsFrame(source,panel);}
 static int captureDisplay(boolean continuity,boolean screenshot,boolean active){return continuity||(screenshot&&active)?1:0;}
 static boolean classic(String legacy,String style,boolean screenshot){return "classic".equals(legacy)||"duo_classic".equals(style);}
}
