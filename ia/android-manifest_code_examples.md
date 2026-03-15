# Manifiesto Android — Ejemplos de Código

> Documento complementario al **Manifiesto de Desarrollo Android**.  
> Los números de sección corresponden exactamente al índice del manifiesto principal.  
> Todo el código fue extraído de repositorios y documentación oficiales de Google/Android:
>
> - [`android/nowinandroid`](https://github.com/android/nowinandroid) — app de referencia oficial
> - [`android/architecture-samples`](https://github.com/android/architecture-samples) — samples de arquitectura
> - [`android/security-samples`](https://github.com/android/security-samples) — samples de seguridad
> - [`android/performance-samples`](https://github.com/android/performance-samples) — samples de rendimiento
> - [developer.android.com](https://developer.android.com) — documentación oficial

---

## Índice de Ejemplos

- [2.3 — MVVM: ViewModel con StateFlow](#23-patrones-mvvm--mvi)
- [2.3 — UI State como data class inmutable](#23-patrones-mvvm--mvi)
- [2.3 — Collect en Compose con repeatOnLifecycle](#23-patrones-mvvm--mvi)
- [2.4 — Inyección de Dependencias con Hilt](#24-inyección-de-dependencias-con-hilt)
- [2.5 — Navegación con Navigation Compose](#25-navegación-y-modularización)
- [2.5 — Estructura de módulos Gradle](#25-navegación-y-modularización)
- [3.2 — Accesibilidad en Compose (semantics)](#32-accesibilidad-a11y)
- [4.1 — Datos cifrados con EncryptedSharedPreferences](#41-seguridad-de-datos-y-comunicaciones)
- [4.2 — Solicitud de permiso en runtime](#42-permisos-y-principio-de-mínimo-privilegio)
- [4.3 — Autenticación biométrica con BiometricPrompt](#43-identidad-y-autenticación)
- [5.1 — Baseline Profile + Macrobenchmark](#51-arranque-de-la-app-app-startup)
- [5.2 — WorkManager con constraints](#54-batería-y-trabajo-en-segundo-plano)
- [5.3 — LazyColumn con keys para evitar jank](#52-rendering-y-jank)
- [6 — Unit test de ViewModel con Turbine](#6-testing)
- [6 — UI test con Compose Testing API](#6-testing)

---

## 2.3 Patrones MVVM / MVI

### ViewModel con StateFlow — Patrón backing property

Extraído de la documentación oficial de [`developer.android.com/topic/architecture/ui-layer/state-production`](https://developer.android.com/topic/architecture/ui-layer/state-production) y del sample [`android/nowinandroid` — ForYouViewModel](https://github.com/android/nowinandroid/blob/main/feature/foryou/src/main/kotlin/com/google/samples/apps/nowinandroid/feature/foryou/ForYouViewModel.kt):

```kotlin
// Fuente: android/nowinandroid — ForYouViewModel.kt
// https://github.com/android/nowinandroid

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    syncManager: SyncManager,
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ViewModel() {

    // El StateFlow se expone como read-only a la UI
    // La lógica interna usa el flujo de datos del repositorio directamente
    val uiState: StateFlow<ForYouUiState> = ...
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ForYouUiState.Loading,
        )
}
```

### UI State como data class inmutable

Extraído de [`developer.android.com/topic/architecture/ui-layer/state-production`](https://developer.android.com/topic/architecture/ui-layer/state-production):

```kotlin
// Fuente: documentación oficial de Android — UI State Production
// https://developer.android.com/topic/architecture/ui-layer/state-production

// ✅ Estado modelado como data class inmutable
data class AddEditTaskUiState(
    val title: String = "",
    val description: String = "",
    val isTaskCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isTaskSaved: Boolean = false,
)

class AddEditTaskViewModel(...) : ViewModel() {

    // Propiedad privada mutable — solo modificable dentro del ViewModel
    private val _uiState = MutableStateFlow(AddEditTaskUiState())

    // Expuesto como read-only StateFlow a la UI
    val uiState: StateFlow<AddEditTaskUiState> = _uiState.asStateFlow()

    private fun createNewTask() {
        viewModelScope.launch {
            val newTask = Task(uiState.value.title, uiState.value.description)
            try {
                tasksRepository.saveTask(newTask)
                // Actualizar el estado de forma inmutable con copy()
                _uiState.update { it.copy(isTaskSaved = true) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (exception: Exception) {
                _uiState.update { it.copy(userMessage = getErrorMessage(exception)) }
            }
        }
    }
}
```

### Combinar múltiples flows en un único UiState

Extraído de la documentación oficial de [`developer.android.com/kotlin/flow/stateflow-and-sharedflow`](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow):

```kotlin
// Fuente: documentación oficial — StateFlow and SharedFlow
// https://developer.android.com/kotlin/flow/stateflow-and-sharedflow

class LatestNewsViewModel(
    private val newsRepository: NewsRepository,
) : ViewModel() {

    // Backing property para evitar actualizaciones de estado desde otras clases
    private val _uiState = MutableStateFlow(LatestNewsUiState.Success(emptyList()))

    // La UI colecta este StateFlow para obtener actualizaciones de estado
    val uiState: StateFlow<LatestNewsUiState> = _uiState

    init {
        viewModelScope.launch {
            newsRepository.favoriteLatestNews
                .collect { favoriteNews ->
                    _uiState.value = LatestNewsUiState.Success(favoriteNews)
                }
        }
    }
}

// Estados posibles modelados como sealed class
sealed class LatestNewsUiState {
    data class Success(val news: List<ArticleHeadline>) : LatestNewsUiState()
    data class Error(val exception: Throwable) : LatestNewsUiState()
}
```

### Collect en la UI con repeatOnLifecycle (lifecycle-aware)

Extraído de la documentación oficial de [`developer.android.com/kotlin/flow/stateflow-and-sharedflow`](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow):

```kotlin
// Fuente: documentación oficial — StateFlow and SharedFlow
// IMPORTANTE: Nunca usar launch {} directamente; siempre repeatOnLifecycle

class LatestNewsActivity : AppCompatActivity() {
    private val latestNewsViewModel: LatestNewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // repeatOnLifecycle lanza el bloque cuando el lifecycle está en STARTED
            // y lo cancela automáticamente cuando pasa a STOPPED
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                latestNewsViewModel.uiState.collect { uiState ->
                    when (uiState) {
                        is LatestNewsUiState.Success -> showFavoriteNews(uiState.news)
                        is LatestNewsUiState.Error   -> showError(uiState.exception)
                    }
                }
            }
        }
    }
}
```

---

## 2.4 Inyección de Dependencias con Hilt

Extraído del sample oficial [`android/nowinandroid`](https://github.com/android/nowinandroid):

```kotlin
// Fuente: android/nowinandroid — ForYouViewModel.kt
// https://github.com/android/nowinandroid

// ✅ @HiltViewModel para ViewModels con dependencias inyectadas
@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    syncManager: SyncManager,
    private val analyticsHelper: AnalyticsHelper,
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ViewModel() { /* ... */ }
```

```kotlin
// Fuente: android/nowinandroid — patrón de Module con Hilt
// Binding de interfaz a implementación concreta en el módulo de datos

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // La interfaz NewsRepository es lo que usan las capas superiores.
    // La implementación real queda encapsulada en la capa de datos.
    @Binds
    abstract fun bindsNewsRepository(
        newsRepository: OfflineFirstNewsRepository,
    ): NewsRepository

    @Binds
    abstract fun bindsUserDataRepository(
        userDataRepository: OfflineFirstUserDataRepository,
    ): UserDataRepository
}
```

---

## 2.5 Navegación y Modularización

### Navigation Compose — grafo de navegación

Extraído del sample [`android/architecture-samples`](https://github.com/android/architecture-samples) y documentación oficial:

```kotlin
// Fuente: android/architecture-samples
// https://github.com/android/architecture-samples
// Arquitectura: single-activity + Navigation Compose

@Composable
fun TodoNavGraph(
    appState: TodoAppState = rememberTodoAppState(),
) {
    val navController = appState.navController

    NavHost(
        navController = navController,
        startDestination = TodoDestinations.TASKS_ROUTE,
    ) {
        // Destino: lista de tareas
        composable(TodoDestinations.TASKS_ROUTE) {
            TasksScreen(
                onAddTask = { navController.navigate(TodoDestinations.ADD_EDIT_TASK_ROUTE) },
                onTaskClick = { task ->
                    navController.navigate("${TodoDestinations.TASK_DETAIL_ROUTE}/${task.id}")
                },
            )
        }

        // Destino: detalle de tarea (recibe ID como argumento)
        composable(
            route = "${TodoDestinations.TASK_DETAIL_ROUTE}/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskDetailScreen(onEditTask = { taskId ->
                navController.navigate("${TodoDestinations.ADD_EDIT_TASK_ROUTE}?taskId=$taskId")
            })
        }
    }
}
```

### Estructura de módulos Gradle — nowinandroid

Extraído del [`android/nowinandroid — ModularizationLearningJourney.md`](https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md):

```
// Fuente: android/nowinandroid — estructura de módulos
// https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md

:app                          ← módulo raíz, solo glue code

:feature:foryou               ← feature module: pantalla "For You"
:feature:bookmarks            ← feature module: pantalla "Bookmarks"
:feature:interests            ← feature module: pantalla "Interests"
:feature:search               ← feature module: pantalla "Search"

:core:data                    ← repositorios, modelos de dominio
:core:database                ← Room DAOs, entidades, migrations
:core:network                 ← Retrofit, DTOs de red, API service
:core:datastore               ← Proto DataStore, preferencias
:core:ui                      ← componentes Compose reutilizables
:core:designsystem            ← tema M3, tokens de color, tipografía
:core:common                  ← utilidades Kotlin compartidas
:core:testing                 ← fakes, test utilities compartidas

:sync                         ← WorkManager sync workers
:benchmark                    ← Macrobenchmark + Baseline Profile
```

**Regla clave del sample oficial:**

> `feature` modules solo dependen de `core` modules.  
> `feature` modules **nunca** dependen de otros `feature` modules.

```kotlin
// build.gradle.kts de un feature module (ejemplo: :feature:foryou)
// Fuente: android/nowinandroid

plugins {
    alias(libs.plugins.nowinandroid.android.feature.impl)
    alias(libs.plugins.nowinandroid.android.library.compose)
}

dependencies {
    implementation(projects.core.data)        // ✅ depende de core
    implementation(projects.core.ui)          // ✅ depende de core
    implementation(projects.core.designsystem) // ✅ depende de core
    // ❌ NUNCA: implementation(projects.feature.bookmarks)
}
```

---

## 3.2 Accesibilidad (A11y)

Extraído de la documentación oficial de Compose [`developer.android.com/develop/ui/compose/accessibility`](https://developer.android.com/develop/ui/compose/accessibility):

```kotlin
// Fuente: documentación oficial — Compose Accessibility
// https://developer.android.com/develop/ui/compose/accessibility

// ✅ contentDescription para imágenes sin texto visible
Image(
    painter = painterResource(id = R.drawable.ic_add),
    contentDescription = stringResource(R.string.cd_add_transaction), // "Agregar transacción"
)

// ✅ semantics para agrupar elementos relacionados
// Evita que TalkBack anuncie cada elemento por separado
Row(
    modifier = Modifier.semantics(mergeDescendants = true) {}
) {
    Icon(Icons.Default.AccountBalance, contentDescription = null) // null: el Row ya tiene semántica
    Text("Saldo total: $1.250,00")
}

// ✅ Rol explícito para elementos interactivos no estándar
Box(
    modifier = Modifier
        .clickable { onToggleFavorite() }
        .semantics {
            role = Role.Checkbox
            contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
            stateDescription = if (isFavorite) "Marcado" else "Sin marcar"
        }
) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = null, // ya está en el Box semantics
    )
}

// ✅ Touch target mínimo 48x48dp — usar sizeIn o minimumInteractiveComponentSize
IconButton(
    onClick = onDeleteClick,
    modifier = Modifier.minimumInteractiveComponentSize(), // garantiza 48x48dp
) {
    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
}
```

---

## 4.1 Seguridad de Datos y Comunicaciones

### EncryptedSharedPreferences para datos sensibles

Extraído de la documentación oficial de Jetpack Security [`developer.android.com/topic/security/data`](https://developer.android.com/topic/security/data):

```kotlin
// Fuente: documentación oficial — Jetpack Security (EncryptedSharedPreferences)
// https://developer.android.com/topic/security/data
// Dependencia: androidx.security:security-crypto:1.1.0-alpha06

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// 1. Crear/obtener la clave maestra del Android Keystore
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

// 2. Crear SharedPreferences cifradas
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",              // nombre del archivo
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
)

// 3. Usar exactamente igual que SharedPreferences normales
encryptedPrefs.edit()
    .putString("auth_token", token)
    .apply()

val savedToken = encryptedPrefs.getString("auth_token", null)
// La clave y el valor están cifrados en disco con AES-256
```

### Network Security Config — deshabilitar cleartext en producción

```xml
<!-- res/xml/network_security_config.xml -->
<!-- Fuente: developer.android.com/training/articles/security-config -->

<network-security-config>
    <!-- Producción: solo HTTPS, sin cleartext -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.miapp.com</domain>
    </domain-config>

    <!-- Debug: permitir cleartext solo en localhost para desarrollo -->
    <debug-overrides>
        <trust-anchors>
            <certificates src="user" />
        </trust-anchors>
    </debug-overrides>
</network-security-config>
```

```xml
<!-- AndroidManifest.xml -->
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

---

## 4.2 Permisos y Principio de Mínimo Privilegio

Extraído de la documentación oficial [`developer.android.com/training/permissions/requesting`](https://developer.android.com/training/permissions/requesting):

```kotlin
// Fuente: documentación oficial — Request runtime permissions
// https://developer.android.com/training/permissions/requesting

// ✅ Patrón correcto: solicitar permiso just-in-time con ActivityResultContracts

val requestPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // Permiso concedido — proceder con la funcionalidad
        startCameraScanner()
    } else {
        // Permiso denegado — degradar con gracia
        showManualEntryForm()
    }
}

// Mostrar rationale antes de solicitar si es necesario
@Composable
fun ScanReceiptButton(onScanClick: () -> Unit) {
    val context = LocalContext.current

    Button(onClick = {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Ya tiene permiso
                onScanClick()
            }
            shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.CAMERA) -> {
                // Mostrar explicación al usuario antes de pedir
                showCameraRationaleDialog { requestPermissionLauncher.launch(Manifest.permission.CAMERA) }
            }
            else -> {
                // Primera vez — solicitar directamente
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }) {
        Text("Escanear recibo")
    }
}
```

---

## 4.3 Identidad y Autenticación

### BiometricPrompt con CryptoObject

Extraído del sample oficial [`android/security-samples — BiometricLoginKotlin`](https://github.com/android/security-samples/tree/main/BiometricLoginKotlin):

```kotlin
// Fuente: android/security-samples — BiometricLoginKotlin (sample canónico)
// https://github.com/android/security-samples/tree/main/BiometricLoginKotlin
// Dependencia: androidx.biometric:biometric:1.2.0-alpha05

// Dependencia en build.gradle.kts:
// implementation("androidx.biometric:biometric:1.2.0-alpha05")

fun showBiometricPromptForDecryption(
    activity: FragmentActivity,
    cipher: Cipher,           // Cipher inicializado desde Android Keystore
    onSuccess: (Cipher) -> Unit,
    onError: (String) -> Unit,
) {
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Verificar identidad")
        .setSubtitle("Use su biometría para acceder")
        .setNegativeButtonText("Usar contraseña")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        .build()

    val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                // El Cipher ya fue desbloqueado por el hardware biométrico
                result.cryptoObject?.cipher?.let { unlockedCipher ->
                    onSuccess(unlockedCipher)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                // Intento fallido pero se puede reintentar — no hacer nada aquí
            }
        },
    )

    // Vincular el Cipher al prompt para que el hardware lo desbloquee
    biometricPrompt.authenticate(
        promptInfo,
        BiometricPrompt.CryptoObject(cipher),
    )
}
```

---

## 5.1 Arranque de la App (App Startup)

### Macrobenchmark para medir Cold Start

Extraído directamente del sample oficial [`android/performance-samples — MacrobenchmarkSample`](https://github.com/android/performance-samples/tree/main/MacrobenchmarkSample):

```kotlin
// Fuente: android/performance-samples — SampleStartupBenchmark.kt
// https://github.com/android/performance-samples/tree/main/MacrobenchmarkSample

package com.example.macrobenchmark.benchmark.startup

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SampleStartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    // Sin compilación AOT: peor caso (fresh install sin Baseline Profile)
    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    // Con Baseline Profile: representa el caso real en producción
    @Test
    fun startupBaselineProfile() = startup(CompilationMode.Partial())

    private fun startup(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilationMode,
            iterations = 10,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Asegurar que la app esté cerrada antes de medir
                pressHome()
            },
        ) {
            startActivityAndWait()
        }
}

// Resultado típico en Pixel 7 (tomado de la doc oficial):
// NoCompilation:   324.8ms
// BaselineProfile: 229.0ms  ← ~30% de mejora
```

### Baseline Profile — generación

Extraído de la documentación oficial [`developer.android.com/topic/performance/baselineprofiles`](https://developer.android.com/topic/performance/baselineprofiles):

```kotlin
// Fuente: android/nowinandroid — BaselineProfileGenerator
// https://github.com/android/nowinandroid

@RunWith(AndroidJUnit4ClassRunner::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.google.samples.apps.nowinandroid",
    ) {
        // Cubrir los critical user paths: login, pantalla principal, scrolling
        startActivityAndWait()

        // Navegar a la pantalla principal y hacer scroll para precompilar
        // el código de rendering de LazyColumn
        device.findObject(By.text("For you")).click()
        device.waitForIdle()

        repeat(3) {
            device.findObject(By.scrollable(true)).scroll(Direction.DOWN, 1f)
        }
    }
}
```

```
# El archivo generado app/src/main/baseline-prof.txt tiene este aspecto:
# Fuente: android/nowinandroid/app/src/main/baseline-prof.txt

HSPLcom/google/samples/apps/nowinandroid/MainActivity;->onCreate(Landroid/os/Bundle;)V
HSPLcom/google/samples/apps/nowinandroid/ui/NiaApp;->invoke(Landroid/app/Activity;)V
...
# H = Hot method (compilado AOT como código nativo)
# S = Startup (ejecutado durante el inicio de la app)
# P = Post-startup (ejecutado poco después del inicio)
```

---

## 5.2 Rendering y Jank

### LazyColumn con keys para evitar recomposiciones innecesarias

Extraído de la documentación oficial de Compose [`developer.android.com/develop/ui/compose/lists`](https://developer.android.com/develop/ui/compose/lists):

```kotlin
// Fuente: documentación oficial — Compose Lists
// https://developer.android.com/develop/ui/compose/lists

// ❌ SIN keys: Compose no puede identificar qué ítem cambió
// → re-renderiza toda la lista cuando un elemento se actualiza
LazyColumn {
    items(transactions) { transaction ->
        TransactionItem(transaction)
    }
}

// ✅ CON keys: Compose identifica qué ítem cambió y recompone solo ese
LazyColumn {
    items(
        items = transactions,
        key = { transaction -> transaction.id }, // ID estable y único
    ) { transaction ->
        TransactionItem(
            transaction = transaction,
            modifier = Modifier.animateItem(), // animaciones fluidas en inserciones/eliminaciones
        )
    }
}
```

### derivedStateOf para evitar recomposiciones en scroll

```kotlin
// Fuente: documentación oficial — Compose State
// https://developer.android.com/develop/ui/compose/state#derivedstateof

@Composable
fun TransactionListScreen(transactions: List<Transaction>) {
    val listState = rememberLazyListState()

    // ✅ derivedStateOf: el FAB solo se recompone cuando cambia firstVisibleItemIndex,
    // no en cada pixel de scroll
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Box {
        LazyColumn(state = listState) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionItem(transaction)
            }
        }

        if (showScrollToTop) {
            FloatingActionButton(
                onClick = { /* scroll to top */ },
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Icon(Icons.Default.KeyboardArrowUp, "Volver arriba")
            }
        }
    }
}
```

---

## 5.4 Batería y Trabajo en Segundo Plano

### WorkManager con constraints

Extraído de la documentación oficial [`developer.android.com/develop/background-work/background-tasks/persistent/getting-started`](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started) y del sample [`android/nowinandroid — SyncWorker`](https://github.com/android/nowinandroid):

```kotlin
// Fuente: android/nowinandroid — SyncWorker + documentación oficial WorkManager
// https://github.com/android/nowinandroid

// 1. Definir el Worker
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Sincronizar datos: operaciones en Dispatchers.IO automáticamente
            syncRepository.sync()
            Result.success()
        } catch (e: Exception) {
            // Reintentar con backoff exponencial automático
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "SyncWorker"
    }
}

// 2. Encolar el trabajo con constraints
val syncConstraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED) // solo con red
    .build()

val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
    repeatInterval = 1,
    repeatIntervalTimeUnit = TimeUnit.HOURS,
)
    .setConstraints(syncConstraints)
    .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        WorkRequest.MIN_BACKOFF_MILLIS,
        TimeUnit.MILLISECONDS,
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    SyncWorker.WORK_NAME,
    ExistingPeriodicWorkPolicy.KEEP, // no reencolar si ya existe
    syncRequest,
)
```

---

## 6. Testing

### Unit test de ViewModel con Turbine (flows)

Extraído del patrón de testing de [`android/nowinandroid`](https://github.com/android/nowinandroid) y documentación oficial:

```kotlin
// Fuente: android/nowinandroid — patrón de testing de ViewModels
// https://github.com/android/nowinandroid
// Dependencias: app.cash.turbine:turbine, org.jetbrains.kotlinx:kotlinx-coroutines-test

@OptIn(ExperimentalCoroutinesApi::class)
class ForYouViewModelTest {

    // Regla para usar TestDispatcher en coroutines de test
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Fake del repositorio (no mock) — patrón preferido en nowinandroid
    private val userDataRepository = FakeUserDataRepository()
    private val newsRepository = FakeNewsRepository()

    private lateinit var viewModel: ForYouViewModel

    @Before
    fun setup() {
        viewModel = ForYouViewModel(
            userDataRepository = userDataRepository,
            newsRepository = newsRepository,
        )
    }

    @Test
    fun `uiState emite Loading como estado inicial`() = runTest {
        // Turbine: API fluida para testear flows
        viewModel.uiState.test {
            assertEquals(ForYouUiState.Loading, awaitItem())
            cancel()
        }
    }

    @Test
    fun `uiState emite Success cuando el repositorio tiene datos`() = runTest {
        viewModel.uiState.test {
            awaitItem() // consumir el estado Loading inicial

            // Emitir datos desde el fake repositorio
            newsRepository.emitNewsResources(testNewsResources)

            val successState = awaitItem()
            assertIs<ForYouUiState.Success>(successState)
            assertEquals(testNewsResources.size, successState.feed.size)
            cancel()
        }
    }
}
```

### UI test con Compose Testing API

Extraído de la documentación oficial [`developer.android.com/develop/ui/compose/testing`](https://developer.android.com/develop/ui/compose/testing):

```kotlin
// Fuente: documentación oficial — Compose Testing
// https://developer.android.com/develop/ui/compose/testing

class TransactionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `pantalla muestra lista de transacciones`() {
        val fakeTransactions = listOf(
            Transaction(id = "1", description = "Supermercado", amount = -1500.0),
            Transaction(id = "2", description = "Sueldo",       amount =  80000.0),
        )

        composeTestRule.setContent {
            TransactionListScreen(transactions = fakeTransactions)
        }

        // Verificar que los elementos estén visibles
        composeTestRule.onNodeWithText("Supermercado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sueldo").assertIsDisplayed()
        composeTestRule.onNodeWithText("-$1.500,00").assertIsDisplayed()
    }

    @Test
    fun `click en FAB navega a pantalla de nueva transaccion`() {
        var navigated = false

        composeTestRule.setContent {
            TransactionListScreen(
                transactions = emptyList(),
                onAddClick = { navigated = true },
            )
        }

        // Buscar el FAB por content description y hacer click
        composeTestRule
            .onNodeWithContentDescription("Agregar transacción")
            .performClick()

        assertTrue(navigated)
    }

    @Test
    fun `estado vacio muestra mensaje y boton de accion`() {
        composeTestRule.setContent {
            TransactionListScreen(transactions = emptyList())
        }

        composeTestRule.onNodeWithText("No hay transacciones").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agregar primera transacción").assertIsDisplayed()
    }
}
```

---

## Referencias de los repositorios fuente

| Repositorio                    | Descripción                                       | URL                                             |
| ------------------------------ | ------------------------------------------------- | ----------------------------------------------- |
| `android/nowinandroid`         | App de referencia oficial — arquitectura completa | https://github.com/android/nowinandroid         |
| `android/architecture-samples` | Samples de patrones arquitectónicos (TODO app)    | https://github.com/android/architecture-samples |
| `android/security-samples`     | Samples de seguridad — BiometricLogin canónico    | https://github.com/android/security-samples     |
| `android/performance-samples`  | Samples de rendimiento — Macrobenchmark           | https://github.com/android/performance-samples  |
| `android/compose-samples`      | Samples oficiales de Jetpack Compose              | https://github.com/android/compose-samples      |
| `developer.android.com`        | Documentación oficial de Android                  | https://developer.android.com                   |
