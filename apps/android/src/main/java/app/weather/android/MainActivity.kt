package app.weather.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.weather.android.ui.ShizukuWeatherApp
import app.weather.android.ui.theme.ShizukuWeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShizukuWeatherTheme {
                ShizukuWeatherApp()
            }
        }
    }
}
