package com.example.model

import androidx.compose.runtime.Immutable

enum class WeatherCondition {
    CLEAR_SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    SNOWY,
    THUNDERSTORM
}

@Immutable
data class WeatherInfo(
    val temperatureCelsius: Int = 20,
    val condition: WeatherCondition = WeatherCondition.CLEAR_SUNNY,
    val conditionName: String = "Clear",
    val locationName: String = "Kyiv"
)
