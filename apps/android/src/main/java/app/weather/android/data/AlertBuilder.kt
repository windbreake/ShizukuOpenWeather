package app.weather.android.data

import app.weather.android.model.AlertKind
import app.weather.android.model.AlertSeverity
import app.weather.android.model.DailyForecast
import app.weather.android.model.WeatherAlert

internal object AlertBuilder {
    fun risks(
        precipitation: Int,
        currentWind: Int,
        maxWind: Int,
        aqi: Int,
        today: DailyForecast?,
    ): List<WeatherAlert> = buildList {
        if (precipitation >= 80) {
            add(
                WeatherAlert(
                    id = "rain-risk",
                    title = "强降雨风险提示",
                    severity = AlertSeverity.WARNING,
                    kind = AlertKind.RAIN,
                    detail = "未来数小时降雨概率较高，请关注当地气象部门发布的正式预警。",
                    official = false,
                ),
            )
        } else if (precipitation >= 60) {
            add(
                WeatherAlert(
                    id = "rain-watch",
                    title = "降雨关注",
                    severity = AlertSeverity.WATCH,
                    kind = AlertKind.RAIN,
                    detail = "降雨概率持续偏高，外出建议携带雨具。",
                    official = false,
                ),
            )
        }

        if (maxWind >= 55 || currentWind >= 45) {
            add(
                WeatherAlert(
                    id = "wind-risk",
                    title = "大风风险提示",
                    severity = AlertSeverity.WARNING,
                    kind = AlertKind.WIND,
                    detail = "风速较高，请远离临时搭建物并注意出行安全。",
                    official = false,
                ),
            )
        }

        if (aqi >= 130) {
            add(
                WeatherAlert(
                    id = "air-risk",
                    title = "空气质量提示",
                    severity = AlertSeverity.WATCH,
                    kind = AlertKind.AIR,
                    detail = "空气质量较差，敏感人群建议减少室外停留。",
                    official = false,
                ),
            )
        }

        if ((today?.highTemp ?: 0.0) >= 37) {
            add(
                WeatherAlert(
                    id = "heat-risk",
                    title = "高温风险提示",
                    severity = AlertSeverity.WATCH,
                    kind = AlertKind.HEAT,
                    detail = "最高气温较高，请注意补水并减少午后户外活动。",
                    official = false,
                ),
            )
        }
    }
}
