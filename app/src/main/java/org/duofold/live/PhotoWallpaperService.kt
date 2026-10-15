package org.duofold.live

import android.graphics.*
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File

/** Diagnostic real wallpaper. No sensor substitution or simulated hinge readings. */
class PhotoWallpaperService:WallpaperService(){
 override fun onCreateEngine():Engine=PhotoEngine()
 inner class PhotoEngine:Engine(){
  private var photo:Bitmap?=null
  private var lastSample=0L
  private val angles=LiveAngles.Listener{value,_->
   val now=android.os.SystemClock.elapsedRealtime()
   if(isVisible && now-lastSample>=1500){
    lastSample=now
    RecoveryLog.add("Photo wallpaper hinge sample: angle=$value age=${LiveAngles.ageMs()} ms preview=$isPreview")
   }
  }
  override fun onCreate(holder:SurfaceHolder){
   super.onCreate(holder)
   LiveAngles.add(angles)
   val source=File(filesDir,"photo.jpg").takeIf{it.exists()}?:WallpaperFiles.photoFile(this@PhotoWallpaperService)
   photo=runCatching{WallpaperPhoto.decode(this@PhotoWallpaperService,android.net.Uri.fromFile(source))}.getOrNull()
   RecoveryLog.add("Photo wallpaper engine created; preview=$isPreview")
  }
  override fun onSurfaceChanged(holder:SurfaceHolder,format:Int,width:Int,height:Int){super.onSurfaceChanged(holder,format,width,height);draw()}
  override fun onVisibilityChanged(visible:Boolean){RecoveryLog.add("Photo wallpaper visible=$visible preview=$isPreview; angle age=${LiveAngles.ageMs()} ms");if(visible)draw()}
  private fun draw(){
   val canvas=try{surfaceHolder.lockCanvas()}catch(e:Exception){null}?:return
   try{
    canvas.drawColor(Color.rgb(20,24,32))
    photo?.let{bitmap->
     val scale=maxOf(canvas.width.toFloat()/bitmap.width,canvas.height.toFloat()/bitmap.height)
     val w=bitmap.width*scale;val h=bitmap.height*scale
     canvas.drawBitmap(bitmap,null,RectF((canvas.width-w)/2,(canvas.height-h)/2,(canvas.width+w)/2,(canvas.height+h)/2),Paint(Paint.FILTER_BITMAP_FLAG))
    }
   }finally{surfaceHolder.unlockCanvasAndPost(canvas)}
  }
  override fun onDestroy(){LiveAngles.remove(angles);photo?.recycle();photo=null;super.onDestroy()}
 }
}
