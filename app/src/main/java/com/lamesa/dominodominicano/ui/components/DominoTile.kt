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
    faceUp: Boolean = true,
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
        if (faceUp) drawFaceUpTile(tile, selected) else drawFaceDownTile()
    }
}

private fun DrawScope.drawFaceUpTile(tile: Tile, selected: Boolean) {
    val w = size.width
    val h = size.height
    val radius = 6.dp.toPx()

    drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(2f, 3f), Size(w, h), CornerRadius(radius))
    drawRoundRect(Color(0xFFF5F0E6), size = Size(w, h), cornerRadius = CornerRadius(radius))
    drawRoundRect(
        if (selected) Color(0xFF2E7D32) else Color(0xFF3E2723),
        size = Size(w, h),
        cornerRadius = CornerRadius(radius),
        style = Stroke(width = if (selected) 3.dp.toPx() else 1.5.dp.toPx())
    )

    val midY = h / 2
    drawLine(Color(0xFF5D4037).copy(alpha = 0.6f), Offset(6.dp.toPx(), midY), Offset(w - 6.dp.toPx(), midY), 1.5.dp.toPx())

    val pipColor = Color(0xFF1A1A1A)
    drawPips(tile.left, Offset(0f, 0f), Size(w, midY), pipColor)
    drawPips(tile.right, Offset(0f, midY), Size(w, midY), pipColor)
}

private fun DrawScope.drawFaceDownTile() {
    val w = size.width
    val h = size.height
    val radius = 5.dp.toPx()
    drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(1.5f, 2f), Size(w, h), CornerRadius(radius))
    drawRoundRect(Color(0xFF1B1B1B), size = Size(w, h), cornerRadius = CornerRadius(radius))
    drawRoundRect(Color(0xFF4A4A4A), size = Size(w, h), cornerRadius = CornerRadius(radius), style = Stroke(1.2.dp.toPx()))
}

private fun DrawScope.drawPips(value: Int, origin: Offset, area: Size, color: Color) {
    val cx = origin.x + area.width / 2
    val cy = origin.y + area.height / 2
    val r = minOf(area.width, area.height) * 0.09f
    val offset = minOf(area.width, area.height) * 0.22f
    fun pip(x: Float, y: Float) = drawCircle(color, r, Offset(x, y))
    when (value) {
        1 -> pip(cx, cy)
        2 -> { pip(cx - offset, cy - offset); pip(cx + offset, cy + offset) }
        3 -> { pip(cx - offset, cy - offset); pip(cx, cy); pip(cx + offset, cy + offset) }
        4 -> { pip(cx - offset, cy - offset); pip(cx + offset, cy - offset); pip(cx - offset, cy + offset); pip(cx + offset, cy + offset) }
        5 -> { pip(cx - offset, cy - offset); pip(cx + offset, cy - offset); pip(cx, cy); pip(cx - offset, cy + offset); pip(cx + offset, cy + offset) }
        6 -> { pip(cx - offset, cy - offset); pip(cx + offset, cy - offset); pip(cx - offset, cy); pip(cx + offset, cy); pip(cx - offset, cy + offset); pip(cx + offset, cy + offset) }
    }
}
