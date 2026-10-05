package org.duofold.live;
import java.util.regex.*;
public final class AngleParser {
 // ~640 wallpaper lines/s reach this while polling: compile once, build the action tokens once per action.
 private static final Pattern ANGLE=Pattern.compile("mCurrentAngle(?:=|\\[)([0-9]+(?:\\.[0-9]+)?)");
 private static String tokenAction,tokenEq,tokenBracket;
 public static Float parse(String line,String action){
  if(!line.contains("SprWallpaper|FoldInteractive")||!line.contains("onCommand:"))return null;
  if(!action.equals(tokenAction)){tokenAction=action;tokenEq="action="+action+",";tokenBracket="action["+action+"]";}
  if(!(line.contains(tokenEq)||line.contains(tokenBracket))||!(line.contains("isVisible=true")||line.contains("isVisible[true]")))return null;
  Matcher m=ANGLE.matcher(line);
  if(!m.find())return null;
  float value=Float.parseFloat(m.group(1));return value>=0&&value<=180?value:null;
 }
}
