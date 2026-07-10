package app.weather.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.weather.android.AppTab

@Composable
internal fun FloatingGlassNavigation(
    selected: AppTab,
    visible: Boolean,
    animationsEnabled: Boolean,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animationsEnabled) {
            fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
        } else {
            EnterTransition.None
        },
        exit = if (animationsEnabled) {
            fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        } else {
            ExitTransition.None
        },
    ) {
        Surface(
            modifier = Modifier
                .width(238.dp)
                .height(62.dp)
                .shadow(14.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.48f)),
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                GlassNavButton(
                    selected = selected == AppTab.WEATHER,
                    onClick = { onSelect(AppTab.WEATHER) },
                ) {
                    Icon(Icons.Rounded.WbCloudy, contentDescription = "天气")
                }
                GlassNavButton(
                    selected = selected == AppTab.LOCATIONS,
                    onClick = { onSelect(AppTab.LOCATIONS) },
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = "地点")
                }
                GlassNavButton(
                    selected = selected == AppTab.SETTINGS,
                    onClick = { onSelect(AppTab.SETTINGS) },
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "设置")
                }
            }
        }
    }
}

@Composable
private fun GlassNavButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            Color.Transparent
        },
    ) {
        IconButton(onClick = onClick) {
            icon()
        }
    }
}
