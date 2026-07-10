package app.weather.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF276B67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEEA),
    onPrimaryContainer = Color(0xFF143F3D),
    secondary = Color(0xFF74558B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE3F3),
    tertiary = Color(0xFFB85B4A),
    background = Color(0xFFF4F7F8),
    surface = Color(0xFFF9FBFC),
    surfaceVariant = Color(0xFFE8EEF0),
    onSurface = Color(0xFF172124),
    onSurfaceVariant = Color(0xFF536166),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD5CC),
    onPrimary = Color(0xFF0A3835),
    primaryContainer = Color(0xFF245B58),
    secondary = Color(0xFFD6BCE4),
    tertiary = Color(0xFFFFB4A6),
    background = Color(0xFF111719),
    surface = Color(0xFF182124),
    surfaceVariant = Color(0xFF263235),
    onSurface = Color(0xFFE7EFF1),
    onSurfaceVariant = Color(0xFFBBC8CB),
)

@Composable
fun ShizukuWeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = ShizukuTypography,
        content = content,
    )
}
