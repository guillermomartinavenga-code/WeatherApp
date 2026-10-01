package com.securitytraining.weatherapp.domain.usecase

import com.securitytraining.weatherapp.domain.model.CurrentWeather
import com.securitytraining.weatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCurrentWeatherUseCaseTest {

    private val fakeWeather = CurrentWeather(
        cityName = "Buenos Aires",
        temperatureCelsius = 21.5,
        description = "clear sky",
    )

    private val repository = object : WeatherRepository {
        override suspend fun getCurrentWeather(city: String): Result<CurrentWeather> =
            Result.success(fakeWeather)
    }

    @Test
    fun `invoke delegates to the repository and returns its result`() = runTest {
        val useCase = GetCurrentWeatherUseCase(repository)

        val result = useCase("Buenos Aires")

        assertEquals(fakeWeather, result.getOrNull())
    }
}
