package cn.gdcp.timetable

import android.animation.ValueAnimator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

/** HyperCeiler congratulations layout adapted to personal timetable setup. AGPL-3.0. */
@Composable internal fun SetupComplete(onFinish: () -> Unit, active:Boolean=true) {
    Box(Modifier.fillMaxSize()) {
        WelcomeFlow(Modifier.fillMaxSize(), {1f}, false, !ValueAnimator.areAnimatorsEnabled(), {0.38f}, true, running=active)
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(30f))
            Image(painterResource(R.drawable.ic_launcher),"个人课表应用图标",Modifier.size(90.dp).clip(RoundedCornerShape(24.dp)))
            Spacer(Modifier.height(20.dp))
            Text("个人课表",fontSize=38.sp,fontWeight=FontWeight.Bold,color=Color(0xFF57172C))
            Spacer(Modifier.height(38.dp))
            Text("设置完毕",fontSize=23.sp,color=Color(0xFF745665))
            Spacer(Modifier.weight(40f))
            Box(Modifier.fillMaxWidth().heightIn(min=52.dp).clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF40345F).copy(alpha=0.48f)).clickable(onClick=onFinish)
                .semantics { role=Role.Button },contentAlignment=Alignment.Center) {
                Text("开始使用",fontSize=18.sp,color=Color.White,modifier=Modifier.padding(14.dp))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
