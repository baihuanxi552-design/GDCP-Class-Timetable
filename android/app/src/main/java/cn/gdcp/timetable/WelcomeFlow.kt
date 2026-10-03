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

// Original AGSL: moving color fields and a radial reveal, without upstream shader code.
private const val FLOW_SHADER = """
uniform float2 viewport;
uniform float clock;
uniform float reveal;
uniform float night;
half4 main(float2 p) {
    float2 uv = p / viewport;
    float t = clock * 0.24;
    float2 wave = uv + float2(sin(uv.y * 4.0 + t) * 0.14, cos(uv.x * 3.0 - t) * 0.12);
    float2 a = float2(0.24 + sin(t * 0.8) * 0.27, 0.35 + cos(t * 0.7) * 0.22);
    float2 b = float2(0.69 + cos(t * 0.6) * 0.24, 0.65 + sin(t * 0.9) * 0.24);
    float blue = exp(-dot(wave-a, wave-a) * 3.5);
    float pink = exp(-dot(wave-b, wave-b) * 3.6);
    float3 color = mix(float3(0.93, 0.90, 0.98), float3(0.63, 0.71, 0.98), blue * 0.70);
    color = mix(color, float3(0.96, 0.59, 0.72), pink * 0.73);
    color = mix(color, color * 0.29, night);
    float d = length(p - viewport * float2(0.5, 0.38));
    float radius = reveal * length(viewport) * 0.90;
    float feather = min(viewport.x, viewport.y) * 0.075;
    float visible = (1.0 - smoothstep(radius - feather, radius + feather, d)) * smoothstep(0.0, 0.025, reveal);
    float ring = exp(-pow((d - radius) / (feather * 0.65), 2.0)) * sin(reveal * 3.14159);
    color += float3(0.13, 0.10, 0.18) * ring;
    return half4(color * visible, 1.0);
}
"""

@RequiresApi(33)
private class FlowShader {
    val shader = RuntimeShader(FLOW_SHADER)
    val paint = Paint().apply { shader = this@FlowShader.shader }
}

@Composable
internal fun WelcomeFlow(modifier: Modifier, progress: () -> Float, dark: Boolean, reduced: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val clock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(lifecycle, reduced) {
        if (!reduced) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var previous = 0L
            while (true) withFrameNanos { now ->
                if (previous != 0L) clock.floatValue += ((now - previous) / 1_000_000_000f).coerceAtMost(0.05f)
                previous = now
            }
        }
    }
    val gpu = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { FlowShader() }.getOrNull() else null }
    Canvas(modifier.clearAndSetSemantics {}) {
        val g = progress()
        if (Build.VERSION.SDK_INT >= 33 && gpu != null) {
            gpu.shader.setFloatUniform("viewport", size.width, size.height)
            gpu.shader.setFloatUniform("clock", clock.floatValue)
            gpu.shader.setFloatUniform("reveal", g)
            gpu.shader.setFloatUniform("night", if (dark) 1f else 0f)
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
