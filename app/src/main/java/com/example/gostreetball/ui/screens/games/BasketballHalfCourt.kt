package com.example.gostreetball.ui.screens.games

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.example.gostreetball.data.model.Court
import com.example.gostreetball.data.model.User
import com.example.gostreetball.ui.theme.GoStreetBallTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

sealed class CourtPosition {
    data object FreeThrow : CourtPosition()
    data object BelowBasket : CourtPosition()
    data class ThreePointer(val offset: Float) : CourtPosition()
    data class KeyArea(val left: Boolean, val offset: Float) : CourtPosition()
}

fun CourtPosition.toCoordinates(): Pair<Float, Float> {
    return when (this) {
        CourtPosition.FreeThrow -> 0.5f to (1 / 2.4f)
        CourtPosition.BelowBasket -> 0.5f to 0.9f

        is CourtPosition.ThreePointer -> {
            val r = 0.46f
            val cx = 0.5f
            val cy = 1f - r * 0.2f
            val x = cx + r * cos(offset * PI.toFloat())
            val y = cy - r * (1 / 0.7f) * sin(offset * PI.toFloat())
            x to y
        }

        is CourtPosition.KeyArea -> {
            val x = 0.5f - if (left) 1 / 4.3f / 2 else (- 1 / 4.3f / 2)
            val y = 0.9f - offset / 2.3f
            x to y
        }
    }
}

@Composable
fun BasketballHalfCourt(
    modifier: Modifier = Modifier,
    players: List<Pair<User, CourtPosition>> = emptyList()
) {
    val lineColor = MaterialTheme.colorScheme.onSurface

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f / 0.7f)
            .background(Color.Transparent)
    ) {
        val courtWidth = constraints.maxWidth.toFloat()
        val courtHeight = constraints.maxHeight.toFloat()

        Canvas(
            modifier = modifier.fillMaxSize().zIndex(0f)
        ) {
            val lineWidth = 4f

            drawRect(
                color = lineColor,
                topLeft = Offset(0.0f, 0.0f),
                size = size,
                style = Stroke(width = lineWidth)
            )

            val arcRadius = courtWidth * 0.43f
            drawArc(
                color = lineColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(
                    (courtWidth / 2) - arcRadius,
                    courtHeight - arcRadius * 1.2f
                ),
                size = Size(arcRadius * 2, arcRadius * 2),
                style = Stroke(width = lineWidth)
            )
            drawLine(
                color = lineColor,
                start = Offset(
                    (courtWidth / 2) - arcRadius,
                    courtHeight
                ),
                end = Offset(
                    ((courtWidth / 2) - arcRadius),
                    courtHeight - arcRadius * 0.2f
                ),
                strokeWidth = lineWidth
            )
            drawLine(
                color = lineColor,
                start = Offset(
                    (courtWidth / 2) + arcRadius,
                    courtHeight
                ),
                end = Offset(
                    ((courtWidth / 2) + arcRadius),
                    courtHeight - arcRadius * 0.2f
                ),
                strokeWidth = lineWidth
            )

            val keyWidth = courtWidth / 4.3f
            val keyHeight = courtHeight / 1.9f
            drawRect(
                color = lineColor,
                topLeft = Offset(
                    (courtWidth - keyWidth) / 2f,
                    courtHeight - keyHeight
                ),
                size = Size(keyWidth, keyHeight),
                style = Stroke(width = lineWidth)
            )
            drawArc(
                color = lineColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(
                    (courtWidth / 2) - keyWidth / 2f,
                    courtHeight - keyHeight - keyWidth / 2f
                ),
                size = Size(keyWidth, keyWidth),
                style = Stroke(width = lineWidth)
            )
        }

        val density = LocalDensity.current
        val sizeDp = 40.dp
        val sizePx = with(density) { sizeDp.toPx() }

        players.forEach { (user, position) ->
            val (xNorm, yNorm) = position.toCoordinates()
            val x = xNorm * courtWidth
            val y = yNorm * courtHeight

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset {
                        IntOffset(
                            (x - sizePx / 2).toInt(),
                            (y - sizePx / 2).toInt()
                        )
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.profileImageUrl,
                        contentDescription = user.username,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default user",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Preview")
@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Dark Preview"
)
@Composable
fun HalfCourtScreenPreview() {
    GoStreetBallTheme {
        BasketballHalfCourt(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background),
            players = listOf(
                User(uid = "1", username = "Alice", profileImageUrl = "") to CourtPosition.FreeThrow,
                User(uid = "2", username = "Bob", profileImageUrl = "") to CourtPosition.BelowBasket,
                User(uid = "3", username = "Eve", profileImageUrl = "") to CourtPosition.ThreePointer(0.25f),
                User(uid = "3", username = "Eve", profileImageUrl = "") to CourtPosition.KeyArea(false, 0.5f)
            )
        )
    }
}