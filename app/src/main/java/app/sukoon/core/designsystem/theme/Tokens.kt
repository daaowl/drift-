package app.sukoon.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** 4/8 grid. */
@Immutable
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 48.dp
    val huge: Dp = 64.dp

    /** Horizontal page gutter. */
    val gutter: Dp = 20.dp

    /** Comfortable max line length for reading on wide screens. */
    val readingMaxWidth: Dp = 640.dp
}

@Immutable
object Radii {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val pill: Dp = 999.dp
}

val SukoonShapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl),
)

@Immutable
object Elevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 10.dp
}

object Motion {
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
    val offsetSpring: FiniteAnimationSpec<IntOffset> = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

    const val FADE_SHORT = 180
    const val FADE_MEDIUM = 320
    const val FADE_LONG = 600
}
