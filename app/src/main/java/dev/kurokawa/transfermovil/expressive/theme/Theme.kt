package dev.kurokawa.transfermovil.expressive.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.kurokawa.transfermovil.expressive.R

// Transfermóvil brand: bank blue #003DA5 + arrow green #2CA800.
internal val LightScheme = lightColorScheme(
    primary = Color(0xFF003DA5), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF), onPrimaryContainer = Color(0xFF001847),
    secondary = Color(0xFF2CA800), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB7F39A), onSecondaryContainer = Color(0xFF0C2000),
    surface = Color(0xFFF8F9FD), onSurface = Color(0xFF191B20),
)
internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF), onPrimary = Color(0xFF002C73),
    primaryContainer = Color(0xFF0040A0), onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF8DD96B), onSecondary = Color(0xFF173800),
    secondaryContainer = Color(0xFF255100), onSecondaryContainer = Color(0xFFB7F39A),
    surface = Color(0xFF101318), onSurface = Color(0xFFE1E2E8),
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
private fun sansFlexFamily(): FontFamily {
    val normal = Font(R.font.google_sans_flex, FontWeight.Normal)
    val medium = Font(R.font.google_sans_flex, FontWeight.Medium)
    val semiBold = Font(R.font.google_sans_flex, FontWeight.SemiBold)
    val bold = Font(R.font.google_sans_flex, FontWeight.Bold)
    val heavyItalic = Font(R.font.google_sans_flex, FontWeight.ExtraBold, FontStyle.Italic)
    return remember(normal, medium, semiBold, bold, heavyItalic) {
        FontFamily(normal, medium, semiBold, bold, heavyItalic)
    }
}

@Composable
private fun expressiveTypography(): Typography {
    val flex = sansFlexFamily()
    val base = Typography()
    return remember(flex) {
        Typography(
            displayLarge = base.displayLarge.copy(fontFamily = flex, fontWeight = FontWeight.Bold),
            displayMedium = base.displayMedium.copy(fontFamily = flex, fontWeight = FontWeight.Bold),
            displaySmall = base.displaySmall.copy(fontFamily = flex, fontWeight = FontWeight.SemiBold),
            headlineLarge = base.headlineLarge.copy(fontFamily = flex, fontWeight = FontWeight.SemiBold),
            headlineMedium = base.headlineMedium.copy(fontFamily = flex, fontWeight = FontWeight.SemiBold),
            headlineSmall = base.headlineSmall.copy(fontFamily = flex, fontWeight = FontWeight.SemiBold),
            titleLarge = base.titleLarge.copy(fontFamily = flex, fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontFamily = flex, fontWeight = FontWeight.Medium),
            titleSmall = base.titleSmall.copy(fontFamily = flex, fontWeight = FontWeight.Medium),
            bodyLarge = base.bodyLarge.copy(fontFamily = flex),
            bodyMedium = base.bodyMedium.copy(fontFamily = flex),
            bodySmall = base.bodySmall.copy(fontFamily = flex),
            labelLarge = base.labelLarge.copy(fontFamily = flex, fontWeight = FontWeight.Medium),
            labelMedium = base.labelMedium.copy(fontFamily = flex, fontWeight = FontWeight.Medium),
            labelSmall = base.labelSmall.copy(fontFamily = flex, fontWeight = FontWeight.Medium),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransfermovilTheme(darkMode: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= 31 ->
            if (darkMode) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        darkMode -> DarkScheme
        else -> LightScheme
    }
    val motion = remember { MotionScheme.expressive() }
    MaterialExpressiveTheme(
        colorScheme = scheme, motionScheme = motion, shapes = ExpressiveShapes,
        typography = expressiveTypography(), content = content,
    )
}
