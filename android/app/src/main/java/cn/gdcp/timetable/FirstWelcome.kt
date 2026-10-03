package cn.gdcp.timetable

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Independent Compose implementation of a glow / logo / delayed-button welcome sequence.
 * Reference implementation and differences are documented in android/WELCOME-ANIMATION.md.
 * No HyperCeiler source, assets, shaders or vendor-private APIs are included.
 */
@Composable
internal fun FirstWelcome(onContinue: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    var completed by rememberSaveable { mutableStateOf(false) }
    val reduced = !ValueAnimator.areAnimatorsEnabled()
    val instant = completed || reduced
    val logo = remember { Animatable(if (instant) 1f else 0.5f) }
    val visibility = remember { Animatable(if (instant) 1f else 0f) }
    val controls = remember { Animatable(if (instant) 1f else 0f) }
    val glow = remember { Animatable(if (instant) 1f else 0f) }
    val exit = remember { Animatable(1f) }
    var leaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val ease = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    LaunchedEffect(Unit) {
        if (instant) {
            logo.snapTo(1f); visibility.snapTo(1f); controls.snapTo(1f); glow.snapTo(1f)
            completed = true
        } else {
            launch { glow.animateTo(1f, tween(2200, easing = ease)) }
            launch { visibility.animateTo(1f, tween(700, delayMillis = 60)) }
            launch {
                controls.animateTo(1f, tween(450, delayMillis = 1340, easing = ease))
                completed = true
            }
            logo.animateTo(0.95f, tween(440, easing = ease))
            logo.animateTo(1f, tween(700, easing = ease))
            // Wait until the delayed controls are completely visible before preserving the end state.
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background).graphicsLayer {
        alpha = exit.value
        scaleX = 1f + (1f - exit.value) * 0.025f; scaleY = scaleX
    }.then(if (leaving) Modifier.clearAndSetSemantics {} else Modifier)) {
        val topSpace = (maxHeight * 0.17f).coerceAtMost(140.dp)
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
            val g = glow.value
            val radius = size.minDimension * (0.36f + 0.32f * g)
            val center = Offset(size.width * 0.5f, size.height * 0.39f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF508AFF).copy(alpha = 0.24f * g), Color.Transparent), center, radius),
                radius, center
            )
            val second = center + Offset(radius * 0.32f, -radius * 0.23f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF8A6BFA).copy(alpha = 0.14f * g), Color.Transparent), second, radius * 0.72f),
                radius * 0.72f, second
            )
        }
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topSpace))
            Image(
                painterResource(R.drawable.ic_launcher), contentDescription = "个人课表应用图标",
                modifier = Modifier.size(112.dp).graphicsLayer {
                    scaleX = logo.value; scaleY = logo.value; alpha = visibility.value
                }
            )
            Spacer(Modifier.height(30.dp))
            Text("个人课表", fontSize = 34.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = logo.value; scaleY = logo.value; alpha = visibility.value
                    translationY = (1f - visibility.value) * 24.dp.toPx()
                })
            Spacer(Modifier.height(14.dp))
            Text("每一周，都有清晰的安排", fontSize = 16.sp, color = colors.onSurfaceVariantSummary,
                modifier = Modifier.graphicsLayer { alpha = visibility.value })
            Spacer(Modifier.height(64.dp))
            Column(Modifier.fillMaxWidth().graphicsLayer {
                alpha = controls.value
                scaleX = 0.9f + 0.1f * controls.value; scaleY = scaleX
                translationY = (1f - controls.value) * 16.dp.toPx()
            }.then(if (controls.value < 1f) Modifier.clearAndSetSemantics {} else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("登录教务系统，导入你的课程。\n已有课表将继续从本机读取。", fontSize = 15.sp,
                    color = colors.onSurfaceVariantSummary)
                Spacer(Modifier.height(26.dp))
                TextButton("开始使用  →", {
                    if (controls.value == 1f && !leaving) {
                        leaving = true
                        scope.launch {
                            exit.animateTo(0f, tween(if (reduced) 0 else 240, easing = ease))
                            onContinue()
                        }
                    }
                },
                    Modifier.fillMaxWidth().heightIn(min = 52.dp))
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}
