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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Independent Compose implementation of a glow / logo / delayed-button welcome sequence.
 * Reference implementation and differences are documented in android/WELCOME-ANIMATION.md.
 * Shader and motion parameters adapted from HyperCeiler under AGPL-3.0.
 * Copyright (C) 2023-2026 HyperCeiler Contributions; modified for Compose.
 */
@Composable
internal fun FirstWelcome(onContinue: () -> Unit, expandedContent: @Composable () -> Unit) {
    val colors = MiuixTheme.colorScheme
    var iconCenter by remember { mutableFloatStateOf(0.38f) }
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val activity = androidx.activity.compose.LocalActivity.current
    var completed by rememberSaveable { mutableStateOf(false) }
    val reduced = !ValueAnimator.areAnimatorsEnabled()
    val instant = completed || reduced
    val startSettled = remember { instant }
    val logo = remember { Animatable(if (instant) 1f else 0.5f) }
    val visibility = remember { Animatable(if (instant) 1f else 0f) }
    val controls = remember { Animatable(if (instant) 1f else 0f) }
    val glow = remember { Animatable(if (instant) 1f else 0f) }
    val exit = remember { Animatable(0f) }
    var buttonBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var expansionBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var leaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val welcomeLayer=androidx.compose.ui.graphics.rememberGraphicsLayer()
    val ease = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }
    LaunchedEffect(Unit) {
        if (instant) {
            logo.snapTo(1f); visibility.snapTo(1f); controls.snapTo(1f); glow.snapTo(1f)
            completed = true
        } else {
            launch { glow.animateTo(1f, tween(2200, easing = CubicBezierEasing(0.25f, 0f, 0.2f, 1f))) }
            launch { visibility.animateTo(1f, tween(900, delayMillis = 450, easing = ease)) }
            launch {
                controls.animateTo(1f, tween(450, delayMillis = 1340, easing = ease))
                completed = true
            }
            kotlinx.coroutines.delay(450)
            logo.animateTo(0.95f, tween(440, easing = ease))
            logo.animateTo(1f, tween(700, easing = ease))
            // Wait until the delayed controls are completely visible before preserving the end state.
        }
    }
    val revealed by remember { derivedStateOf { glow.value > 0.5f } }
    DisposableEffect(activity, dark, revealed) {
        activity?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
                isAppearanceLightStatusBars = revealed
                isAppearanceLightNavigationBars = revealed
            }
        }
        onDispose {
            activity?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { rootOrigin=it.positionInRoot() }.background(colors.background).then(if (leaving) Modifier.clearAndSetSemantics {} else Modifier)) {
        val welcomeHeight = maxHeight
        val welcomeWidth = maxWidth
        val backdropShape=devicePageShape()
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(Modifier.fillMaxSize().graphicsLayer {scaleX=1f-.06f*exit.value;scaleY=scaleX;shape=backdropShape;clip=true}.drawWithContent {
            if(!leaving) welcomeLayer.record {this@drawWithContent.drawContent()}
            drawLayer(welcomeLayer)
        }.then(if(leaving) Modifier.clearAndSetSemantics {} else Modifier)) {
        WelcomeFlow(Modifier.fillMaxSize(), { glow.value }, dark, reduced, { iconCenter }, startSettled, running=!leaving)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(30f))
            Image(
                painterResource(R.drawable.ic_launcher), contentDescription = "个人课表应用图标",
                modifier = Modifier.size(90.dp).onGloballyPositioned { coordinates ->
                    val center = coordinates.positionInRoot().y + coordinates.size.height / 2f
                    iconCenter = (center / with(density) { welcomeHeight.toPx() }).coerceIn(0f,1f)
                }.clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).graphicsLayer {
                    scaleX = logo.value; scaleY = logo.value; alpha = visibility.value
                }
            )
            Box(Modifier.weight(40f).fillMaxWidth()) {
                Text("个人课表", fontSize = 38.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF57172C),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top=20.dp).graphicsLayer {
                        scaleX = logo.value; scaleY = logo.value; alpha = visibility.value
                    })
                Box(Modifier.align(Alignment.BottomCenter).size(72.dp).onGloballyPositioned { c -> buttonBounds = c.boundsInRoot() }.graphicsLayer {
                    alpha = controls.value
                    scaleX = 0.9f + 0.1f * controls.value; scaleY = scaleX
                }.then(if (controls.value < 1f) Modifier.clearAndSetSemantics {} else Modifier)
                    .clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF511B35))
                    .clickable(enabled=controls.value==1f && !leaving) {
                        if(buttonBounds==androidx.compose.ui.geometry.Rect.Zero) return@clickable
                        expansionBounds=androidx.compose.ui.geometry.Rect(buttonBounds.left-rootOrigin.x,buttonBounds.top-rootOrigin.y,buttonBounds.right-rootOrigin.x,buttonBounds.bottom-rootOrigin.y)
                        leaving = true
                        scope.launch {
                            exit.animateTo(1f, tween(if (reduced) 0 else 440, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            onContinue()
                        }
                    }.semantics { contentDescription="开始使用，进入个人课表引导"; role=androidx.compose.ui.semantics.Role.Button },
                    contentAlignment=Alignment.Center) {
                    Canvas(Modifier.size(30.dp)) {
                        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.5.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round)
                        val path=androidx.compose.ui.graphics.Path().apply {
                            moveTo(size.width*0.08f,size.height*0.5f);lineTo(size.width*0.88f,size.height*0.5f)
                            moveTo(size.width*0.55f,size.height*0.17f);lineTo(size.width*0.88f,size.height*0.5f);lineTo(size.width*0.55f,size.height*0.83f)
                        }
                        drawPath(path,Color.White,style=stroke)
                    }
                }
            }
            Spacer(Modifier.weight(20f))
        }
        }
        CachedWelcomeExpansion({exit.value},expansionBounds,leaving) { expandedContent() }
    }
}
