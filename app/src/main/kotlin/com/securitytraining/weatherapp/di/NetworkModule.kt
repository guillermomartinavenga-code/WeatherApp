package com.securitytraining.weatherapp.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import org.koin.dsl.module

// Stage v0.6: pin the API host's public key (SPKI) so the client rejects any
// TLS chain not issued by the expected cert, even if a CA is compromised or a
// MITM presents an otherwise-valid cert. Two pins are kept for the same host:
// the current leaf (rotates with every cert renewal, verified 2026-10-02,
// expires 2027-03-25) and its issuing intermediate CA (Sectigo Public Server
// Authentication CA OV R36, expires 2036-03-21) as a backup pin -- without a
// backup, a routine cert renewal would hard-break the app until a new release
// ships. Pins were extracted from the live chain, not invented (see
// docs/evidencia-practica-es.md for the extraction commands/output).
private val openWeatherMapCertificatePinner =
    CertificatePinner.Builder()
        .add("api.openweathermap.org", "sha256/2rABlvP8a/45fRdYlmvSYEWrgBZyNampT8AqVpcPMtk=")
        .add("api.openweathermap.org", "sha256/KqkYYX5LYAYP7XGemqzbtPPIA8x7BS/BbOIcAXf3j2k=")
        .build()

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            engine {
                config {
                    certificatePinner(openWeatherMapCertificatePinner)
                }
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }
    }
}
