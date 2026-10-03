package cn.gdcp.timetable
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import android.os.Build
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
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.theme.*

class WidgetSettingsActivity : ComponentActivity() {
 private val keys=listOf("auto","classic","xiaomi","vivo","oppo","honor","huawei","flyme")
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setResult(RESULT_CANCELED)
  val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
  if(BaseWidgetProvider.providerFor(this,id)==null){finish();return}
  val manager=AppWidgetManager.getInstance(this)
  val existing=manager.getAppWidgetOptions(id)
  enableEdgeToEdge()
  setContent {
   val dark=isSystemInDarkTheme()
   SideEffect {
    WindowCompat.getInsetsController(window,window.decorView).apply {isAppearanceLightNavigationBars=!dark;isAppearanceLightStatusBars=!dark}
    if(Build.VERSION.SDK_INT>=29) window.isNavigationBarContrastEnforced=false
    @Suppress("DEPRECATION")
    if(Build.VERSION.SDK_INT<35) {window.statusBarColor=android.graphics.Color.TRANSPARENT;window.navigationBarColor=android.graphics.Color.TRANSPARENT}
   }
   MiuixTheme(colors=if(dark) darkColorScheme() else lightColorScheme()) {
    var style by rememberSaveable { mutableIntStateOf(keys.indexOf(existing.getString("qingyuan_style","auto")).coerceAtLeast(0)) }
    var day by rememberSaveable { mutableIntStateOf(if(existing.getInt("qingyuan_day",0)==1) 1 else 0) }
    Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).verticalScroll(rememberScrollState()).safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
     Text("小部件设置",fontSize=30.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=20.dp))
     Text("选择显示日期与视觉样式。品牌风格不会改变小部件的系统注册方式。",fontSize=14.sp,color=MiuixTheme.colorScheme.onSurfaceVariantSummary)
     Card(Modifier.fillMaxWidth(),insideMargin=PaddingValues(0.dp)) {
      SuperDropdown(title="视觉样式",items=listOf("跟随手机品牌","清远蓝白","小米简洁风格","vivo 圆润风格","OPPO / 一加 / realme","荣耀风格","华为风格（安卓）","魅族风格"),selectedIndex=style,onSelectedIndexChange={style=it})
      SuperDropdown(title="显示日期",items=listOf("今天","明天"),selectedIndex=day,onSelectedIndexChange={day=it})
     }
     TextButton("保存并显示课表",{
      if(BaseWidgetProvider.providerFor(this@WidgetSettingsActivity,id)!=null){
       val options=Bundle();options.putString("qingyuan_style",keys[style]);options.putInt("qingyuan_day",day)
       manager.updateAppWidgetOptions(id,options);BaseWidgetProvider.updateAll(this@WidgetSettingsActivity)
       setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id))
      }
      finish()
     },Modifier.fillMaxWidth(),colors=ButtonDefaults.textButtonColorsPrimary())
     TextButton("取消",{finish()},Modifier.fillMaxWidth())
    }
   }
  }
 }
}
