package com.catdiego.turbocore

import androidx.compose.ui.graphics.Color

data class Profile(
    val name: String,
    val scale: Float,
    val dpi: Int?, // Null implies reset
    val powerMode: Int?, // 0 = Normal, 1 = Low Power
    val trimRam: Boolean = false,
    val color: Color
)
