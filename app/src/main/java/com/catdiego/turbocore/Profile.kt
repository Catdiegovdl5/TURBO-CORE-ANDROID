package com.catdiego.turbocore

sealed class Profile(val name: String, val colorHex: Long) {
    object Eco : Profile("Eco", 0xFF4CAF50)
    object Balanced : Profile("Balanceado", 0xFF2196F3)

    // Hierarquia de Potência (V800+)
    object RankS : Profile("Rank S", 0xFF03DAC5) // Teal
    object RankSSS : Profile("Rank SSS (God)", 0xFFFFD700) // Gold
    object RankOmega : Profile("Rank Ω (Singularidade)", 0xFFFF0000) // Red
    object RankXi : Profile("Rank Ξ (Sacrifício)", 0xFF6200EE) // Deep Purple

    object SensiFF : Profile("Sensi FF", 0xFFFF9800) // Orange

    // Legacy/Deprecated mapping if needed
    object Turbo : Profile("Turbo (Legacy)", 0xFF757575)
    object Sacrifice : Profile("Sacrifice (Legacy)", 0xFF757575)
}

data class QuickAction(
    val name: String,
    val command: String,
    val description: String
)
