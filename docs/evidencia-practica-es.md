# Evidencia práctica — WeatherApp (borrador)

| | |
|---|---|
| **Proyecto** | WeatherApp (Android, Kotlin, Compose) |
| **Relacionado con** | `api-keys-y-secretos-en-el-codigo-v0.4.md` — completa su §12 ("Ejercicio práctico guiado") |
| **Estado** | Borrador en español. Se producirá una versión final en inglés cuando esta se estabilice. |

## Cómo leer este documento

Cada etapa corresponde a un tag de git sobre `main` (`v0.1-hardcoded-key`, `v0.2-...`, etc.). El historial **no se reescribe ni se aplasta** entre etapas: esa persistencia es, en sí misma, parte de la evidencia (ver §3.1 y §8.2 del documento de referencia). Ninguna etapa "insegura" contiene un secreto real: siempre se usa un valor ficticio con formato plausible. La key real y desechable de OpenWeatherMap, usada para las capturas de creación/revocación/rotación, se gestiona por fuera del repositorio (ver etapa 3).

---

## Etapa v0.1 — Clave hardcodeada en el código fuente

**Referencia teórica:** §3.1 (código fuente y VCS), §5 (limitaciones estructurales), §8.1 (mantener los secretos fuera del código).

**Qué se hizo:** primer commit del proyecto. Scaffold completo de la app (Clean Architecture por paquetes, MVVM, Compose, Koin, Ktor) con la API key de OpenWeatherMap como literal de Kotlin, usada directamente por el cliente HTTP:

```kotlin
// app/src/main/kotlin/com/securitytraining/weatherapp/core/Constants.kt
object Constants {
    const val OPEN_WEATHER_API_KEY = "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b"
    const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
}
```

```kotlin
// app/src/main/kotlin/com/securitytraining/weatherapp/data/remote/WeatherApiService.kt
httpClient.get("${Constants.BASE_URL}weather") {
    parameter("q", city)
    parameter("appid", Constants.OPEN_WEATHER_API_KEY)
    parameter("units", "metric")
}
```

**Nota sobre el valor usado:** `b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b` **no es una key real**. Es una cadena hexadecimal de 32 caracteres (formato real de las keys de OpenWeatherMap) generada solo para esta demostración. Al ser público el repositorio, el objetivo es observar si el escaneo de secretos de GitHub la marca por *formato* — confirmado previamente que `openweather_api_key` es un patrón reconocido por GitHub (no "partner", es decir, sin verificación de validez en vivo, pero sí con alerta/push protection por defecto en repos públicos).

**Riesgo evidenciado:** cualquiera con acceso de lectura al repositorio — y, más importante, a su historial de git aunque la línea se elimine en un commit futuro — puede recuperar este valor. Es el antipatrón más básico y más común (§3.1).

**Verificación realizada en esta etapa:**
- `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest` → `BUILD SUCCESSFUL` (test unitario de `GetCurrentWeatherUseCase`).
- Demo **estática**: en esta etapa no se conecta la key real; el valor commiteado es el único presente, por lo que la app no realiza llamadas reales todavía (eso comienza en la etapa que introduce `local.properties`).

**Verificación del escaneo de secretos nativo de GitHub (post-push):**

Tras pushear el repo público, se revisó **Security and quality → Secret scanning alerts → View detected secrets**: no apareció ninguna alerta para el valor de `Constants.kt`.

**Corrección (ver también etapa v0.2):** en un primer análisis durante la planificación se afirmó que el patrón `openweather_api_key` tiene push protection *deshabilitada* por defecto. Ese dato era incorrecto — surgió de una lectura automática mal interpretada de la tabla de la documentación de GitHub. Inspeccionando directamente los datos estructurados que GitHub embebe en esa misma página (`secretType":"openweather_api_key", ..., "hasPushProtection":true`), el patrón **sí tiene push protection habilitada por defecto** en repos públicos. Esto se confirma además de forma concreta en la etapa v0.2: el push de esa etapa fue bloqueado por GitHub precisamente por este patrón. Lo que sigue sin explicación confirmada es por qué el push de *esta* etapa (v0.1) no fue bloqueado ni alertado pese a contener el mismo valor — ver el análisis en la etapa v0.2.

**Conclusión para el documento final:** el escaneo de secretos *pattern-based* de GitHub tiene comportamiento inconsistente entre contextos de código distintos para el mismo valor (ver v0.2) — lo cual, lejos de invalidar el control, refuerza por qué §8.10 lo trata como un complemento, no como única línea de defensa. La etapa de CI/CD (más adelante) agrega `gitleaks` corrido por nosotros mismos, con detección garantizada y reproducible sobre el propio repositorio, independiente de heurísticas de terceros que no controlamos.

**Verificación con escaneo propio (Gitleaks):**

Para no depender únicamente del resultado (negativo) del escaneo nativo de GitHub, se instaló y corrió [Gitleaks](https://github.com/gitleaks/gitleaks) v8.30.1 (licencia MIT, binario oficial descargado de GitHub Releases, checksum verificado contra `gitleaks_8.30.1_checksums.txt`) directamente sobre el historial de git local:

```bash
gitleaks detect --source . -v --report-format json --report-path gitleaks-report.json
```

**Resultado:** `leaks found: 2`, ambos sobre el mismo valor (`b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b`), en el commit `f2aa2c0` (etapa v0.1):

| Archivo | Línea | Regla |
|---|---|---|
| `app/src/main/kotlin/com/securitytraining/weatherapp/core/Constants.kt` | 19 | `generic-api-key` |
| `docs/evidencia-practica-es.md` | 24 | `generic-api-key` (el snippet de código citado en este mismo documento, como evidencia) |

**Dato relevante:** la regla que disparó es `generic-api-key` (basada en entropía + contexto léxico como `const val ... KEY = "..."`), **no** una regla específica de OpenWeatherMap — el ruleset por defecto de Gitleaks no tiene un patrón dedicado para este proveedor. Es decir, Gitleaks detectó el valor por su *forma genérica de secreto*, no por reconocer el formato puntual de OpenWeatherMap.

**Contraste con el hallazgo anterior:** esto refuerza la conclusión de la sección previa, ahora con evidencia en ambas direcciones — GitHub (reconoce el *proveedor* vía patrón `openweather_api_key`, pero no alertó sobre este valor sintético) vs. Gitleaks (no conoce el proveedor, pero sí detectó el valor por heurística genérica de entropía/contexto). Ningún escáner es una garantía por sí solo; **ejecutar nuestro propio control, reproducible y bajo nuestro control, es justamente el argumento de §8.10 y lo que motiva incorporar Gitleaks al pipeline de CI/CD** en una etapa posterior — no solo confiar en el escaneo pasivo de la plataforma.

**Pendiente para el documento final:** capturas de pantalla de Android Studio mostrando el archivo `Constants.kt`, la captura ya tomada de la pantalla "Secret scanning alerts" sin resultados, y la salida de consola de este escaneo con Gitleaks.

---

## Etapa v0.2 — Clave en archivo de configuración expuesto

**Referencia teórica:** §3.2 (archivos de configuración), §5 (limitaciones estructurales).

**Qué se hizo:** se retiró el literal hardcodeado de `Constants.kt` (eliminado) y la key pasó a vivir en un archivo de configuración en texto plano, cargado en tiempo de ejecución:

```json
// app/src/main/assets/config.json
{
    "openWeatherApiKey": "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b",
    "baseUrl": "https://api.openweathermap.org/data/2.5/"
}
```

```kotlin
// app/src/main/kotlin/com/securitytraining/weatherapp/core/ConfigLoader.kt
class ConfigLoader(private val context: Context) {
    fun load(): AppConfig =
        context.assets.open(CONFIG_FILE_NAME).bufferedReader().use { reader ->
            Json.decodeFromString(AppConfig.serializer(), reader.readText())
        }
    // ...
}
```

`config.json` se ubica en `app/src/main/assets/`, carpeta que Android empaqueta **sin compilar ni ofuscar** dentro del APK (a diferencia de `res/`, que sí pasa por el compilador de recursos). El archivo **no se agregó a `.gitignore`** — queda versionado igual que cualquier otro archivo fuente.

**Nota sobre el valor usado:** se reutilizó el mismo placeholder `b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b` de la etapa v0.1 (sigue sin ser una key real), para mantener continuidad narrativa: el "secreto" nunca rotó entre etapas, tal como ocurriría en un caso real si nadie detecta la exposición.

**Riesgo evidenciado:** esta etapa expone el valor por **dos vías simultáneas** — (a) el árbol de código fuente y su historial de git, igual que en v0.1, y (b) el binario distribuido: cualquiera que descargue el APK puede descomprimirlo con una herramienta estándar (`unzip app-debug.apk -d out`) y leer `out/assets/config.json` en texto plano, **sin necesidad de un decompilador** como `jadx`/`apktool`. Es una exposición más accesible que la de un literal en bytecode Kotlin, porque no requiere ningún paso de ingeniería inversa.

**Verificación realizada en esta etapa:**
- `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL`.
- `./gradlew :app:testDebugUnitTest` → `BUILD SUCCESSFUL`.
- Demo **estática**, igual que v0.1: no hay key real conectada todavía.

**Verificación de extracción directa del APK (sin decompilar):**

Ejecutado por Guillermo sobre el APK debug generado por el build anterior:

```
$ unzip app-debug.apk assets/config.json -d out && cat out/assets/config.json
Archive:  app-debug.apk
  inflating: out/assets/config.json
{
    "openWeatherApiKey": "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b",
    "baseUrl": "https://api.openweathermap.org/data/2.5/"
}
```

Confirma el riesgo descrito arriba de forma concreta, no solo teórica: `unzip` (una herramienta estándar, sin ningún paso de ingeniería inversa) alcanza para leer la key en texto plano directamente del binario distribuido.

**Hallazgo no planificado: GitHub Push Protection bloqueó este push (a diferencia de v0.1):**

Al intentar pushear esta etapa, GitHub **rechazó el push** con `GH013: Repository rule violations found` / `Push cannot contain secrets`, señalando el patrón **"Openweather API Key"** en tres ubicaciones del mismo commit:

```
remote: error: GH013: Repository rule violations found for refs/heads/main.
remote: - GITHUB PUSH PROTECTION
remote:     - Push cannot contain secrets
remote:   —— Openweather API Key ———————————————————————————————
remote:    locations:
remote:      - commit: 06fdd43673d9fc7012b90d7137807e1893f6b5f9
remote:        path: app/src/main/assets/config.json:2
remote:      - commit: 06fdd43673d9fc7012b90d7137807e1893f6b5f9
remote:        path: docs/evidencia-practica-es.md:90
remote:      - commit: 06fdd43673d9fc7012b90d7137807e1893f6b5f9
remote:        path: docs/evidencia-practica-es.md:126
remote:    (?) To push, remove secret from commit(s) or follow this URL to allow the secret.
remote:    https://github.com/guillermomartinavenga-code/WeatherApp/security/secret-scanning/unblock-secret/3K8o57ctov8kBZlZ4KIyugYh3dC
To github-guillermomartinavenga-code:guillermomartinavenga-code/WeatherApp.git
 ! [remote rejected] main -> main (push declined due to repository rule violations)
```

El mismo rechazo ocurrió al pushear el tag `v0.2-config-file-exposed`.

Esto contradice el resultado negativo observado en v0.1 con el **mismo valor de key** — y motivó corregir ahí mismo el dato impreciso sobre push protection (ver nota de corrección en v0.1). Con la fuente de verdad confirmada (push protection habilitada por defecto para este patrón), quedan dos hipótesis abiertas para la discrepancia v0.1/v0.2 — **ninguna confirmada**, se documentan ambas honestamente:

1. **Coincidencia de contexto léxico:** `config.json` usa literalmente la clave `"openWeatherApiKey"`, que calza de forma más exacta con el patrón interno de GitHub para `openweather_api_key` que el nombre de constante Kotlin `OPEN_WEATHER_API_KEY` usado en v0.1 (los patrones de GitHub no son puramente regex sobre el valor; suelen considerar también el contexto/nombre de variable circundante).
2. **Ventana temporal de propagación:** el escaneo de secret scanning no es necesariamente instantáneo sobre cada commit; es posible que el commit de v0.1 no haya sido re-evaluado por push protection en el momento exacto del push (push protection corre en el momento del push; alertas de escaneo del historial completo pueden tener demora adicional).

**Qué dice esto para el documento final:** sea cual sea la causa exacta, el hallazgo es valioso tal cual: push protection **sí funcionó** cuando debía, bloqueando en el momento del push un commit con un secreto reconocible — exactamente el control que §8.10 recomienda, y una demostración mucho más fuerte que la ausencia de alerta observada en v0.1.

**Próximo paso:** dado que el valor es un placeholder ficticio (nunca una key real) y el objetivo del proyecto es justamente documentar este flujo, corresponde **permitir el secreto** desde la URL que GitHub ofrece (`.../security/secret-scanning/unblock-secret/...`), eligiendo una razón honesta (p. ej. "Used in tests"), y luego reintentar el push. Vale la pena capturar esa pantalla de "Allow secret" antes de confirmarla — es evidencia aún mejor que la planeada originalmente.

**Pendiente para el documento final:** captura de pantalla de la salida de consola de v0.1 y v0.2 (gitleaks, unzip, GH013), y de la pantalla de "Allow secret" de GitHub.

## Etapa v0.3 — `.gitignore` tardío + pipeline de CI/CD inseguro

**Referencia teórica:** §3.1/§8.2 (persistencia en el historial de git), §3.5/§8.11 (secretos en pipelines de CI/CD).

**Qué se hizo:** se agregó `app/src/main/assets/config.json` a `.gitignore` y se le quitó el tracking (`git rm --cached`) — el error clásico de pensar "ya lo gitignoré, ya está arreglado". El archivo sigue físicamente en el disco local (Guillermo lo sigue necesitando para compilar/correr localmente), pero deja de aparecer en commits futuros.

**Evidencia de que el árbol actual "parece limpio":**

```
$ git ls-files | grep config.json
(sin resultados -- no hay ningún config.json trackeado)
```

**Evidencia de que el valor sigue 100% recuperable del historial, pese a lo anterior:**

```
$ git log --all --oneline -- app/src/main/assets/config.json
93d1a19 Stage v0.3: late .gitignore + insecure CI/CD pipeline
77f77d0 Stage v0.2: move API key to an exposed plaintext config file

$ git show 77f77d0:app/src/main/assets/config.json
{
    "openWeatherApiKey": "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b",
    "baseUrl": "https://api.openweathermap.org/data/2.5/"
}
```

`git log --all` encuentra el archivo en los dos commits donde existió (su creación en v0.2 y su propio borrado/.gitignore en v0.3), y `git show <sha>:<path>` extrae el contenido completo de cualquiera de esos puntos del historial. Ni agregar `.gitignore` ni quitar el tracking borra nada del pasado — ambos solo afectan commits *futuros* (§3.1, confirmado aquí de forma concreta y no solo citado).

**Efecto colateral honesto:** un clon nuevo del repositorio (como lo haría cualquier pipeline de CI) ya **no trae `config.json`** — se verificó clonando el repo a una carpeta temporal limpia: la carpeta `app/src/main/assets/` directamente no existe. `./gradlew :app:assembleDebug` y `:app:testDebugUnitTest` igual terminan en `BUILD SUCCESSFUL`, porque Gradle no necesita el asset en tiempo de compilación — pero una instalación real del APK fallaría al intentar leer la configuración en runtime (`ConfigLoader.load()` lanzaría una excepción). Esta "rotura" queda así, sin resolver, hasta la etapa v0.4 (`BuildConfig` + `local.properties`), que es la que introduce el mecanismo correcto de provisión de la key. No se oculta ni se parchea antes de tiempo: es consecuencia realista de intentar una solución apurada (`.gitignore` tardío) sin reemplazar todavía el mecanismo de carga.

**Pipeline de CI/CD inseguro (`.github/workflows/ci.yml`):**

Se agregó el primer workflow de GitHub Actions del proyecto — build + test en cada push/PR a `main` — con dos antipatrones deliberados de §3.5:

```yaml
env:
  # INSECURE ON PURPOSE: hardcoded directly in the workflow file.
  OPEN_WEATHER_API_KEY: b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b

steps:
  # ...
  - name: Debug print build environment
    run: echo "OpenWeatherMap key in use -> $OPEN_WEATHER_API_KEY"
```

1. El valor está escrito directamente en el archivo de definición del pipeline, que vive en el repositorio igual que cualquier otro archivo fuente.
2. Se imprime en texto plano al log del build vía `echo` — GitHub Actions solo enmascara (`***`) valores registrados como *secret* encriptado en la configuración del repo, no variables hardcodeadas como ésta.

**Nota de honestidad:** el build de Gradle **no** consume todavía esta variable — la app sigue sin tener wireada ninguna key real (seguimos en demo estática). Este secreto del workflow existe únicamente para demostrar, de forma aislada, el antipatrón específico de CI/CD; la variable se conecta al build recién en v0.4, cuando se reemplaza por un secret real de GitHub Actions.

**Verificación realizada en esta etapa:**
- `./gradlew :app:assembleDebug` y `:app:testDebugUnitTest` → `BUILD SUCCESSFUL`, tanto en el working tree local como en un clon limpio (sin `config.json`).
- Runner verificado: `ubuntu-latest` trae preinstalado el Android SDK (`android-37`, build-tools `37.0.0`) con `ANDROID_HOME`/`ANDROID_SDK_ROOT` ya configurados — confirmado contra la [documentación de `actions/runner-images`](https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2404-Readme.md), no asumido. El workflow no necesita un paso adicional de `setup-android`.

**Punto de pivote (acción de Guillermo, fuera del repositorio):** a partir de aquí corresponde generar la key real y desechable de OpenWeatherMap (para las capturas de creación/revocación/rotación) y tenerla lista para la etapa v0.4, que es la primera que la conecta de verdad a través de `local.properties`. Esa key real nunca se pega en este documento ni en ningún archivo gestionado por Claude.

**Pendiente para el documento final:** captura de la ejecución real de este workflow en GitHub Actions (incluyendo el log con la key impresa en texto plano), y captura de la pantalla de creación de la key real en el dashboard de OpenWeatherMap.

## Etapa v0.4 — `BuildConfig` + `local.properties` (primera etapa de "fix")

**Referencia teórica:** §8 (controles recomendados), específicamente la inyección vía `BuildConfig`/`local.properties` y su límite conocido: saca la key del control de versiones y de los assets del APK, pero no la saca del binario compilado.

**Qué se hizo:**
- Se borraron por completo `core/AppConfig.kt`, `core/ConfigLoader.kt` y `assets/config.json` (el mecanismo entero de v0.2/v0.3), no solo se dejaron de usar.
- `app/build.gradle.kts` ahora lee la key en este orden: primero una Gradle property (`-POPEN_WEATHER_API_KEY=...`, la vía que usa CI), y si no está presente, `local.properties` (gitignored desde el `.gitignore` original del template, nunca comiteado) — con fallback a cadena vacía si ninguna de las dos está. El valor se expone como `BuildConfig.OPEN_WEATHER_API_KEY` vía `buildConfigField`:

```kotlin
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) load(FileInputStream(localPropertiesFile))
}

fun resolveOpenWeatherApiKey(): String =
    (project.findProperty("OPEN_WEATHER_API_KEY") as String?)
        ?: localProperties.getProperty("OPEN_WEATHER_API_KEY")
        ?: ""

android {
    defaultConfig {
        buildConfigField("String", "OPEN_WEATHER_API_KEY", "\"${resolveOpenWeatherApiKey()}\"")
    }
    buildFeatures { buildConfig = true }
}
```

- `WeatherApiService` pasó de depender de `AppConfig` (inyectado por Koin) a leer directamente `BuildConfig.OPEN_WEATHER_API_KEY`, ya que es una constante de compilación — no necesita DI. `AppModule.kt` se simplificó acorde (ya no instancia `ConfigLoader`).

**Pipeline de CI/CD (fix, contraste directo con v0.3):**

```yaml
env:
  # El valor ahora viene de un secret encriptado de GitHub Actions, nunca de
  # un literal en este archivo.
  OPEN_WEATHER_API_KEY: ${{ secrets.OPEN_WEATHER_API_KEY }}

steps:
  # Mismo comando que en v0.3, a propósito, como contraste antes/después:
  - name: Debug print build environment
    run: echo "OpenWeatherMap key in use -> $OPEN_WEATHER_API_KEY"
  - name: Assemble debug APK
    run: ./gradlew :app:assembleDebug -POPEN_WEATHER_API_KEY="$OPEN_WEATHER_API_KEY"
```

En v0.3 ese mismo `echo` imprimía el valor en texto plano porque la variable era un literal del YAML, no un *secret* registrado. Ahora, una vez que Guillermo registre `OPEN_WEATHER_API_KEY` como secret real del repositorio (Settings → Secrets and variables → Actions), GitHub Actions debería reemplazar automáticamente cualquier aparición de ese valor en el log por `***` — mismo comando, resultado distinto, porque lo que cambió es *dónde* vive el valor, no el comando que lo imprime.

**Verificación realizada en esta etapa:**
- `./gradlew :app:assembleDebug :app:testDebugUnitTest -POPEN_WEATHER_API_KEY=b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b` → `BUILD SUCCESSFUL` (el valor usado es el mismo placeholder de siempre, nunca una key real; Claude no tiene acceso de escritura/lectura a `local.properties` en este entorno por diseño, así que esta verificación local pasa el valor por `-P` en vez de tocar ese archivo).

**Evidencia de decompilación (`apktool`) — el límite real de `BuildConfig`:**

```
$ apktool d -f -o /tmp/weatherapp-v0.4-decompiled app/build/outputs/apk/debug/app-debug.apk
...
$ grep -rn "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b" /tmp/weatherapp-v0.4-decompiled
smali_classes11/com/securitytraining/weatherapp/BuildConfig.smali:13:.field public static final OPEN_WEATHER_API_KEY:Ljava/lang/String; = "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b"
smali_classes9/com/securitytraining/weatherapp/data/remote/WeatherApiService.smali:365:    const-string v0, "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b"
```

Confirma exactamente lo que dice §8 sobre este control: `BuildConfig` saca la key del historial de git y del APK como asset plano (ya no hay ningún archivo de config ni JSON extraíble con `unzip`), pero **no** la saca del binario — sigue en texto plano, trivialmente extraíble con `apktool`/`strings`/un decompilador. Un hallazgo adicional, más fuerte de lo esperado: el valor aparece **duplicado** en dos archivos smali distintos, no solo en `BuildConfig.smali`. Esto es porque `BuildConfig.OPEN_WEATHER_API_KEY` es una constante de compilación (`static final`), y tanto el compilador de Kotlin como R8 la **inlinean** en cada sitio donde se usa (acá, `WeatherApiService`), en vez de mantener una única referencia centralizada en runtime. El resultado práctico: cuantos más lugares del código lean la key, en más lugares del binario compilado queda copiada.

**La rotación de la key no cambia este hallazgo:** para dejarlo explícito (y no solo asumirlo), se repitió exactamente el mismo experimento con un segundo valor ficticio distinto (`9f3c8a21b4e6d0729c1a5f8b3d6e9012`, simulando "la key ya rotada" tras el incidente simulado descrito más arriba), reconstruyendo el APK desde cero:

```
$ apktool d -f -o /tmp/weatherapp-v0.4-rotated app/build/outputs/apk/debug/app-debug.apk
...
$ grep -rn "9f3c8a21b4e6d0729c1a5f8b3d6e9012" /tmp/weatherapp-v0.4-rotated
smali_classes9/com/securitytraining/weatherapp/data/remote/WeatherApiService.smali:365:    const-string v0, "9f3c8a21b4e6d0729c1a5f8b3d6e9012"
smali_classes11/com/securitytraining/weatherapp/BuildConfig.smali:13:.field public static final OPEN_WEATHER_API_KEY:Ljava/lang/String; = "9f3c8a21b4e6d0729c1a5f8b3d6e9012"
```

Mismas dos ubicaciones, exactamente el mismo patrón, con un valor completamente distinto. Esto confirma que rotar la key (revocar la vieja, emitir una nueva) resuelve el problema de "¿sigue siendo válida la que se filtró?", pero **no** resuelve "¿puede alguien extraer la key actual del APK?" — son dos problemas distintos, y `BuildConfig` por sí solo no ataca el segundo. La key real y efectivamente rotada por Guillermo en el dashboard de OpenWeatherMap sigue el mismo patrón (verificado por él mismo con su propio valor, nunca compartido con Claude ni pegado en este documento).

**Verificación del enmascarado de CI (post-push):** el primer run con el secret todavía sin crear mostró `OpenWeatherMap key in use -> ` (vacío — no había nada que enmascarar porque `secrets.OPEN_WEATHER_API_KEY` no existía). Una vez que Guillermo creó el secret `OPEN_WEATHER_API_KEY` en Settings → Secrets and variables → Actions con su key real y re-corrió el job, el mismo paso mostró `OpenWeatherMap key in use -> ***` — confirmando el contraste buscado: mismo comando que en v0.3 (donde salía en texto plano), ahora oculto porque el valor vive en un secret registrado.

**Pendiente para el documento final:** captura del log de GitHub Actions con el secret ya enmascarado (`***`), y captura de Android Studio mostrando `BuildConfig.OPEN_WEATHER_API_KEY` resuelto en el autocompletado/build.

## Etapa v0.5 — Restricción de key por proveedor: no disponible en OpenWeatherMap

**Referencia teórica:** §8.4 (restricción de keys por aplicación/dominio/IP como control complementario), §5 (limitaciones estructurales específicas de cada proveedor).

**Qué se hizo:** no hay cambio de código en esta etapa — es un hallazgo negativo, documentado como tal, no parcheado. Se verificó qué opciones de restricción ofrece el dashboard de OpenWeatherMap para una API key, contrastado contra un proveedor que sí ofrece ese control.

**Verificación (FAQ pública de OpenWeatherMap):**

> "You can create as many keys as you like... Usage from all API keys associated with your account is combined and counted toward the same account limits."

No hay mención en ningún punto de la documentación pública a restricción por package name de Android, huella SHA-1 del certificado de firma, HTTP referrer o IP. La única granularidad disponible es "cuántas keys tenés" y "borrar las que no uses" — los límites de uso (60 llamadas/min, 1.000.000/mes en el free tier) se aplican **a nivel de cuenta**, no por key individual.

**Contraste con un proveedor que sí lo ofrece (Google Maps Platform, confirmado contra su documentación oficial):**

> "Add the Android package name (from the AndroidManifest.xml file) and the SHA-1 signing certificate fingerprint of each Android application you want to authorize."

Google Maps Platform permite atar una key a un `applicationId` + SHA-1 concretos, de forma que aunque la key se filtre, solo funciona desde builds firmados con ese certificado específico. OpenWeatherMap no tiene ningún control equivalente.

**Qué significa esto para el proyecto:** confirma que §8.4 es un control real y valioso (y disponible en otros proveedores que Guillermo pueda usar en el futuro), pero **no aplicable** a OpenWeatherMap tal como está hoy. Esto refuerza por qué las etapas anteriores (BuildConfig, y las que siguen — certificate pinning, rotación) importan más acá que en un proveedor con restricción por app: sin esa restricción, rotación y minimizar la superficie de exposición son prácticamente los únicos controles disponibles del lado del cliente.

## Etapa v0.6 — Certificate pinning (SPKI) sobre el host de la API

**Referencia teórica:** §8 (controles complementarios de transporte), específicamente certificate/public-key pinning como defensa contra un MITM que presente un certificado válido emitido por una CA distinta a la esperada (CA comprometida, proxy corporativo con CA propia instalada, etc.).

**Qué se hizo:** se configuró `OkHttp.CertificatePinner` sobre el engine OkHttp de Ktor (`di/NetworkModule.kt`), fijando el hash SHA-256 de la clave pública (SPKI) del certificado hoja de `api.openweathermap.org` más el de su CA intermedia emisora como backup:

```kotlin
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
            install(ContentNegotiation) { /* ... */ }
        }
    }
}
```

**Por qué dos pines y no uno solo:** pinnear únicamente el certificado hoja es frágil — en cuanto el proveedor renueve ese certificado (cambia con cada renovación, acá vence 2027-03-25), la app queda rota hasta que se publique una nueva versión con el pin actualizado. Se agregó como respaldo el pin de la CA intermedia que lo emite (`Sectigo Public Server Authentication CA OV R36`, vence 2036-03-21): mientras OpenWeatherMap siga renovando certificados bajo la misma intermedia, el pin de backup sigue validando aunque cambie el hoja. Pinnear la raíz hubiese sido demasiado laxo para el propósito del ejercicio (equivale a confiar en casi cualquier cosa que esa CA raíz emita).

**Extracción de los pines — comandos reales, no inventados** (cadena completa obtenida con `openssl s_client -showcerts` contra el host real, hash SPKI calculado con la librería `cryptography` de Python sobre cada certificado de la cadena):

```
$ python3 -c "
import subprocess, re, hashlib, base64
from cryptography import x509
from cryptography.hazmat.primitives import serialization

out = subprocess.run(['openssl', 's_client', '-connect', 'api.openweathermap.org:443',
                       '-servername', 'api.openweathermap.org', '-showcerts'],
                      input=b'', capture_output=True, timeout=10).stdout.decode()
for pem in re.findall(r'-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----', out, re.S):
    cert = x509.load_pem_x509_certificate(pem.encode())
    pk_der = cert.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
    print(cert.subject.rfc4514_string(), '->', 'sha256/' + base64.b64encode(hashlib.sha256(pk_der).digest()).decode())
"
CN=*.openweathermap.org,O=Openweather Ltd.,ST=London\, City of,C=GB -> sha256/2rABlvP8a/45fRdYlmvSYEWrgBZyNampT8AqVpcPMtk=
CN=Sectigo Public Server Authentication CA OV R36,O=Sectigo Limited,C=GB -> sha256/KqkYYX5LYAYP7XGemqzbtPPIA8x7BS/BbOIcAXf3j2k=
CN=Sectigo Public Server Authentication Root R46,O=Sectigo Limited,C=GB -> sha256/Douxi77vs4G+Ib/BogbTFymEYq0QSFXwSgVCaZcI09Q=
CN=USERTrust RSA Certification Authority,O=The USERTRUST Network,L=Jersey City,ST=New Jersey,C=US -> sha256/x4QzPSC810K5/cMjb05Qm4k3Bw5zBn4lTdO/nEW/Td4=
```

(verificado el 2026-10-02; solo se usaron el primer y segundo hash — hoja e intermedia — como se explicó arriba).

**Verificación realizada en esta etapa:**
- `./gradlew clean :app:assembleDebug :app:testDebugUnitTest -POPEN_WEATHER_API_KEY=b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b` → `BUILD SUCCESSFUL`.
- Prueba funcional del pinning en sí (no solo que compila): un programa Java standalone (usando el mismo `OkHttpClient`/`CertificatePinner`, fuera del árbol de tests del proyecto) hizo dos conexiones reales a `api.openweathermap.org`:

  **Escenario 1 — pines correctos (los de arriba):**
  ```
  Result: HTTP 401 (TLS handshake + pin check succeeded)
  ```
  (401 porque el `appid` usado en la URL de prueba es inválido a propósito — lo relevante es que el handshake TLS y la verificación de pin pasaron sin error antes de llegar a la respuesta HTTP.)

  **Escenario 2 — un pin incorrecto a propósito:**
  ```
  Result: javax.net.ssl.SSLPeerUnverifiedException: Certificate pinning failure!
    Peer certificate chain:
      sha256/2rABlvP8a/45fRdYlmvSYEWrgBZyNampT8AqVpcPMtk=: CN=*.openweathermap.org, O=Openweather Ltd., ST="London, City of", C=GB
      sha256/KqkYYX5LYAYP7XGemqzbtPPIA8x7BS/BbOIcAXf3j2k=: CN=Sectigo Public Server Authentication CA OV R36, O=Sectigo Limited, C=GB
      sha256/Douxi77vs4G+Ib/BogbTFymEYq0QSFXwSgVCaZcI09Q=: CN=Sectigo Public Server Authentication Root R46, O=Sectigo Limited, C=GB
    Pinned certificates for api.openweathermap.org:
      sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
  ```

  Confirma el comportamiento esperado: OkHttp calcula el pin de cada certificado de la cadena real y lo compara contra la lista configurada; si ninguno coincide, aborta la conexión en la fase TLS antes de enviar cualquier dato de la petición (incluida la API key, que nunca llega a salir por el socket en este escenario). Esto es exactamente el escenario que el pinning está pensado para frenar: un atacante en posición de MITM con un certificado válido para el dominio pero emitido por una CA distinta a la esperada.

**Confirmación on-device (más fuerte que la prueba JVM aislada):** la prueba anterior corre fuera de la app, en un programa Java suelto. Para confirmar el mismo comportamiento dentro de la app real, se repitió el experimento en el emulador: se reemplazó temporalmente el pin correcto en `NetworkModule.kt` por un valor inventado, se agregó un `Log.e` temporal en el `onFailure` de `WeatherViewModel` (la cadena original solo guardaba `it.message` en el estado de UI sin loguear nada, y la UI además mostraba un string de error genérico fijo, no el mensaje real — por eso la excepción no era visible en ningún lado sin este cambio puntual), y se reconstruyó/instaló la app. Al buscar una ciudad, Logcat mostró:

```
E/WeatherViewModel: getCurrentWeather failed
javax.net.ssl.SSLPeerUnverifiedException: Certificate pinning failure!
  Peer certificate chain:
    sha256/2rABlvP8a/45fRdYlmvSYEWrgBZyNampT8AqVpcPMtk=: CN=*.openweathermap.org,O=Openweather Ltd.,ST=London\, City of,C=GB
    sha256/KqkYYX5LYAYP7XGemqzbtPPIA8x7BS/BbOIcAXf3j2k=: CN=Sectigo Public Server Authentication CA OV R36,O=Sectigo Limited,C=GB
    sha256/Douxi77vs4G+Ib/BogbTFymEYq0QSFXwSgVCaZcI09Q=: CN=Sectigo Public Server Authentication Root R46,O=Sectigo Limited,C=GB
  Pinned certificates for api.openweathermap.org:
    sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
	at okhttp3.CertificatePinner.check$okhttp(CertificatePinner.kt:209)
	at okhttp3.internal.connection.ConnectPlan.connectTls(ConnectPlan.kt:405)
	at okhttp3.internal.connection.ConnectPlan.connectTlsEtc(ConnectPlan.kt:213)
	at okhttp3.internal.connection.FastFallbackExchangeFinder.find(FastFallbackExchangeFinder.kt:80)
	at okhttp3.internal.connection.RealCall.initExchange$okhttp(RealCall.kt:306)
	... (cadena de interceptors de OkHttp) ...
	Suppressed: java.net.SocketException: Socket is closed
	Suppressed: javax.net.ssl.SSLPeerUnverifiedException: Certificate pinning failure! (mismo detalle)
```

Mismo resultado que en la prueba JVM, pero ahora en el stack TLS real de Android (Conscryptprovider), con el pool de hilos (`ThreadPoolExecutor`) y el dispatcher de OkHttp que usa la app en producción — confirma que el control funciona de punta a punta, no solo en un entorno de prueba simplificado. Ambos cambios temporales (pin falso + `Log.e` de diagnóstico) se revirtieron inmediatamente después de la captura; el árbol de trabajo quedó limpio, sin diferencias contra el commit de esta etapa.

**Qué no resuelve este control:** certificate pinning protege el **tránsito** (que nadie intercepte la conexión en la red), no el **almacenamiento** de la key en el cliente — sigue siendo extraíble del APK por `apktool`/decompilación, exactamente como se demostró en v0.4. Son controles complementarios, no sustitutos uno del otro.

**Pendiente para el documento final:** captura de Android Studio mostrando el bloque `certificatePinner(...)` en `NetworkModule.kt`, y captura de Logcat con la excepción de arriba (ya reproducida, ver evidencia on-device).

## Etapas v0.7 en adelante — *(pendientes)*

Ver el plan de trabajo para la lista completa (token de runtime en EncryptedSharedPreferences, purga de historial, cierre con SECURITY.md).
