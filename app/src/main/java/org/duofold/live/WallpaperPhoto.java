package org.duofold.live;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.view.Display;
import java.io.IOException;

/** Shared native-resolution import for setup, settings and wallpaper experiments. */
public final class WallpaperPhoto {
 private WallpaperPhoto() {}
 public static Bitmap decode(Context context,Uri uri)throws IOException{
  // Keep sufficient detail for the known inner panel even if Android temporarily
  // omits it while folded. Larger built-in panels raise this bound automatically.
  int edge=2448;
  DisplayManager manager=context.getSystemService(DisplayManager.class);
  if(manager!=null)for(Display display:manager.getDisplays()){
   if(!BuiltInPanel.accepts(display))continue;
   for(Display.Mode mode:display.getSupportedModes())
    edge=Math.max(edge,Math.max(mode.getPhysicalWidth(),mode.getPhysicalHeight()));
  }
  final int panelEdge=edge;
  return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(decoder,info,source)->{
   decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
   int[] size=WallpaperResolution.decodeSize(info.getSize().getWidth(),info.getSize().getHeight(),panelEdge);
   decoder.setTargetSize(size[0],size[1]);
   // All consumers center-crop. Discard only pixels outside every panel's crop,
   // bounding stored/decoded memory even for very wide panoramas.
   int w=Math.min(size[0],panelEdge),h=Math.min(size[1],panelEdge);
   int left=(size[0]-w)/2,top=(size[1]-h)/2;
   decoder.setCrop(new Rect(left,top,left+w,top+h));
  });
 }
}
