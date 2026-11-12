package com.nexcode.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Rayons exacts de la maquette : 12, 14, 18 et 20 dp, plus la pilule 999 dp
val Radius12 = 12.dp
val Radius14 = 14.dp
val Radius18 = 18.dp
val Radius20 = 20.dp
val RadiusPill = 999.dp

val ShapeRadius12 = RoundedCornerShape(Radius12)
val ShapeRadius14 = RoundedCornerShape(Radius14)
val ShapeRadius18 = RoundedCornerShape(Radius18)
val ShapeRadius20 = RoundedCornerShape(Radius20)
val PillShape = RoundedCornerShape(RadiusPill)

val Shapes = Shapes(
    extraSmall = ShapeRadius12,
    small = ShapeRadius12,
    medium = ShapeRadius14,
    large = ShapeRadius18,
    extraLarge = ShapeRadius20
)
