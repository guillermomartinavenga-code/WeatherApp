package com.securitytraining.weatherapp.di

import com.securitytraining.weatherapp.core.ConfigLoader
import com.securitytraining.weatherapp.data.remote.WeatherApiService
import com.securitytraining.weatherapp.data.repository.WeatherRepositoryImpl
import com.securitytraining.weatherapp.domain.repository.WeatherRepository
import com.securitytraining.weatherapp.domain.usecase.GetCurrentWeatherUseCase
import com.securitytraining.weatherapp.presentation.weather.WeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { ConfigLoader(get()).load() }
    single { WeatherApiService(get(), get()) }
    single<WeatherRepository> { WeatherRepositoryImpl(get()) }
    factory { GetCurrentWeatherUseCase(get()) }
    viewModel { WeatherViewModel(get()) }
}
