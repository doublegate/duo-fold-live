package org.duofold.live;

/** Retain enough centered source pixels for every panel, including rotation. */
public final class WallpaperResolution {
 private WallpaperResolution() {}
 public static int[] decodeSize(int width,int height,int panelEdge){
  if(width<=0||height<=0||panelEdge<=0)throw new IllegalArgumentException("Invalid photo/display size");
  double scale=Math.min(1d,(double)panelEdge/Math.min(width,height));
  return new int[]{(int)Math.ceil(width*scale),(int)Math.ceil(height*scale)};
 }
}
