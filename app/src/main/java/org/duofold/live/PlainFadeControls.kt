package org.duofold.live
import android.content.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt

@Composable internal fun PlainFadeControls(prefs:SharedPreferences,restart:()->Unit){
 val context=LocalContext.current
 var revision by remember{mutableIntStateOf(0)}
 SettingsCard("Plain Fade controls","Separate wipe and brightness settings for each display. Saved when you release a slider."){
  Text("Position 0% is the starting edge; 100% is the opposite cover edge or the inner hinge. Motion runs from clear to handoff; unfolding on the inner screen plays the same path backward. Lower timing exponents move sooner; 1 is linear; higher values move later.",style=MaterialTheme.typography.bodySmall)
  Text("Brightness controls the dimming overlay, not your phone’s brightness setting. The inner display remains fully black before handoff. Other animation styles are unaffected.",style=MaterialTheme.typography.bodySmall)
  for(panel in listOf("cover","inner")){
   Text(if(panel=="cover")"Cover display" else "Inner display",style=MaterialTheme.typography.titleMedium)
   key(panel,revision){
    for(c in PlainFadeSettings.controls){
     var value by remember{mutableFloatStateOf(PlainFadeSettings.value(prefs,panel,c))}
     Text(c.label+" · "+if(c.percent)"${(value*100).roundToInt()}%" else String.format(java.util.Locale.US,"%.2f",value))
     Slider(value=value,onValueChange={value=it},valueRange=c.range,onValueChangeFinished={
      val edit=prefs.edit().putFloat(PlainFadeSettings.key(panel,c.key),value)
      if(c.key=="begin"){
       val finish=PlainFadeSettings.value(prefs,panel,PlainFadeSettings.controls[3])
       if(finish<=value)edit.putFloat(PlainFadeSettings.key(panel,"finish"),(value+.05f).coerceAtMost(1f))
      }
      if(c.key=="finish"){
       val begin=PlainFadeSettings.value(prefs,panel,PlainFadeSettings.controls[2])
       if(begin>=value)edit.putFloat(PlainFadeSettings.key(panel,"begin"),(value-.05f).coerceAtLeast(0f))
      }
      edit.apply();revision++;restart()
     })
    }
   }
   TextButton(onClick={
    val edit=prefs.edit();for(c in PlainFadeSettings.controls)edit.remove(PlainFadeSettings.key(panel,c.key))
    edit.apply();revision++;restart()
   }){Text("Reset $panel controls")}
  }
  Button(onClick={
   context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Plain Fade settings",PlainFadeSettings.report(prefs)))
   android.widget.Toast.makeText(context,"Plain Fade settings copied",android.widget.Toast.LENGTH_SHORT).show()
  }){Text("Copy Plain Fade settings")}
 }
}
