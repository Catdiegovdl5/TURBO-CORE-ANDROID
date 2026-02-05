package com.catdiego.turbocore

sealed class Profile(val name: String, val colorHex: Long) {
    object Eco : Profile("Eco", 0xFF4CAF50)
    object Balanced : Profile("Balanceado", 0xFF2196F3)
    object Turbo : Profile("Turbo", 0xFFFF0000)
    object Sacrifice : Profile("Sacrifício (Ξ)", 0xFF6200EE)
}
