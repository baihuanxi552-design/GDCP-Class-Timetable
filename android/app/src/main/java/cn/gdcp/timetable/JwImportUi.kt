package cn.gdcp.timetable

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.*

/** Shared Miuix shell; the school's login form remains on its own origin. */
class JwImportUi {
    private var message by mutableStateOf("登录成功后会自动进入首页。确认本学期，再解析全部教学周。")
    private var busy by mutableStateOf(false)
    private var preview by mutableStateOf<String?>(null)
    private var saveAction: Runnable? = null
    fun updateMessage(value: String) { message=value }
    fun updateBusy(value: Boolean) { busy=value }
    fun showPreview(value: String, save: Runnable) { preview=value;saveAction=save;message="解析完成，请核对后保存。" }
    fun dismissPreview(): Boolean {if(preview==null)return false;preview=null;saveAction=null;return true}
    fun mount(activity: ComponentActivity, web: WebView, parse: Runnable) {
        activity.setContent {
            val goBack={if(!dismissPreview()) {if(web.canGoBack()) web.goBack() else activity.finish()}}
            BackHandler {goBack()}
            val dark=isSystemInDarkTheme()
            MiuixTheme(colors=if(dark) darkColorScheme() else lightColorScheme()) {
                val colors=MiuixTheme.colorScheme
                Column(Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("教务系统",fontSize=28.sp,fontWeight=FontWeight.Bold)
                        TextButton("返回",{goBack()})
                    }
                    Text(message,fontSize=13.sp,color=colors.onSurfaceVariantSummary)
                    if(preview==null) {
                        AndroidView(factory={web},modifier=Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)))
                        Button(onClick={parse.run()},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text(if(busy) "正在解析全部教学周…" else "解析个人课表")}
                        Text("仅在本机保存课程，不保存账号密码。",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
                    } else {
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                            Text("确认个人课表",fontSize=24.sp,fontWeight=FontWeight.Bold)
                            Card(insideMargin=PaddingValues(24.dp)) {Text(preview!!,fontSize=17.sp)}
                            Text("保留自建课程及校区作息，替换上次导入的个人课表。",fontSize=14.sp,color=colors.onSurfaceVariantSummary)
                        }
                        Button(onClick={saveAction?.run()},modifier=Modifier.fillMaxWidth()) {Text("保存并查看课表")}
                        TextButton("返回教务页面",{preview=null;saveAction=null},Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
