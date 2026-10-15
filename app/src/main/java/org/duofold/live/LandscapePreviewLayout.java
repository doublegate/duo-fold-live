package org.duofold.live;
/** Logical cover -> natural panel -> logical inner. Physical hinge stays vertical in natural space. */
final class LandscapePreviewLayout {
 // SurfaceControl order: dsdx, dtdx, dtdy, dsdy, translationX, translationY.
 static float[] transform(int sw,int sh,int sr,int dw,int dh,int dr,boolean reflected){
  if(sw<=0||sh<=0||dw<=0||dh<=0||sr<0||sr>3||dr<0||dr>3)throw new IllegalArgumentException("Invalid panel geometry");
  float[] o=point(0,0,sw,sh,sr,dw,dh,dr,reflected),x=point(sw,0,sw,sh,sr,dw,dh,dr,reflected),y=point(0,sh,sw,sh,sr,dw,dh,dr,reflected);
  return new float[]{(x[0]-o[0])/sw,(x[1]-o[1])/sw,(y[0]-o[0])/sh,(y[1]-o[1])/sh,o[0],o[1]};
 }
 static float naturalX(float x,float y,int sw,int sh,int rotation){return rotation==1?sh-y:rotation==2?sw-x:rotation==3?y:x;}
 static float[] point(float x,float y,int sw,int sh,int sr,int dw,int dh,int dr,boolean reflected){
  float nw=(dr%2==0?dw:dh),nh=(dr%2==0?dh:dw),cw=(sr%2==0?sw:sh),ch=(sr%2==0?sh:sw);
  float nx=naturalX(x,y,sw,sh,sr),ny=sr==1?x:sr==2?sh-y:sr==3?sw-x:y;
  float scale=Math.min(nw/2f/cw,nh/ch),left=nw-cw*scale;
  // Clean content: full source, proportional fit, anchored at physical outside edge.
  // Frosted copy fills the remaining physical half, as in the established portrait path.
  nx=reflected?(cw-nx)*left/cw:left+nx*scale;
  ny=reflected?ny*nh/ch:(nh-ch*scale)/2f+ny*scale;
  if(dr==1)return new float[]{ny,nw-nx};
  if(dr==2)return new float[]{nw-nx,nh-ny};
  if(dr==3)return new float[]{nh-ny,nx};
  return new float[]{nx,ny};
 }
}
