package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

val MEMBER_COLORS = listOf(
    MemberColor1,
    MemberColor2,
    MemberColor3,
    MemberColor4,
    MemberColor5,
    MemberColor6
)

fun getMemberColor(name: String, colorIndex: Int = 0): Color {
    return when {
        name.equals("Você", ignoreCase = true) -> MemberColor1
        name.equals("Mariana", ignoreCase = true) -> MemberColor2
        name.equals("Lucas", ignoreCase = true) -> MemberColor3
        name.equals("Pedro", ignoreCase = true) -> MemberColor4
        name.equals("Camila", ignoreCase = true) -> MemberColor5
        name.equals("Gabriel", ignoreCase = true) -> MemberColor6
        else -> MEMBER_COLORS[Math.abs(name.hashCode()) % MEMBER_COLORS.size]
    }
}

@Composable
fun MemberAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    colorIndex: Int = 0
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val color = getMemberColor(name, colorIndex)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp
        )
    }
}
