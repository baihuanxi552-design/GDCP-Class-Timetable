package cn.gdcp.timetable

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private tailrec fun Context.mainActivity():MainActivity? = when(this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.mainActivity()
    else -> null
}
@Composable fun JwSyncSettings(onLogin:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current
    val activity=context.mainActivity()
    val prefs=remember(context) {JwSession.prefs(context)}
    var revision by remember {mutableIntStateOf(0)}
    DisposableEffect(prefs) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener {_,_->revision++}
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {prefs.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    val enabled=remember(revision) {JwSession.enabled(context)}
    val busy=remember(revision) {JwSession.busy()}
    val message=remember(revision) {prefs.getString("jwSyncMessage","开启后，每次打开应用或回到前台立即检查更新；也可以手动刷新。") ?: ""}
    val success=remember(revision) {prefs.getLong("jwSyncSuccess",0)}
    val pending=remember(revision) {prefs.getString("jwSyncPending",null)}
    val previous=remember(revision) {prefs.getString("jwPreviousPersonal",null)}
    val needsLogin=remember(revision) {prefs.getBoolean("jwSyncNeedsLogin",false)||!JwSession.hasSession(context)}
    val colors=MiuixTheme.colorScheme
    Card(insideMargin=PaddingValues(18.dp)) {
        TextButton(if(enabled) "自动更新课表：已开启" else "自动更新课表：未开启",{
            activity?.stopTimetableRefresh()
            JwSession.setEnabled(context,!enabled)
            if(!enabled)onLogin()
        },Modifier.fillMaxWidth().semantics {stateDescription=if(enabled) "已开启" else "已关闭"})
        Text("开启后加密保留本机登录会话，不保存账号密码；关闭时删除会话。仅打开应用时检查，不在后台定时运行。",fontSize=12.sp,color=colors.onSurfaceVariantSummary)
        if(success>0) {
            val time=java.time.Instant.ofEpochMilli(success).atZone(ScheduleData.ZONE).format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
            Text("上次成功检查：$time",fontSize=12.sp,modifier=Modifier.padding(top=10.dp))
        }
        Text(message,fontSize=13.sp,modifier=Modifier.padding(top=10.dp).semantics {liveRegion=LiveRegionMode.Polite})
        if(enabled) {
            if(needsLogin) TextButton("重新登录以更新课表",onLogin,Modifier.fillMaxWidth(),enabled=!busy)
            else Button(onClick={activity?.refreshPersonalTimetable()},enabled=!busy&&PersonalImport.active(context),modifier=Modifier.fillMaxWidth().padding(top=12.dp)) {
                Text(if(busy) "正在检查课表…" else "立即刷新课表")
            }
        }
        if(pending!=null) {
            val changes=remember(pending) {try {JwCourseDiff.describe(org.json.JSONObject(prefs.getString("jwSyncPendingBase","{}")!!),org.json.JSONObject(pending))} catch(e:Exception) {"请重新刷新后核对。"}}
            Text(changes,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
            Button(onClick={activity?.confirmTimetableRefresh()},enabled=!busy,modifier=Modifier.fillMaxWidth()) {Text("确认这些课表变化")}
            TextButton("取消本次更新",{activity?.dismissTimetableRefresh()},Modifier.fillMaxWidth(),enabled=!busy)
        }
        if(previous!=null) TextButton("恢复上一份教务课表并关闭自动更新",{activity?.restorePreviousTimetable()},Modifier.fillMaxWidth(),enabled=!busy)
    }
}
