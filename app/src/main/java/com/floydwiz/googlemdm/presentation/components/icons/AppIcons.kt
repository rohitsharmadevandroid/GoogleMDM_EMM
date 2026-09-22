package com.floydwiz.googlemdm.presentation.components.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Small stroke-style icon set, ported from the Figma reference's Icons.tsx glyph
// language (thin 1.4-1.5dp strokes on a 16dp grid) so the bottom nav matches
// the rest of the design system without pulling in material-icons-extended.

@Composable
fun IconOverview(modifier: Modifier = Modifier, color: Color = LocalContentColor.current, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val gap = this.size.width * 0.06f
        val cell = (this.size.width - gap) / 2f
        val corner = cell * 0.22f
        listOf(
            Offset(0f, 0f),
            Offset(cell + gap, 0f),
            Offset(0f, cell + gap),
            Offset(cell + gap, cell + gap)
        ).forEach { offset ->
            drawRoundRect(
                color = color,
                topLeft = offset,
                size = Size(cell, cell),
                cornerRadius = CornerRadius(corner, corner)
            )
        }
    }
}

@Composable
fun IconNetwork(modifier: Modifier = Modifier, color: Color = LocalContentColor.current, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.11f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val center = Offset(this.size.width / 2f, this.size.height * 0.78f)

        drawCircle(color = color, radius = strokeWidth * 0.9f, center = center)

        listOf(0.34f, 0.58f, 0.82f).forEachIndexed { index, radiusFraction ->
            val radius = this.size.width * radiusFraction
            drawArc(
                color = color,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = stroke,
                alpha = 1f - index * 0.12f
            )
        }
    }
}

@Composable
fun IconPolicy(modifier: Modifier = Modifier, color: Color = LocalContentColor.current, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.1f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val inset = this.size.width * 0.06f

        drawRoundRect(
            color = color,
            topLeft = Offset(inset * 2f, inset),
            size = Size(this.size.width - inset * 4f, this.size.height - inset * 2f),
            cornerRadius = CornerRadius(this.size.width * 0.1f, this.size.width * 0.1f),
            style = stroke
        )

        val lineStartX = this.size.width * 0.32f
        val lineEndX = this.size.width * 0.68f
        listOf(0.36f, 0.52f, 0.68f).forEachIndexed { index, yFraction ->
            val end = if (index == 2) lineStartX + (lineEndX - lineStartX) * 0.55f else lineEndX
            drawLine(
                color = color,
                start = Offset(lineStartX, this.size.height * yFraction),
                end = Offset(end, this.size.height * yFraction),
                strokeWidth = strokeWidth * 0.85f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Cloud + sync arrow, for the "EMM Backend" splash option and nav — no Figma equivalent exists for this concept. */
@Composable
fun IconBackend(modifier: Modifier = Modifier, color: Color = LocalContentColor.current, size: Dp = 16.dp) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.width * 0.1f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val w = this.size.width
        val h = this.size.height

        // Cloud outline
        val cloudPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.28f, h * 0.62f)
            cubicTo(w * 0.08f, h * 0.62f, w * 0.08f, h * 0.34f, w * 0.28f, h * 0.34f)
            cubicTo(w * 0.32f, h * 0.16f, w * 0.62f, h * 0.14f, w * 0.7f, h * 0.32f)
            cubicTo(w * 0.94f, h * 0.3f, w * 0.96f, h * 0.62f, w * 0.74f, h * 0.62f)
            close()
        }
        drawPath(path = cloudPath, color = color, style = stroke)

        // Sync arrow underneath
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 250f,
            useCenter = false,
            topLeft = Offset(w * 0.32f, h * 0.66f),
            size = Size(w * 0.36f, w * 0.36f),
            style = stroke
        )
    }
}
