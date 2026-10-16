package cn.gdcp.timetable

import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

class WidgetSettingsActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
  setResult(RESULT_CANCELED)
  if(id==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return}
  val prefs=getSharedPreferences("widget_dates",MODE_PRIVATE)
  enableEdgeToEdge()
  setContent {
   MiuixTheme(colors=if(isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
    var mode by rememberSaveable { mutableIntStateOf(prefs.getInt("mode_"+id,0)) }
    var fixed by rememberSaveable { mutableStateOf(prefs.getString("date_"+id,LocalDate.now(ScheduleData.ZONE).toString())!!) }
    Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
     Text("小部件设置",fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=20.dp,bottom=12.dp))
     Text("选择这个小部件显示哪天的课程。其他小部件不会随之改变。",modifier=Modifier.padding(bottom=12.dp))
     TextButton(text=if(mode==0) "✓ 今天" else "今天",onClick={mode=0},modifier=Modifier.fillMaxWidth())
     Text("跟随当前日期；当天课程全部结束后自动展示明天。",fontSize=13.sp)
     TextButton(text=if(mode==1) "✓ 明天" else "明天",onClick={mode=1},modifier=Modifier.fillMaxWidth())
     Text("每天自动展示下一天的课程。",fontSize=13.sp)
     TextButton(text=if(mode==2) "✓ 指定日期 · $fixed" else "选择具体日期",onClick={
      val chosen=LocalDate.parse(fixed)
      DatePickerDialog(this@WidgetSettingsActivity,{_,year,month,day->fixed=LocalDate.of(year,month+1,day).toString();mode=2},chosen.year,chosen.monthValue-1,chosen.dayOfMonth).show()
     },modifier=Modifier.fillMaxWidth())
     Text("指定日期保持不变；无课或学期外会显示相应提示。",fontSize=13.sp)
     Spacer(Modifier.height(12.dp))
     TextButton(text="保存并更新小部件",onClick={
      if(BaseWidgetProvider.providerFor(this@WidgetSettingsActivity,id)==null){Toast.makeText(this@WidgetSettingsActivity,"这个小部件已不存在，请重新添加",Toast.LENGTH_SHORT).show();finish()}
      else if(BaseWidgetProvider.saveDate(this@WidgetSettingsActivity,id,mode,fixed)){
       BaseWidgetProvider.updateAll(this@WidgetSettingsActivity)
       setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish()
      }else Toast.makeText(this@WidgetSettingsActivity,"日期未保存，请重试",Toast.LENGTH_SHORT).show()
     },modifier=Modifier.fillMaxWidth())
     TextButton(text="取消",onClick={finish()},modifier=Modifier.fillMaxWidth())
    }
   }
  }
 }
}
