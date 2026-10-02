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

## Etapa v0.7 — Token de instalación en runtime vía EncryptedSharedPreferences/Keystore

**Referencia teórica:** el documento es explícito en que `EncryptedSharedPreferences`/Keystore **no** son un control para proteger la key de un proveedor externo (eso se resuelve del lado servidor o, en su defecto, con los controles ya aplicados en v0.4/v0.6) — sirven para proteger un **secreto generado en runtime** después de algún flujo propio de la app (login, pairing, etc.). Como WeatherApp no tiene login, se agrega la mínima feature necesaria para ejercitar ese control de forma honesta: un token anónimo de instalación, generado una sola vez en el dispositivo y reutilizado en cada request.

**Qué se hizo:**

```kotlin
// data/local/InstallTokenStore.kt
class InstallTokenStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOrCreateToken(): String =
        preferences.getString(TOKEN_KEY, null)
            ?: UUID.randomUUID().toString().also { token ->
                preferences.edit().putString(TOKEN_KEY, token).apply()
            }
}
```

El token se genera con `UUID.randomUUID()` la primera vez que se pide, se persiste cifrado (clave maestra respaldada por Android Keystore, `AES256_GCM`), y de ahí en más se reutiliza. `WeatherApiService` lo adjunta como header custom en cada request:

```kotlin
header("X-Install-Token", installTokenStore.getOrCreateToken())
```

Separación de responsabilidades explícita: este token viaja en un header propio, nunca se mezcla con `appid` (la key de OpenWeatherMap) ni la reemplaza — son dos secretos de naturaleza distinta, con controles distintos, documentados por separado a propósito.

**Hallazgo real durante la implementación — la librería que el documento recomienda ya está deprecada:** al compilar, el propio compilador de Kotlin marcó `EncryptedSharedPreferences`, `MasterKey` y los enums de esquema de cifrado como `@Deprecated`. Se verificó contra fuentes externas (no solo el warning del compilador): toda la librería **Jetpack Security Crypto** fue deprecada a partir de la versión `1.1.0-beta01` (junio de 2025), **sin nuevas releases planeadas**, en favor de:

1. Uso directo de Android Keystore (`KeyGenParameterSpec` + `Cipher`), persistiendo el texto cifrado en `SharedPreferences`/`DataStore` normales — mismo nivel de seguridad, sin la dependencia deprecada.
2. Jetpack DataStore combinado con Tink (de Google) para cifrado a nivel de stream.

Fuentes: [Include Security — "EncryptedSharedPreferences is Dead"](https://blog.includesecurity.com/2026/08/encryptedsharedpreferences-is-dead-heres-what-you-should-use-instead/), [ProAndroidDev — "Goodbye EncryptedSharedPreferences: A 2026 Migration Guide"](https://proandroiddev.com/goodbye-encryptedsharedpreferences-a-2026-migration-guide-4b819b4a537a), [Android Developers Reference — EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences).

**Decisión para este ejercicio:** se mantiene `EncryptedSharedPreferences` de todos modos, porque (a) sigue siendo funcional y compila sin errores — solo warnings —, (b) es la API que el documento de referencia describe explícitamente, y el objetivo de esta etapa es ejercitar *ese* control tal como está documentado, y (c) migrar a Keystore directo o DataStore+Tink sería una reescritura fuera del alcance mínimo de este ejercicio. Queda documentado como nota honesta: en un proyecto real nuevo, hoy correspondería usar una de las dos alternativas vigentes en vez de esta librería.

**Verificación realizada en esta etapa:**
- `./gradlew :app:assembleDebug :app:testDebugUnitTest -POPEN_WEATHER_API_KEY=b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b` → `BUILD SUCCESSFUL` (con los warnings de deprecación arriba mencionados, sin errores).

**Evidencia on-device — el archivo de preferencias queda cifrado, no solo el token:**

```
$ adb shell run-as com.securitytraining.weatherapp \
    cat /data/data/com.securitytraining.weatherapp/shared_prefs/install_token_store.xml
```

Primera corrida (justo después de abrir la app, antes de buscar ninguna ciudad):

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="__androidx_security_crypto_encrypted_prefs_key_keyset__">12a901255c9896a2...</string>
    <string name="__androidx_security_crypto_encrypted_prefs_value_keyset__">1288015aa8bac2ce...</string>
</map>
```

Solo aparecen las *keysets* de Tink (las claves de cifrado en sí, generadas automáticamente al construir `InstallTokenStore` porque Koin resuelve todo el grafo de dependencias apenas la pantalla pide el `ViewModel` — y `WeatherApiService` necesita una instancia de `InstallTokenStore` en su constructor, aunque `getOrCreateToken()` todavía no se haya llamado). Después de buscar una ciudad en la app (disparando `fetchCurrentWeather()` → `getOrCreateToken()` por primera vez), el mismo archivo pasó a tener una tercera entrada:

```xml
<string name="AVbp3FE3zlgK2EONZbOCXQHyPA4AZ7e/NCtoQ8O0mTe1Zg==">ARLnWWCZs/UZCPHzMoDoGtJTPrCW+ScwtC4BWPtw6F4ckcwSZ/pgwSJE76FYlEFTc3rES4uOiz0xRYbBQYQJA5bCsNPbo8zf6Je/19M=</string>
```

Un hallazgo más fuerte de lo esperado: con `PrefKeyEncryptionScheme.AES256_SIV` (determinístico), **ni siquiera el nombre de la clave `"install_token"` queda en texto plano** — tanto el nombre (`AVbp3FE3...`) como el valor (el UUID, cifrado con `AES256_GCM`) son blobs ilegibles sin la master key respaldada por Android Keystore. Contraste directo con `local.properties`/`BuildConfig` (v0.4), donde la key de OpenWeatherMap queda legible en texto plano en el binario: acá, ni abriendo el archivo con acceso de `run-as` se puede leer el token sin acceso al Keystore del dispositivo.

**Qué no resuelve este control:** el token de instalación no reemplaza ni protege la API key de OpenWeatherMap — son secretos distintos. Tampoco pinnea nada ni afecta el transporte (eso ya lo cubre v0.6). Su único propósito es demostrar correctamente el único caso de uso para el que Keystore-backed storage tiene sentido según el documento: un secreto generado localmente en runtime.

**Pendiente para el documento final:** captura de Android Studio mostrando `InstallTokenStore.kt`.

## v0.8 — Demostración de remediación de historial de git (`git-filter-repo`)

**Alcance y por qué es una copia descartable:** en este proyecto nunca se filtró una key real (los valores "insecure" de v0.1–v0.3 son placeholders falsos). Por eso esta etapa no reescribe el historial real del repositorio — eso rompería la cadena de commits/tags que es la evidencia del ejercicio (ver `CLAUDE.md`: "Don't squash or rewrite this history"). En cambio, se clona el repo a una carpeta descartable fuera del proyecto y se demuestra ahí la técnica de purga que el documento de referencia recomienda (§8.2) para el caso real: un secreto que sí llegó a un remoto público.

**Herramienta:** [`git-filter-repo`](https://github.com/newren/git-filter-repo) (sucesor mantenido activamente de `git filter-branch`/BFG Repo-Cleaner, recomendado por la propia documentación de Git). Instalado en el entorno vía `pip3 install --user git-filter-repo`.

**Paso 1 — confirmar que el secreto placeholder de v0.1 (`b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b`) sigue recuperable en todo el historial real**, incluso en archivos que hoy ya no existen en el working tree:

```
$ git rev-list --all | xargs -I{} git grep -l "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b" {}
1802397:docs/evidencia-practica-es.md
2c67870:app/src/main/kotlin/com/securitytraining/weatherapp/core/Constants.kt
2c67870:docs/evidencia-practica-es.md
2cc6bf3:docs/evidencia-practica-es.md
77f77d0:app/src/main/assets/config.json
77f77d0:docs/evidencia-practica-es.md
a80d3bf:docs/evidencia-practica-es.md
c995706:docs/evidencia-practica-es.md
ceca952:docs/evidencia-practica-es.md
ceca952:.github/workflows/ci.yml
```

Hallazgo real no trivial: el secreto no solo vive en `Constants.kt` (v0.1) y `config.json` (v0.2, el archivo de configuración expuesto) — también quedó citado en el propio documento de evidencia (porque cada etapa pega snippets de código) y en el workflow de CI inseguro (`ceca952`, donde además se imprimía por `echo` en el log). Es exactamente el patrón que el documento de referencia advierte en §3: un secreto filtrado rara vez vive en un solo lugar.

**Paso 2 — clonar a una carpeta descartable y ejecutar la purga:**

```
$ git clone /home/guillermo/Projects/Personal/security/WeatherApp weatherapp-history-demo
$ cd weatherapp-history-demo
$ echo 'b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b==>***REMOVED***' > replace-rules.txt
$ git-filter-repo --replace-text replace-rules.txt --force
NOTICE: Removing 'origin' remote; see 'Why is my origin removed?'
        in the manual if you want to push back there.
Parsed 7 commits
New history written in 0.05 seconds; now repacking/cleaning...
Repacking your repo and cleaning out old unneeded objects
Completely finished after 0.13 seconds.
```

Nota de comportamiento real de la herramienta, no documentada explícitamente de antemano: `git-filter-repo` elimina el remoto `origin` automáticamente como medida de seguridad, precisamente para que nadie reescriba por accidente el historial de un remoto compartido sin un paso explícito e intencional.

**Paso 3 — verificar el resultado.** Los 7 commits fueron reescritos (todos los hashes cambian, no solo el de v0.1, porque cada commit incluye el hash de su padre):

| Etapa | Hash original | Hash reescrito |
|---|---|---|
| v0.1 | `2c67870` | `95ad86a` |
| v0.2 | `77f77d0` | `463e69c` |
| v0.3 | `ceca952` | `a6ed139` |
| v0.4 | `1802397` | `7731337` |
| v0.5 | `a80d3bf` | `8f07146` |
| v0.6 | `2cc6bf3` | `0818c5a` |
| v0.7 | `c995706` | `fbd8701` |

Y una búsqueda del mismo string sobre **todo** el historial reescrito ya no encuentra nada:

```
$ git rev-list --all | xargs -I{} git grep -l "b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b" {}
(sin salida — exit code 123, ningún blob contiene el string)
```

Confirmación puntual en el blob reescrito de `Constants.kt` (primer commit, hash nuevo `95ad86a`):

```kotlin
const val OPEN_WEATHER_API_KEY = "***REMOVED***"
```

Mismo resultado en `config.json` (v0.2) y en `ci.yml` (v0.3) — la sustitución corrió sobre los tres lugares donde vivía el secreto, no solo el "principal".

**Qué prueba esto y qué NO prueba:**
- Prueba que la mecánica de purga funciona exactamente como describe §8.2: reescribe cada blob/commit que contenía el secreto, en todo el árbol de historia, no solo en el `HEAD` actual.
- **No** prueba que esto sea suficiente como respuesta a un incidente real. Si el secreto ya fue pusheado a un remoto público (GitHub), cualquiera que ya haya hecho `fetch`/`clone` (incluidos forks, caches de GitHub, y el propio GitHub Archive) conserva los blobs viejos con el secreto indefinidamente — reescribir localmente y forzar el push no los hace desaparecer de ahí. Por eso el documento de referencia es explícito: **la única mitigación real ante una fuga ya pública es revocar/rotar la key** (lo que se documentó en v0.3 como el pivote hacia el key real desechable); la purga de historial es higiene adicional para evitar que quede trivialmente buscable en el propio repo, no un sustituto de la rotación.
- Tampoco se hizo sobre el repo real de este proyecto: como nunca hubo una key real comprometida, forzar un rewrite del historial real solo destruiría la evidencia encadenada que es el objetivo del ejercicio (commits/tags v0.1→v0.9). La carpeta `weatherapp-history-demo` se descartó después de capturar esta evidencia.

## v0.9 — Cierre: `SECURITY.md`, CI endurecido y falla controlada sin secreto

**Referencia teórica:** cierre general contra §13 (tabla de riesgos y controles) y §11 (verificación de controles, no solo su existencia).

**Qué se hizo — tres cambios concretos, no solo el documento de cierre:**

**1. `app/build.gradle.kts` ya no construye con una key vacía en silencio.** Antes, si ni `-POPEN_WEATHER_API_KEY` ni `local.properties` tenían el valor, `resolveOpenWeatherApiKey()` caía a `?: ""` — el build terminaba en `BUILD SUCCESSFUL` con una app que siempre iba a fallar en runtime con 401, sin ninguna señal en tiempo de compilación. Se reemplazó el fallback por una excepción explícita:

```kotlin
fun resolveOpenWeatherApiKey(): String =
    (project.findProperty("OPEN_WEATHER_API_KEY") as String?)
        ?: localProperties.getProperty("OPEN_WEATHER_API_KEY")
        ?: throw GradleException(
            "OPEN_WEATHER_API_KEY is not set. Provide it via " +
                "-POPEN_WEATHER_API_KEY=<key> or app/local.properties " +
                "(see CLAUDE.md for local setup / CI secret wiring).",
        )
```

**Verificación real, en un clon limpio (sin `local.properties`, igual que vería un runner de CI):**

```
$ ./gradlew :app:assembleDebug -Pandroid.useAndroidX=true
...
FAILURE: Build failed with an exception.
* Where: Build file '.../app/build.gradle.kts' line: 27
* What went wrong:
OPEN_WEATHER_API_KEY is not set. Provide it via -POPEN_WEATHER_API_KEY=<key> or app/local.properties (see CLAUDE.md for local setup / CI secret wiring).
BUILD FAILED in 5s
```

```
$ ./gradlew :app:assembleDebug -Pandroid.useAndroidX=true -POPEN_WEATHER_API_KEY=b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b
...
BUILD SUCCESSFUL in 3s
```

Confirmado en ambas direcciones: falla fuerte y clara sin el secreto, sigue construyendo normalmente con él.

**2. `.github/workflows/ci.yml` incorpora los dos pasos que quedaban pendientes del plan original de CI/CD:**
- Un paso que descarga y corre el binario de `gitleaks` v8.30.1 directamente (sin el wrapper `gitleaks-action`, que requiere licencia paga fuera de repos personales) sobre el historial completo (`fetch-depth: 0`).
- Un job separado (`verify-fails-without-secret`) que corre el build *sin* pasar la key y espera que falle — si en algún momento alguien revierte el cambio del punto 1 sin darse cuenta, este job lo detecta.

**3. Hallazgo real durante la implementación — el propio gitleaks iba a marcar en rojo cada corrida de CI para siempre.** Como el historial de este proyecto nunca se reescribe (ver `CLAUDE.md`), los placeholders ficticios de `v0.1`–`v0.4` (`b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b`, `9f3c8a21b4e6d0729c1a5f8b3d6e9012`) van a seguir apareciendo para siempre en commits viejos — y también citados en este mismo documento. Se verificó esto empíricamente antes de asumirlo:

```
$ gitleaks detect --source . -v --no-banner
...
leaks found: 11
```

Se agregó `.gitleaks.toml` con un *allowlist* acotado a estos dos valores exactos (no un `paths` que ignore archivos enteros, para no esconder un secreto real que caiga ahí):

```toml
[extend]
useDefault = true

[allowlist]
description = "Known fake placeholders used by this security-training exercise, never real secrets"
regexes = [
  '''b7e2f1a09c3d4e5f6a7b8c9d0e1f2a3b''',
  '''9f3c8a21b4e6d0729c1a5f8b3d6e9012''',
]
```

**Verificación real de que el allowlist funciona (no solo que el TOML es sintácticamente válido):** se corrió gitleaks dos veces sobre el mismo historial, con y sin el archivo de config presente.

```
# con .gitleaks.toml (el de este repo)
$ gitleaks detect --source . -v --no-banner
8 commits scanned.
no leaks found

# el mismo comando, config movida temporalmente fuera del repo
$ gitleaks detect --source . -v --no-banner
8 commits scanned.
leaks found: 11
```

El binario de `gitleaks` se descargó puntualmente para esta verificación (autorizado explícitamente por Guillermo en el momento, ya que el sistema de permisos bloquea por defecto que Claude descargue y ejecute binarios externos) y se borró del entorno apenas terminó la verificación.

**4. `SECURITY.md` (raíz del repo):** recorre cada fila de la tabla §13 del documento de referencia y documenta, para este proyecto puntual, si el control está **Aplicado** (con la etapa que lo hizo), **No aplica** (con el hallazgo verificado que lo justifica, p. ej. la restricción por SHA-1 de `v0.5`) o **Fuera de alcance** (con la decisión explícita detrás, p. ej. no backend proxy). Incluye además una síntesis mínima de "qué hacer ante una fuga real", construida a partir de lo ya ejercitado en `v0.3`–`v0.5` y `v0.8`, en vez de dejar ese punto del documento de referencia (§8.12, playbook de incidentes) sin ningún correlato práctico.

**Pendiente para el documento final:** nada nuevo de Android Studio en esta etapa (es la primera que no toca la app en sí) — sí vale capturar el run verde de GitHub Actions con los dos jobs nuevos (`build` con el paso de gitleaks, y `verify-fails-without-secret`).

---

## v0.10 — Secrets manager externo: Google Secret Manager + Workload Identity Federation

**Referencia teórica:** §8.3 ("Dónde y cómo almacenar los secretos" — jerarquía de opciones, con los *secrets managers* como nivel por encima de variables de entorno/secrets nativos de CI), ejercitado por primera vez de forma concreta en este proyecto — hasta `v0.9` solo se había usado el secret encriptado nativo de GitHub Actions (`v0.4`).

**Por qué esta etapa, después de haber cerrado en `v0.9`:** Guillermo propuso explícitamente sumar un secrets manager *distinto* al que ya provee GitHub, para ejercitar un control que el documento de referencia menciona pero que el proyecto no había probado todavía. Decisión de diseño: no se mete un secrets manager *dentro* de la app Android (reabriría el antipatrón de "secreto en el cliente" que ya se resolvió) — el lugar correcto es el pipeline de CI/CD, reemplazando de dónde saca la key, no cómo la usa la app.

**Decisión de proveedor — Google Secret Manager, con Workload Identity Federation (no una service-account key descargada):** la alternativa obvia y más simple (crear una service account, descargar su JSON de credenciales, pegarlo como un nuevo secret de GitHub) hubiese sido un paso atrás: reemplaza un secreto estático (la API key) por *otro* secreto estático (la credencial de GCP) guardado en el mismo lugar. Workload Identity Federation evita esto: GitHub Actions obtiene un token OIDC de corta duración, GCP lo cambia por credenciales temporales *solo si* el token dice venir del repo exacto configurado — no hay ninguna credencial de larga duración guardada en GitHub en ningún momento.

**Qué se hizo — enteramente ejecutado por Guillermo (requiere su propia cuenta de GCP; fuera del alcance de Claude en este entorno), guiado paso a paso:**

**1. Proyecto de GCP dedicado** (no reutilizar uno existente — mínimo privilegio / blast radius acotado):

```
$ gcloud init
...
Pick cloud project to use: [8] Create a new project
Project ID: weatherapp-sec-training-gm
Your current project has been set to: [weatherapp-sec-training-gm].
```

Proyecto: `weatherapp-sec-training-gm` — número de proyecto `309786944632` (confirmado en la consola).

**2. APIs necesarias habilitadas:**

```
$ gcloud services enable secretmanager.googleapis.com sts.googleapis.com iamcredentials.googleapis.com iam.googleapis.com --project="$PROJECT_ID"
Operation "operations/acat.p2-309786944632-..." finished successfully.
```

**3. El secreto en Secret Manager** (valor real tipeado directo por Guillermo vía stdin — nunca visible para Claude; se reutilizó a propósito uno de los placeholders ficticios ya usados en `v0.1`/`v0.4`, `9f3c8a21b4e6d0729c1a5f8b3d6e9012`, por continuidad y porque este documento también queda público):

```
$ gcloud secrets create OPEN_WEATHER_API_KEY --project="$PROJECT_ID" --replication-policy="automatic"
Created secret [OPEN_WEATHER_API_KEY].
$ printf '%s' "9f3c8a21b4e6d0729c1a5f8b3d6e9012" | gcloud secrets versions add OPEN_WEATHER_API_KEY --project="$PROJECT_ID" --data-file=-
Created version [1] of the secret [OPEN_WEATHER_API_KEY].
```

**4. Workload Identity Pool + Provider, acotado al repo exacto** (la parte que reemplaza "confío en quien tenga esta credencial" por "confío en un token que diga ser este repo puntual"):

```
$ gcloud iam workload-identity-pools create github-actions-pool --project="$PROJECT_ID" --location="global" --display-name="GitHub Actions"
Created workload identity pool [github-actions-pool].

$ gcloud iam workload-identity-pools providers create-oidc github-actions-provider \
  --project="$PROJECT_ID" --location="global" --workload-identity-pool="github-actions-pool" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository == 'guillermomartinavenga-code/WeatherApp'"
Created workload identity pool provider [github-actions-provider].
```

**5. Service account dedicada + los dos bindings de mínimo privilegio** (ninguno a nivel de proyecto):

```
$ gcloud iam service-accounts create github-actions-weatherapp --project="$PROJECT_ID" --display-name="GitHub Actions - WeatherApp CI"
Created service account [github-actions-weatherapp].

# (a) la service account puede leer *este* secreto puntual, no el proyecto entero
$ gcloud secrets add-iam-policy-binding OPEN_WEATHER_API_KEY --project="$PROJECT_ID" \
  --role="roles/secretmanager.secretAccessor" \
  --member="serviceAccount:github-actions-weatherapp@weatherapp-sec-training-gm.iam.gserviceaccount.com"
Updated IAM policy for secret [OPEN_WEATHER_API_KEY].

# (b) solo este repo de GitHub puede "pedir prestada" esta service account
$ gcloud iam service-accounts add-iam-policy-binding github-actions-weatherapp@weatherapp-sec-training-gm.iam.gserviceaccount.com \
  --project="$PROJECT_ID" --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/309786944632/locations/global/workloadIdentityPools/github-actions-pool/attribute.repository/guillermomartinavenga-code/WeatherApp"
Updated IAM policy for serviceAccount [github-actions-weatherapp@weatherapp-sec-training-gm.iam.gserviceaccount.com].
```

**6. Tres variables de repositorio en GitHub** (Settings → Secrets and variables → Actions → pestaña **Variables**, no Secrets — ninguno de estos tres valores es sensible por sí solo, son identificadores de recursos):

| Variable | Valor |
|---|---|
| `GCP_PROJECT_ID` | `weatherapp-sec-training-gm` |
| `GCP_SERVICE_ACCOUNT_EMAIL` | `github-actions-weatherapp@weatherapp-sec-training-gm.iam.gserviceaccount.com` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/309786944632/locations/global/workloadIdentityPools/github-actions-pool/providers/github-actions-provider` |

**7. `.github/workflows/ci.yml`:** se quitó el `env: OPEN_WEATHER_API_KEY: ${{ secrets.OPEN_WEATHER_API_KEY }}` a nivel de job (la fuente de verdad desde `v0.4`) y se agregaron dos steps antes del resto del pipeline:

```yaml
permissions:
  contents: read
  id-token: write   # necesario para pedir el token OIDC de GitHub

steps:
  - id: auth
    uses: google-github-actions/auth@v3
    with:
      project_id: ${{ vars.GCP_PROJECT_ID }}
      workload_identity_provider: ${{ vars.GCP_WORKLOAD_IDENTITY_PROVIDER }}
      service_account: ${{ vars.GCP_SERVICE_ACCOUNT_EMAIL }}

  - id: secrets
    uses: google-github-actions/get-secretmanager-secrets@v3
    with:
      secrets: |-
        OPEN_WEATHER_API_KEY:${{ vars.GCP_PROJECT_ID }}/OPEN_WEATHER_API_KEY
```

Los steps siguientes (`Debug print build environment`, `Assemble debug APK`, `Run unit tests`) pasaron de leer `env.OPEN_WEATHER_API_KEY` (del job) a `steps.secrets.outputs.OPEN_WEATHER_API_KEY`, sin otro cambio.

**Verificación de versiones y comportamiento de las Actions oficiales de Google, antes de confiar en ellas (no asumido):** se confirmó contra la documentación real de ambos repos (`google-github-actions/auth`, `google-github-actions/get-secretmanager-secrets`) que la versión mayor estable actual es `v3` para ambas, y que `get-secretmanager-secrets` enmascara automáticamente ("After a secret is accessed, its value is added to the mask of the build") cualquier valor que obtiene — el mismo mecanismo de enmascarado que GitHub usa para sus propios secrets, solo que invocado por esta Action en vez de nativamente.

**Verificación real — corrida completa de GitHub Actions tras el push, pegada por Guillermo:**

- Job `build`, step **"Authenticate to Google Cloud (Workload Identity Federation)"**: ✅, generó el archivo de credenciales temporal (`gha-creds-....json`) sin ningún secreto estático involucrado.
- Job `build`, step **"Fetch OPEN_WEATHER_API_KEY from Secret Manager"**: ✅.
- Job `build`, step **"Debug print build environment"**:
  ```
  OpenWeatherMap key in use -> ***
  ```
  Confirma el enmascarado automático de la Action de Google — mismo resultado visual que el secret nativo de GitHub en `v0.4`, ahora con la key viniendo de un sistema distinto.
- Job `build` completo: **succeeded** (incluye `assembleDebug`, `testDebugUnitTest` y el scan de `gitleaks` de `v0.9`, todos sin cambios de comportamiento).
- Job `verify-fails-without-secret` (de `v0.9`, sin relación con GCP): **succeeded**, mostrando la falla esperada de Gradle (`OPEN_WEATHER_API_KEY is not set...`) seguida de `OK: build failed as expected when the secret is absent.` — confirma que el control de `v0.9` sigue intacto después de este cambio.

**Qué no cambia respecto a `v0.4`:** el build local (`local.properties`) no se tocó — Workload Identity Federation solo tiene sentido para una identidad de máquina como un runner de CI, no para la laptop de un desarrollador. La app en sí tampoco cambió: sigue leyendo `BuildConfig.OPEN_WEATHER_API_KEY`, sin ninguna dependencia nueva ni lógica de secrets manager embebida en el cliente Android.

**Próximo paso manual de Guillermo:** una vez verificado que el flujo nuevo funciona (confirmado arriba), borrar el secret `OPEN_WEATHER_API_KEY` nativo de GitHub Actions (pestaña **Secrets**, no Variables) — ya no lo usa ningún workflow, y dejarlo sin usar es justamente el antipatrón de "secreto sin trazabilidad" que se quiere evitar.

**Capturas de la consola de GCP (ya tomadas) — dos hallazgos no anticipados:**

- **Secret Manager → `OPEN_WEATHER_API_KEY` → pestaña Permisos:** un único miembro con acceso explícito, `github-actions-weatherapp@weatherapp-sec-training-gm.iam.gserviceaccount.com`, rol "Usuario con acceso a secretos de Secret Manager" (`roles/secretmanager.secretAccessor`) — exactamente el binding de mínimo privilegio creado por `gcloud`, visible y auditable desde la consola, no solo desde la CLI.
- **Workload Identity Pool `github-actions-pool` → panel de uso:** el gráfico "Recuento de intercambios de tokens correctos mediante la federación de identidades para cargas de trabajo" mostró actividad real (~0.00667/s) coincidiendo con la hora del run de GitHub Actions — confirma que la federación se **usó** de verdad en una corrida real, no que quedó solamente configurada sin ejercitarse.
- **Mismo pool → pestaña "Cuentas de servicio conectadas":** muestra la condición de atributo tal como quedó aplicada, `attribute.repository="guillermomartinavenga-code/WeatherApp"` — confirmación visual, desde la UI de GCP (no solo desde el comando de creación), de que el alcance quedó acotado al repo exacto.
- **Cuentas de servicio → `github-actions-weatherapp` → pestaña Claves:** la lista está vacía ("No hay filas para mostrar"). Más fuerte que lo esperado: la propia consola de Google muestra un banner de advertencia en esa pantalla, textual: *"Las claves de cuenta de servicio podrían poner en riesgo la seguridad si se ven comprometidas. Te recomendamos que no descargues claves de cuenta de servicio y que, en su lugar, uses la Federación de identidades para cargas de trabajo."* — es la plataforma misma, en su propia interfaz, validando la decisión de diseño de esta etapa (preferir WIF por sobre una key de service account descargada), no una recomendación externa de un blog o del documento de referencia.

---

Con esta etapa se extiende el recorrido planificado (`v0.1`→`v0.10`). El estado de cada control queda consolidado en [`SECURITY.md`](../SECURITY.md); este documento sigue siendo la bitácora paso a paso de cómo se llegó a eso.
