package org.duofold.live;
import org.junit.Test;
import static org.junit.Assert.*;
public class WallpaperResolutionTest {
 @Test public void cameraPhotoRetainsNativeDetailAfterBothPanelCrops(){
  int[] size=WallpaperResolution.decodeSize(3000,4000,2448);
  assertArrayEquals(new int[]{2448,3264},size);
  assertTrue(Math.min(size[0],2448)>=2448);
  assertTrue(Math.min(size[1],2448)>=1972);
 }
 @Test public void landscapeRetainsInnerAndRotatedCoverDetail(){
  assertArrayEquals(new int[]{3264,2448},WallpaperResolution.decodeSize(4000,3000,2448));
 }
 @Test public void nativePhotoIsNotDownsampled(){
  assertArrayEquals(new int[]{2448,1848},WallpaperResolution.decodeSize(2448,1848,2448));
 }
 @Test public void smallPhotoIsNotArtificiallyUpscaled(){
  assertArrayEquals(new int[]{800,1200},WallpaperResolution.decodeSize(800,1200,2448));
 }
 @Test public void largerPanelsRaiseResolution(){
  assertArrayEquals(new int[]{3200,4800},WallpaperResolution.decodeSize(4000,6000,3200));
 }
}
