package org.duofold.live;
/** Corner landmarks verify orientation, hinge side and scale independently of the implementation. */
public final class LandscapePreviewGeometryChecks {
 private static void equal(float expected,float actual){if(Math.abs(expected-actual)>.02f)throw new AssertionError(expected+" != "+actual);}
 private static float[] mapped(float[] m,float x,float y){return new float[]{m[0]*x+m[2]*y+m[4],m[1]*x+m[3]*y+m[5]};}
 private static float[] natural(float[] point,int rotation,float nw,float nh){float x=point[0],y=point[1];switch(rotation){case 1:return new float[]{nw-y,x};case 2:return new float[]{nw-x,nh-y};case 3:return new float[]{y,nh-x};default:return point;}}
 public static void verify(){
  for(int[] panel:new int[][]{{2448,1848},{2208,1768},{2160,1856}})for(int sr:new int[]{1,3})for(int dr=0;dr<4;dr++){
   int nw=panel[0],nh=panel[1],dw=dr%2==0?nw:nh,dh=dr%2==0?nh:nw,sw=1972,sh=1248;
   float scale=Math.min(nw/2f/sh,nh/(float)sw),left=nw-sh*scale,top=(nh-sw*scale)/2;
   float[] m=LandscapePreviewLayout.transform(sw,sh,sr,dw,dh,dr,false);
   float[][] input={{0,0},{sw,0},{0,sh},{sw,sh}};
   float[][] expected=sr==1?new float[][]{{nw,top},{nw,top+sw*scale},{left,top},{left,top+sw*scale}}:new float[][]{{left,top+sw*scale},{left,top},{nw,top+sw*scale},{nw,top}};
   for(int i=0;i<4;i++){
    float[] logical=mapped(m,input[i][0],input[i][1]);
    if(logical[0]<-.02||logical[0]>dw+.02||logical[1]<-.02||logical[1]>dh+.02)throw new AssertionError("Source corner clipped");
    float[] n=natural(logical,dr,nw,nh);equal(expected[i][0],n[0]);equal(expected[i][1],n[1]);
    if(n[0]<nw/2f-.02)throw new AssertionError("Clean preview crosses physical hinge");
   }
   equal(scale,(float)Math.hypot(m[0],m[1]));equal(scale,(float)Math.hypot(m[2],m[3]));
   if(m[0]*m[3]-m[1]*m[2]<=0)throw new AssertionError("Clean image mirrored");
   if(sr==dr){equal(scale,m[0]);equal(scale,m[3]);equal(0,m[1]);equal(0,m[2]);}
   float[] reflection=LandscapePreviewLayout.transform(sw,sh,sr,dw,dh,dr,true);
   for(float[] c:input){float[] n=natural(mapped(reflection,c[0],c[1]),dr,nw,nh);if(n[0]<-.02||n[0]>left+.02||n[1]<-.02||n[1]>nh+.02)throw new AssertionError("Reflection misplaced");}
   if(reflection[0]*reflection[3]-reflection[1]*reflection[2]>=0)throw new AssertionError("Reflection must mirror across hinge");
  }
  // Downsampled hold and full-resolution live source must map the same landmarks.
  for(int sr:new int[]{1,3})for(int dr=0;dr<4;dr++){
   int dw=dr%2==0?2448:1848,dh=dr%2==0?1848:2448;
   float[] live=LandscapePreviewLayout.transform(2000,1200,sr,dw,dh,dr,false),hold=LandscapePreviewLayout.transform(300,180,sr,dw,dh,dr,false);
   for(float x:new float[]{0,.25f,1})for(float y:new float[]{0,.75f,1}){float[] a=mapped(live,2000*x,1200*y),b=mapped(hold,300*x,180*y);equal(a[0],b[0]);equal(a[1],b[1]);}
  }
 }
 public static void main(String[] args){verify();System.out.println("PASS: landscape corner orientation, physical halves, full content, uniform scale, both directions/all destination rotations, live/hold alignment");}
}
