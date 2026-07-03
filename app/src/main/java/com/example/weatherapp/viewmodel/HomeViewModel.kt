package com.example.weatherapp.viewmodel

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.*
import com.example.weatherapp.api.WeatherApi
import com.example.weatherapp.api.WeatherForecastApi
import com.example.weatherapp.model.Weather
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

import com.example.weatherapp.api.GeminiApi
import com.example.weatherapp.model.GeminiRequest
import com.example.weatherapp.model.GeminiContent
import com.example.weatherapp.model.GeminiPart
import com.example.weatherapp.BuildConfig

class HomeViewModel : ViewModel() {
    private val _response = MutableLiveData<String>()
    val response: LiveData<String>
        get() = _response

    private var _weather = MutableLiveData<Weather>()
    val weather: LiveData<Weather>
        get() = _weather
    private var _weatherTip = MutableLiveData<String>()
    val weatherTip: LiveData<String>
        get() = _weatherTip
    private var _listDayWeather = MutableLiveData<ArrayList<DayWeather>>()
        .apply { postValue(ArrayList()) }
    val listDayWeather: LiveData<ArrayList<DayWeather>>
        get() = _listDayWeather

    init {
        getWeather()
        getWeatherForecast()
    }

    private fun getWeather() {
        viewModelScope.launch {
            try {
                val result =
                    WeatherApi.retrofitService.getCurrentWeatherData(LATITUDE, LONGITUDE, APPID)
                _weather.value = result
                result.main?.temp?.minus(KELVIN_TO_CELSIUS)?.let { tempC ->
                    result.weather.firstOrNull()?.description?.let { condition ->
                        getWeatherTip(tempC.toFloat(), condition)
                    }
                }
                _response.value = "Success!!!"
            } catch (e: Exception) {
                _response.value = "Failure: ${e.message}"
                Log.i("VM", e.toString())
            }
        }
    }
    private fun getWeatherTip(temp: Float, condition: String) {
        viewModelScope.launch {
            try {
                val prompt = "Weather: ${temp}°C, $condition. Give a one-sentence practical tip in under 15 words."
                val result = GeminiApi.service.getWeatherSummary(
                    apiKey = BuildConfig.GEMINI_API_KEY,
                    request = GeminiRequest(contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))))
                )
                _weatherTip.value = result.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No tip available"
            } catch (e: Exception) {
                _weatherTip.value = "Tip unavailable"
                Log.i("VM", e.toString())
            }
        }
    }

    private fun getWeatherForecast() {
        viewModelScope.launch {
            try {
                Log.i("Debug", "Long: ${LONGITUDE}, Lat: ${LATITUDE}")
                val result = WeatherForecastApi.weatherForecastService.getWeatherForecast(
                    LATITUDE,
                    LONGITUDE,
                    APPID
                )
//                _weatherForecast.value = result
                for (i: Int in HOUR_FORECAST.indices) {
                    val iconUrl = BASE_URL + "/img/w/" + result.list[i].weather[0].icon + ".png"
                    result.list[i].main?.temp?.minus(KELVIN_TO_CELSIUS)?.roundToInt()?.let { temp ->
                        result.list[i].dt_txt?.let { dateTime ->
                            DayWeather(
                                dateTime.toDate().formatTo("dd/MM"),
                                dateTime.toDate().formatTo("HH")+"h",
                                temp,
                                iconUrl
                            )
                        }
                    }?.let { _listDayWeather.value?.add(it) }

                    _listDayWeather.value = _listDayWeather.value
                }
                _response.value = "Success!!!"
                Log.i("Home VM", "listDayWeather size: ${_listDayWeather.value?.size}")
                Log.i("VM", _response.value!!)
            } catch (e: Exception) {
                _response.value = "Failure: ${e.message}"
                Log.i("VM", e.toString())
            }
        }
    }

    private fun getDate(dateTime: String): String {
        val strArr = dateTime.split(" ")[0].split("-")
        return strArr[2] + "/" + strArr[1]
    }

    private fun getTime(dateTime: String): String {
        return dateTime.split(" ")[1].split(":")[0] + "h"
    }


    private fun String.toDate(
        dateFormat: String = "yyyy-MM-dd HH:mm:ss",
        timeZone: TimeZone = TimeZone.getTimeZone("UTC")
    ): Date {
        val parser = SimpleDateFormat(dateFormat, Locale.TAIWAN)
        parser.timeZone = timeZone
        return parser.parse(this)
    }

    private fun Date.formatTo(
        dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val formatter = SimpleDateFormat(dateFormat, Locale.TAIWAN)
        formatter.timeZone = timeZone
        return formatter.format(this)
    }
}


