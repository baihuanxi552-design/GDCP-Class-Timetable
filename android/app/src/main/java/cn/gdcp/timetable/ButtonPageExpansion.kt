package cn.gdcp.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Shared implementation extracted unchanged from the add-course button expansion. */
@Composable internal fun ButtonPageExpansion(t:Float,source:Rect,startColor:Color,blocked:Boolean,content:@Composable ()->Unit) {
    val colors=MiuixTheme.colorScheme
    val density=LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.22f*t)))
        val fullWidth=constraints.maxWidth.toFloat();val fullHeight=constraints.maxHeight.toFloat()
        val pageWidth=maxWidth;val pageHeight=maxHeight
        val start=if(source==Rect.Zero) with(density) {Rect(fullWidth-84.dp.toPx(),fullHeight-80.dp.toPx(),fullWidth-20.dp.toPx(),fullHeight-16.dp.toPx())} else source
        val x=start.left*(1f-t);val y=start.top*(1f-t)
        val width=start.width+(fullWidth-start.width)*t;val height=start.height+(fullHeight-start.height)*t
        val shape=devicePageShape(t)
        Box(Modifier.offset {IntOffset(x.roundToInt(),y.roundToInt())}.size(with(density){width.toDp()},with(density){height.toDp()}).shadow(44.dp*(1f-t*.6f),shape,ambientColor=Color(0xFF353D49).copy(alpha=.75f),spotColor=Color(0xFF353D49).copy(alpha=.95f)).clip(shape).background(lerp(startColor,colors.background,t))) {
            Box(Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredSize(pageWidth,pageHeight).graphicsLayer {alpha=((t-.5f)/.5f).coerceIn(0f,1f)}.then(if(blocked) Modifier.clearAndSetSemantics {}.blockMotionInput() else Modifier)) { content() }
        }
    }
}
