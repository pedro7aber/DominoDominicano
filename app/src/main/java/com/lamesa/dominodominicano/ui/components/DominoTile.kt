package com.lamesa.dominodominicano.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamesa.dominodominicano.domain.Tile

@Composable
fun DominoTileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 88.dp,
    selected: Boolean = false,
    legal: Boolean = false,
    faceUp: Boolean = true,
    horizontalDouble: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .size(width, height)
            .then(
                if (onClick != null) Modifier.pointerInput(tile) {
                    detectTapGestures { onClick() }
                } else Modifier
            )
    ) {
        if (faceUp) {
            if (horizontalDouble && tile.isDouble) {
                // Dibujar doble en orientación transversal (rotado 90°)
                rotate(90f, pivot = center) {
                    drawFaceUpTile(tile, selected, legal)
                }
            } else {
                drawFaceUpTile(tile, selected, legal)
            }
        } else {
            drawFaceDownTile()
        }
    }
}

private fun DrawScope.drawFaceUpTile(tile: Tile, selected: Boolean, legal: Boolean) {
    val w = size.width
    val h = size.height
    val radius = 5.dp.toPx()

    // Sombra sutil
    drawRoundRect(
        Color.Black.copy(alpha = 0.28f),
        Offset(1.8f, 2.5f),
        Size(w, h),
        CornerRadius(radius)
    )

    // Cuerpo marfil
    drawRoundRect(
        Color(0xFFF8F1E3),
        size = Size(w, h),
        cornerRadius = CornerRadius(radius)
    )

    // Borde
    val borderColor = when {
        selected -> Color(0xFF2E7D32)
        legal -> Color(0xFF66BB6A)
        else -> Color(0xFF4E342E)
    }
    drawRoundRect(
        borderColor,
        size = Size(w, h),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = if (selected) 3.2.dp.toPx() else if (legal) 2.2.dp.toPx() else 1.4.dp.toPx())
    )

    // Línea divisoria
    val midY = h / 2
    drawLine(
        Color(0xFF5D4037).copy(alpha = 0.55f),
        Offset(5.dp.toPx(), midY),
        Offset(w - 5.dp.toPx(), midY),
        strokeWidth = 1.4.dp.toPx()
    )

    val pipColor = Color(0xFF1C1C1C)
    drawPips(tile.left, Offset(0f, 0f), Size(w, midY), pipColor)
    drawPips(tile.right, Offset(0f, midY), Size(w, midY), pipColor)
}

private fun DrawScope.drawFaceDownTile() {
    val w = size.width
    val h = size.height
    val radius = 4.dp.toPx()
    drawRoundRect(Color.Black.copy(alpha = 0.32f), Offset(1.5f, 2f), Size(w, h), CornerRadius(radius))
    drawRoundRect(Color(0xFF1A1A1A), size = Size(w, h), cornerRadius = CornerRadius(radius))
    drawRoundRect(
        Color(0xFF4A4A4A),
        size = Size(w, h),
        cornerRadius = CornerRadius(radius),
        style = Stroke(1.1.dp.toPx())
    )
    // Pequeño detalle central
    drawCircle(Color(0xFF333333), radius = minOf(w, h) * 0.12f, center = center)
}

private fun DrawScope.drawPips(value: Int, origin: Offset, area: Size, color: Color) {
    val cx = origin.x + area.width / 2
    val cy = origin.y + area.height / 2
    val r = minOf(area.width, area.height) * 0.095f
    val offset = minOf(area.width, area.height) * 0.23f
    fun pip(x: Float, y: Float) = drawCircle(color, r, Offset(x, y))
    when (value) {
        1 -> pip(cx, cy)
        2 -> { pip(cx - offset, cy - offset); pip(cx + offset, cy + offset) }
        3 -> { pip(cx - offset, cy - offset); pip(cx, cy); pip(cx + offset, cy + offset) }
        4 -> {
            pip(cx - offset, cy - offset); pip(cx + offset, cy - offset)
            pip(cx - offset, cy + offset); pip(cx + offset, cy + offset)
        }
        5 -> {
            pip(cx - offset, cy - offset); pip(cx + offset, cy - offset)
            pip(cx, cy)
            pip(cx - offset, cy + offset); pip(cx + offset, cy + offset)
        }
        6 -> {
            pip(cx - offset, cy - offset); pip(cx + offset, cy - offset)
            pip(cx - offset, cy); pip(cx + offset, cy)
            pip(cx - offset, cy + offset); pip(cx + offset, cy + offset)
        }
    }
}
