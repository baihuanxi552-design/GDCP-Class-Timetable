package cn.gdcp.timetable

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.sin

/** Adapted from HyperCeiler GlowPainter / GlowController (AGPL-3.0).
 * Copyright (C) 2023-2026 HyperCeiler Contributions.
 * Modifications: Compose Canvas, lifecycle-aware frame clock, portable fallback.
 * See assets/AGPL-3.0.txt and android/WELCOME-ANIMATION.md.
 */
@RequiresApi(33)
private class FlowShader(context: android.content.Context) {
    val shader = RuntimeShader(context.assets.open("hyperceiler-glow.glsl").bufferedReader().use { it.readText() })
    val paint = Paint().apply { shader = this@FlowShader.shader }
    init {
        shader.setFloatUniform("uScale2", 0.82f)
        shader.setFloatUniform("uSpeed2", 0.49f)
        shader.setFloatUniform("uColorInMin", 0.3f)
        shader.setFloatUniform("uColorInMax", 1.0f)
        shader.setFloatUniform("uColorOutMin", 0.3f)
        shader.setFloatUniform("uColorOutMax", 0.86f)
        shader.setFloatUniform("uColorMidPoint", 0.47f)
        shader.setFloatUniform("uUseOklab", 1.0f)
        shader.setFloatUniform("uScale", 1.3f)
        shader.setFloatUniform("uSpeed", 0.4f)
        shader.setFloatUniform("uBrightnessInMin", 0.25f)
        shader.setFloatUniform("uBrightnessInMax", 1.0f)
        shader.setFloatUniform("uBrightnessOutMin", 0.25f)
        shader.setFloatUniform("uBrightnessOutMax", 1.0f)
        shader.setFloatUniform("uShowCircle", 1.0f)
        shader.setFloatUniform("uCircleThickness", 0.4f)
        shader.setFloatUniform("uCircleFinalRadius", 1.0f)
        shader.setFloatUniform("uCircleYOffset", 0.1f)
        shader.setFloatUniform("uCircleSpeed", 0.9f)
        shader.setFloatUniform("uCircleColorFreq", 1.0f)
        shader.setFloatUniform("uCircleColorSpeed", 0.0f)
        shader.setFloatUniform("uCircleEasing", 1.4f)
        shader.setFloatUniform("uCircleAnimationOffset", 0.0f)
        shader.setFloatUniform("uMaskDelay", 0.3f)
        shader.setFloatUniform("uMaskThickness", 0.3f)
        shader.setFloatUniform("uCircleScreenBlend", 1.0f)
        shader.setFloatUniform("uCircleAddBlend", 0.04f)
        shader.setFloatUniform("uCircleColorOffset", 0.25f)
        shader.setFloatUniform("uCircleUVDistort", 0.0f)
        shader.setFloatUniform("uColorToDistortWidthRatio", 0.6f)
        shader.setFloatUniform("uDistortStartTime", 0.2f)
        shader.setFloatUniform("uDistortEndTime", 0.3f)
        shader.setFloatUniform("uDistortStart", 0.0f)
        shader.setFloatUniform("uDistortEnd", 1.0f)
        shader.setFloatUniform("uStripeFrequency", 0.0f)
        shader.setFloatUniform("uStripeStrengthX", 0.0f)
        shader.setFloatUniform("uStripeStrengthY", 0.0f)
        shader.setFloatUniform("uStripeUVDistort", 0.0f)
        shader.setFloatUniform("uColorBlack", 0.961f, 0.157f, 0.157f)
        shader.setFloatUniform("uColorMid", 0.604f, 0.659f, 0.961f)
        shader.setFloatUniform("uColorWhite", 0.302f, 0.29f, 0.843f)
    }
}

@Composable
internal fun WelcomeFlow(modifier: Modifier, progress: () -> Float, dark: Boolean, reduced: Boolean, centerY: () -> Float, settled: Boolean, running: Boolean=true) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val durationScale = android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f).coerceAtLeast(0.01f)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val clock = remember { mutableFloatStateOf(if (reduced || settled) 3f else 0f) }
    LaunchedEffect(lifecycle, reduced, running) {
        if (!reduced && running) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous = 0L
            var direction = 1f
            while (true) withFrameNanos { now ->
                if (previous != 0L) clock.floatValue += ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f) * direction / durationScale
                if (clock.floatValue >= 120f) direction = -1f
                else if (clock.floatValue <= 2f && direction < 0f) direction = 1f
                previous = now
            }
        }
    }
    val gpu = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { FlowShader(context) }.getOrNull() else null }
    Canvas(modifier.clearAndSetSemantics {}) {
        val g = progress()
        if (Build.VERSION.SDK_INT >= 33 && gpu != null) {
            gpu.shader.setFloatUniform("uResolution", size.width, size.height)
            gpu.shader.setFloatUniform("uTime", clock.floatValue)
            gpu.shader.setFloatUniform("uCircleYOffset", 0.5f - centerY())
            gpu.shader.setFloatUniform("uShowCircle", if (reduced || settled) 0f else 1f)
            drawIntoCanvas { it.nativeCanvas.drawRect(0f, 0f, size.width, size.height, gpu.paint) }
        } else {
            // Older Android keeps the same reveal using broad animated radial gradients.
            drawRect(if (dark) Color(0xFF302B3C) else Color(0xFFEEE5F7))
            val t = clock.floatValue * 0.24f
            val c1 = Offset(size.width * (0.35f + sin(t) * 0.23f), size.height * 0.35f)
            val c2 = Offset(size.width * 0.65f, size.height * (0.6f + sin(t * 0.8f) * 0.22f))
            val r = size.maxDimension * 0.9f
            drawRect(Brush.radialGradient(listOf(Color(0xFF7898F8).copy(alpha = 0.48f), Color.Transparent), c1, r))
            drawRect(Brush.radialGradient(listOf(Color(0xFFF088AE).copy(alpha = 0.52f), Color.Transparent), c2, r))
            if (g <= 0.001f) drawRect(Color.Black)
            else if (g < 1f) {
                val radius = g * kotlin.math.hypot(size.width, size.height) * 0.90f
                val feather = size.minDimension * 0.075f
                val inner = ((radius-feather)/(radius+feather)).coerceIn(0f,0.999f)
                drawRect(Brush.radialGradient(0f to Color.Transparent, inner to Color.Transparent, 1f to Color.Black,
                    center = Offset(size.width * 0.5f, size.height * 0.38f), radius = radius + feather))
            }
        }
    }
}
