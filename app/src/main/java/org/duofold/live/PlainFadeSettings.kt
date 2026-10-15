package org.duofold.live
import android.content.SharedPreferences
internal object PlainFadeSettings {
 data class Control(val key:String,val label:String,val default:Float,val range:ClosedFloatingPointRange<Float>,val percent:Boolean=true)
 val controls=listOf(
  Control("start","Wipe start position",0f,0f..1f),
  Control("end","Wipe end position",1f,0f..1f),
  Control("begin","Start travel at motion",0f,0f .. .95f),
  Control("finish","Finish travel at motion",1f,.05f..1f),
  Control("easing","Wipe timing exponent",1f,.25f..3f,false),
  Control("softness","Wipe edge softness",.08f,.005f .. .5f),
  Control("opacity","Wipe darkness",1f,0f..1f),
  Control("bright_start","Brightness at motion start",1f,0f..1f),
  Control("bright_end","Brightness at handoff",0f,0f..1f),
  Control("dim_easing","Dimming timing exponent",.5f,.25f..3f,false)
 )
 fun key(panel:String,key:String)="plain_fade_${panel}_$key"
 fun value(p:SharedPreferences,panel:String,c:Control):Float=p.getFloat(key(panel,c.key),c.default).let{if(it.isFinite())it.coerceIn(c.range)else c.default}
 fun read(p:SharedPreferences,inner:Boolean):PlainFadeTuning {
  val panel=if(inner)"inner" else "cover"
  val v=controls.map{value(p,panel,it)}
  return PlainFadeTuning(v[0],v[1],v[2],maxOf(v[3],v[2]+.01f),v[4],v[5],v[6],v[7],v[8],v[9])
 }
 fun report(p:SharedPreferences):String=buildString{
  append("Plain Fade settings\n")
  for(panel in listOf("cover","inner")){
   append("$panel:\n")
   for(c in controls)append("${c.key}=${value(p,panel,c)}\n")
  }
  append("handoff_angle=${p.getFloat("handoff_angle",85f)}; open_threshold=${p.getFloat("open_threshold",172f)}\n")
 }
}
