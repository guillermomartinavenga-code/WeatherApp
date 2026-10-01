package com.securitytraining.weatherapp.presentation.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.securitytraining.weatherapp.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var cityInput by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text(stringResource(R.string.city_input_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { viewModel.onSearch(cityInput) }) {
                Text(stringResource(R.string.search_action))
            }

            when (val state = uiState) {
                is WeatherUiState.Idle -> Unit
                is WeatherUiState.Loading -> CircularProgressIndicator()
                is WeatherUiState.Success -> Text(
                    text = "${state.weather.cityName}: " +
                        stringResource(R.string.temperature_format, state.weather.temperatureCelsius) +
                        " · ${state.weather.description}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                is WeatherUiState.Error -> Text(
                    text = stringResource(R.string.error_generic),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
