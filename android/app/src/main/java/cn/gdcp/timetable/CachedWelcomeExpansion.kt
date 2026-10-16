package cn.gdcp.timetable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Premeasure once; animate a cached render layer instead of relaying out a page each frame. */
@Composable internal fun CachedWelcomeExpansion(progress:()->Float,source:Rect,opening:Boolean,content:@Composable ()->Unit) {
    val layer=rememberGraphicsLayer()
    val density=LocalDensity.current
    val colors=MiuixTheme.colorScheme
    val finalShape=devicePageShape(1f)
    var ready by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        // Destination is prepared away from the touch surface before the welcome button is pressed.
        Box(Modifier.fillMaxSize().offset {IntOffset(-constraints.maxWidth*2,0)}.drawWithContent {
            if(!ready) {
                layer.record {this@drawWithContent.drawContent()}
                ready=true
            }
        }) {content()}
        if(opening && ready) Canvas(Modifier.fillMaxSize().blockMotionInput()) {
            val t=progress()
            val x=source.left*(1f-t);val y=source.top*(1f-t)
            val width=source.width+(size.width-source.width)*t
            val height=source.height+(size.height-source.height)*t
            fun corner(target:androidx.compose.foundation.shape.CornerSize)=28.dp.toPx()+(target.toPx(size,density)-28.dp.toPx())*t
            val r=RoundRect(Rect(x,y,x+width,y+height),CornerRadius(corner(finalShape.topStart)),CornerRadius(corner(finalShape.topEnd)),CornerRadius(corner(finalShape.bottomEnd)),CornerRadius(corner(finalShape.bottomStart)))
            drawRect(Color.Black.copy(alpha=.22f*t))
            val path=Path().apply {addRoundRect(r)}
            drawContext.canvas.save()
            drawContext.canvas.clipPath(path)
            drawRect(lerp(Color(0xFF511B35),colors.background,t))
            layer.alpha=((t-.5f)/.5f).coerceIn(0f,1f)
            translate(x,y) {drawLayer(layer)}
            drawContext.canvas.restore()
        }
    }
}
