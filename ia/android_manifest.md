**MANIFIESTO DE DESARROLLO**

**ANDROID**

Diseño · Arquitectura · Calidad · Seguridad · Rendimiento

Guía completa basada en las directrices oficiales de

**developer.android.com**

Versión 1.0 · Marzo 2026

**ÍNDICE DE CONTENIDOS**

**1. DISEÑO DE INTERFAZ DE USUARIO** 3

1.1 Material Design 3 & Principios Visuales 3

1.2 Componentes y Patrones de UI 3

1.3 Tipografía, Color e Iconografía 4

1.4 UI Adaptativa y Múltiples Tamaños de Pantalla 4

**2. ARQUITECTURA DE LA APLICACIÓN** 5

2.1 Principios de Arquitectura Limpia 5

2.2 Capas de UI, Dominio y Datos 5

2.3 Patrones MVVM / MVI 6

2.4 Inyección de Dependencias 6

2.5 Navegación y Modularización 6

**3. CALIDAD DE LA APLICACIÓN** 7

3.1 Calidad Core y User Experience 7

3.2 Accesibilidad 7

3.3 Calidad Técnica 8

**4. SEGURIDAD Y PRIVACIDAD** 9

4.1 Seguridad de Datos y Red 9

4.2 Permisos y Principio de Mínimo Privilegio 9

4.3 Identidad y Autenticación 10

4.4 Prevención de Fraude 10

**5. RENDIMIENTO** 11

5.1 Arranque de la App (App Startup) 11

5.2 Rendering y Jank 11

5.3 Memoria y Fugas (Memory Leaks) 12

5.4 Batería y Trabajo en Segundo Plano 12

5.5 Red y Almacenamiento 13

**6. TESTING** 13

**7. PUBLICACIÓN EN GOOGLE PLAY** 14

**8. CHECKLIST DE CERTIFICACIÓN** 15

---

**SECCIÓN 1 --- DISEÑO DE INTERFAZ DE USUARIO**

---

**1. Diseño de Interfaz de Usuario**

El diseño de apps Android debe seguir Material Design 3 (M3), el sistema
de diseño actualizado de Google que adapta la interfaz a cada
dispositivo y usuario mediante Dynamic Color y componentes modernos. El
objetivo es crear experiencias que sean estéticamente atractivas,
funcionales y coherentes en todo el ecosistema Android.

**1.1 Material Design 3 --- Principios Visuales**

- Usar el sistema de color dinámico (Dynamic Color) basado en el
  wallpaper del usuario cuando esté disponible.

- Respetar los roles de color semánticos: Primary, Secondary,
  Tertiary, Error, Surface, Background, Outline.

- Aplicar elevación mediante tonos de color (color elevation tones) en
  lugar de sombras pesadas.

- Mantener contraste mínimo WCAG AA: 4.5:1 para texto normal, 3:1 para
  texto grande y UI components.

- Usar formas redondeadas (shape scale) de manera consistente:
  ExtraSmall, Small, Medium, Large, ExtraLarge, Full.

- Aplicar el sistema de movimiento de M3: énfasis, entrada, salida y
  transición con curvas de interpolación estándar.

**1.2 Componentes y Patrones de UI**

---

**Componente** **Uso Recomendado**

TopAppBar / Encabezado de pantalla, acciones principales y
CenterAlignedTopAppBar navegación superior

NavigationBar (Bottom) Navegación entre 3-5 destinos principales de
nivel top

NavigationDrawer Navegación con más de 5 destinos o en
tablets/desktops

FloatingActionButton (FAB) Acción primaria de la pantalla, siempre
visible

Card Contenedor de contenido relacionado,
(Elevated/Filled/Outlined) clickeable o no

Scaffold Estructura base de cada pantalla: TopBar +
Content + FAB + Snackbar

Dialog / AlertDialog Confirmaciones y entradas de datos breves

SnackBar Feedback no crítico, con acción opcional (ej.
Deshacer)

Chips (Filter/Assist/Input) Filtros de categorías, acciones secundarias

BottomSheet Contenido complementario desde la parte
(Modal/Persistent) inferior

---

**1.3 Tipografía, Color e Iconografía**

- Usar la Type Scale de M3: Display, Headline, Title, Body, Label en
  variantes Large/Medium/Small.

- Fuente por defecto: Roboto (incluida en Android). Fuente
  personalizada opcional con Google Fonts o assets locales.

- No usar menos de 12sp para texto visible; preferir 14sp-16sp para
  cuerpo de texto.

- Iconos: usar Material Symbols (Sharp/Rounded/Outlined) en formato
  VectorDrawable o ImageVector en Compose.

- Nunca mezclar familias de íconos distintas dentro de la misma app
  (coherencia visual).

- Soportar modo oscuro (Dark Theme): definir colores en ambos temas
  light y dark.

**1.4 UI Adaptativa y Múltiples Tamaños de Pantalla**

Android corre en teléfonos, tablets, foldables, ChromeOS y más. Las apps
deben responder a todos los tamaños usando Window Size Classes.

- Compact (\< 600dp): layout de columna única, NavigationBar inferior.

- Medium (600-840dp): layout de dos paneles cuando el contenido lo
  permita, NavigationRail lateral.

- Expanded (\> 840dp): layout multi-panel, NavigationDrawer siempre
  visible.

- Usar WindowSizeClass de Jetpack para detectar el breakpoint y
  adaptar el layout en tiempo real.

- Testear en emuladores de teléfono (360dp), tablet (600dp, 840dp) y
  foldable (384dp doblado / 884dp desplegado).

- Soportar rotación de pantalla sin pérdida de estado
  (rememberSaveable / ViewModel).

- Manejar el modo multi-ventana (split-screen) correctamente: la app
  debe ser funcional con un ancho mínimo de 220dp.

---

**SECCIÓN 2 --- ARQUITECTURA DE LA APLICACIÓN**

---

**2. Arquitectura de la Aplicación**

Google recomienda una arquitectura en capas bien definidas que separa la
lógica de presentación, la lógica de negocio y el acceso a datos. Esto
mejora la testeabilidad, el mantenimiento y la escalabilidad.

**2.1 Principios Fundamentales de Arquitectura**

- Separación de responsabilidades (Separation of Concerns): cada clase
  hace una sola cosa.

- Unidireccionalidad del estado (UDF --- Unidirectional Data Flow): el
  estado fluye hacia abajo, los eventos hacia arriba.

- Single source of truth: cada dato tiene una única fuente de verdad
  (preferentemente el Repository).

- Diseño orientado a interfaces: depender de abstracciones, no de
  implementaciones concretas.

- Testeabilidad: diseñar clases que puedan ser probadas de forma
  aislada con fakes/mocks.

**2.2 Las Tres Capas Recomendadas**

---

**Capa** **Responsabilidad**

UI Layer Mostrar estado de la app, capturar eventos del
usuario. Contiene: Composables/Fragments,
ViewModels.

Domain Layer (opcional) Lógica de negocio reutilizable y compleja.
Contiene: Use Cases / Interactors.

Data Layer Acceso y exposición de datos. Contiene:
Repositories, Data Sources (local/remoto),
Models.

---

**2.3 Patrón MVVM / MVI con Jetpack**

- ViewModel: sobrevive a cambios de configuración. Expone estado como
  StateFlow o LiveData. Nunca tiene referencias al Context de UI.

- UI State: modelar el estado como data class inmutable (UiState). Un
  solo flujo de estado por pantalla.

- Eventos UI: modelar como sealed class (UiEvent). Procesar eventos en
  el ViewModel, no en la UI.

- Efectos secundarios (Side Effects): emitir como Channel\<Effect\>
  que la UI consume una sola vez.

- Compose: usar collectAsStateWithLifecycle() para observar flows de
  manera lifecycle-aware.

**2.4 Inyección de Dependencias con Hilt**

- Usar Hilt (basado en Dagger) como framework de DI recomendado por
  Google.

- Anotar con \@HiltViewModel los ViewModels que reciben dependencias.

- Definir Modules con \@Module + \@InstallIn para proveer
  implementaciones de repositorios y data sources.

- Evitar singletons globales manuales (object en Kotlin); preferir el
  scope correcto: \@Singleton, \@ActivityScoped, \@ViewModelScoped.

**2.5 Navegación y Modularización**

- Navigation Component (Compose Navigation): grafo de navegación
  centralizado, deep links, back stack gestionado automáticamente.

- Pasar solo tipos primitivos o IDs entre destinos, nunca objetos
  complejos completos (usar el ViewModel compartido o el Repository).

- Modularización: dividir la app en módulos Gradle (:app, :core,
  :feature:\*) para compilación incremental y separación de dominios.

- Módulos de feature: independientes, con sus propias pantallas,
  ViewModels y navegación interna.

- Módulos de core: utilidades compartidas (core:network,
  core:database, core:ui, core:testing).

---

**SECCIÓN 3 --- CALIDAD DE LA APLICACIÓN**

---

**3. Calidad de la Aplicación**

La calidad de una app Android se mide en tres dimensiones: valor core
(hace lo que promete), experiencia de usuario (placentera y coherente) y
calidad técnica (estable, rápida y accesible).

**3.1 Valor Core y Experiencia de Usuario**

- La app debe funcionar offline o degradarse con gracia cuando no hay
  conexión (mostrar datos en caché, mensajes claros).

- Tiempos de respuesta: \< 100ms para feedback visual inmediato, \< 1s
  para acciones del usuario, \< 10s para operaciones largas con
  indicador de progreso.

- No mostrar pantallas en blanco: siempre mostrar un loading skeleton,
  shimmer o placeholder.

- Manejar el estado vacío (empty state) con ilustración + mensaje +
  acción sugerida.

- Manejar errores con mensajes claros en lenguaje no técnico. Ofrecer
  acción de reintento.

- No interrumpir el flujo del usuario con diálogos innecesarios o
  notificaciones excesivas.

- Animaciones fluidas (60fps mínimo, 120fps en dispositivos
  compatibles). Nunca bloquear el hilo principal.

**3.2 Accesibilidad (A11y)**

Android exige que las apps sean accesibles. TalkBack (lector de
pantalla) y Switch Access son los servicios principales a soportar.

- Todo elemento interactivo debe tener un contentDescription
  significativo (no redundante con el texto visible).

- Touch targets mínimos de 48x48dp para cualquier elemento clickeable.

- Implementar semántica correcta en Compose: Modifier.semantics { },
  roles (Role.Button, Role.Checkbox), mergeDescendants.

- Soportar tamaño de fuente grande: testear con 200% de escala de
  fuente. No usar sp fijos para contenedores.

- Soportar alto contraste: nunca transmitir información solo con
  color.

- Orden de enfoque lógico (traversalIndex en Compose para ordenar el
  foco de TalkBack).

- Testear con TalkBack activado antes de cada release. Usar
  Accessibility Scanner de Google.

- Cumplir WCAG 2.1 nivel AA como mínimo.

**3.3 Calidad Técnica**

- Crash-free rate \> 99.5% medido en Android Vitals (Play Console).

- ANR (Application Not Responding) rate \< 0.47% para aparecer en Play
  Store sin restricciones.

- Crash rate \< 1.09% según umbrales de Android Vitals.

- Target API Level: siempre compilar y targetear el API Level
  requerido por Play Store (actualmente API 35 para nuevas apps).

- Min SDK: definir el SDK mínimo según el mercado objetivo. La mayoría
  de apps usan minSdk 24 (Android 7.0) para cubrir \>95% de
  dispositivos.

- Habilitar R8/ProGuard en release para ofuscación y reducción de APK.

- Usar Android App Bundle (.aab) para distribución: permite delivery
  optimizado por dispositivo.

- Testear en dispositivos reales de gama baja (2GB RAM, procesador
  antiguo) además de emuladores.

---

**SECCIÓN 4 --- SEGURIDAD Y PRIVACIDAD**

---

**4. Seguridad y Privacidad**

La seguridad no es opcional: una app insegura puede comprometer datos
financieros y personales de los usuarios. Android provee un sandbox de
seguridad por app; el desarrollador debe reforzarlo con buenas
prácticas.

**4.1 Seguridad de Datos y Comunicaciones**

- Toda comunicación de red debe usar HTTPS/TLS. Nunca usar HTTP para
  datos sensibles.

- Implementar Certificate Pinning para APIs críticas (por ejemplo, el
  backend de login y transacciones financieras).

- Network Security Config (res/xml/network_security_config.xml):
  deshabilitar cleartext traffic en producción.

- Datos sensibles en dispositivo: usar EncryptedSharedPreferences o
  EncryptedFile de Jetpack Security.

- Base de datos local cifrada: usar SQLCipher o Room con
  EncryptedDatabase si se almacenan datos financieros.

- No almacenar contraseñas, tokens o claves de API en texto plano en
  SharedPreferences, bases de datos o logs.

- Nunca loguear datos sensibles (PII, tokens, montos) en producción.
  Usar BuildConfig.DEBUG para logs de desarrollo.

- Usar Android Keystore para almacenar claves criptográficas: las
  claves nunca salen del hardware seguro.

**4.2 Permisos y Principio de Mínimo Privilegio**

- Solicitar solo los permisos estrictamente necesarios para la
  funcionalidad de la app.

- Usar permisos en el momento en que se necesitan (just-in-time), no
  al arrancar la app.

- Explicar el por qué antes de solicitar el permiso (permission
  rationale dialog).

- Manejar el caso de permiso denegado con degradación gracia (la app
  sigue funcionando parcialmente).

- Permisos peligrosos (CAMERA, LOCATION, CONTACTS, etc.): requerir
  aprobación explícita del usuario en runtime.

- Permisos de localización: preferir ACCESS_COARSE_LOCATION sobre
  ACCESS_FINE_LOCATION si la precisión exacta no es necesaria.

- Para apps de finanzas personales: no se necesita localización,
  cámara (salvo escáner de recibos) ni contactos.

**4.3 Identidad y Autenticación**

- Usar Credential Manager API (Jetpack) para login con passkeys,
  contraseñas y federated identity (Google Sign-In).

- Implementar biometría (huella/rostro) con BiometricPrompt de Jetpack
  para operaciones sensibles.

- Tokens de sesión: usar OAuth 2.0 + PKCE para flujos de autenticación
  con backend.

- Refresh tokens: almacenar con EncryptedSharedPreferences o en el
  Keystore, con TTL corto.

- Implementar logout total que invalide el token en el servidor y
  limpie el storage local.

- Account Manager: considerar el uso de AccountManager para cuentas de
  sistema si la app tiene múltiples usuarios.

**4.4 Prevención de Fraude e Integridad**

- Integrar Play Integrity API para verificar que el APK no fue
  manipulado y corre en un dispositivo legítimo.

- Detectar dispositivos rooteados con herramientas como RootBeer y
  degradar funcionalidad crítica si corresponde.

- Implementar anti-tampering: ofuscación con R8/ProGuard + detección
  de debugger adjunto en producción.

- Rate limiting en el lado del servidor para operaciones críticas
  (login, transferencias).

- No confiar en validaciones solo del lado cliente; siempre validar en
  el servidor.

---

**SECCIÓN 5 --- RENDIMIENTO**

---

**5. Rendimiento**

El rendimiento es un pilar de calidad en Android. Una app lenta o que
drena la batería será desinstalada. Android Vitals en Play Console
monitorea métricas clave en dispositivos reales.

**5.1 Arranque de la App (App Startup)**

- Cold Start target: \< 500ms en dispositivos de gama media.

- Warm Start target: \< 200ms.

- Usar App Startup Library de Jetpack para inicializar componentes de
  forma lazy y controlada.

- Mover operaciones pesadas de inicio a coroutines/WorkManager: no
  bloquear Application.onCreate().

- Baseline Profiles: generar y distribuir un perfil de código
  precompilado que reduce el tiempo de JIT compilation en el primer
  arranque.

- Splash Screen API (Android 12+): usar SplashScreen de Jetpack para
  una pantalla de inicio consistente y native.

- Medir con Android Studio Profiler (CPU Trace) y reportar en Android
  Vitals: Start time.

**5.2 Rendering y Jank**

- Target: 60fps estables (16.6ms por frame). En dispositivos 90/120Hz:
  90/120fps cuando sea posible.

- Regla de oro: nunca hacer trabajo pesado en el Main Thread / UI
  Thread.

- Usar coroutines (Dispatchers.IO para I/O, Dispatchers.Default para
  CPU) y StateFlow para comunicar resultados a la UI.

- Evitar recomposiciones innecesarias en Compose: usar keys estables,
  remember, derivedStateOf.

- Lazy layouts (LazyColumn/LazyRow): no crear items fuera de pantalla.
  Usar key() para identificar items y evitar remapping.

- Imágenes: usar Coil o Glide para carga asíncrona, decodificación en
  background y caché. Nunca decodificar bitmaps grandes en el Main
  Thread.

- Medir con Android Studio Profiler (Frame rendering) y Macrobenchmark
  para comparar versiones.

**5.3 Memoria y Fugas (Memory Leaks)**

- Evitar memory leaks: no guardar referencias a Context
  (Activity/Fragment) en singletons o ViewModels.

- Usar LeakCanary en debug builds para detectar automáticamente leaks
  durante desarrollo.

- Bitmaps: reciclar o usar formatos comprimidos (WebP en lugar de PNG
  cuando sea posible).

- Evitar allocations innecesarias en rutas de código de alta
  frecuencia (onDraw, composición frecuente).

- Target heap size: mantener uso de memoria \< 80% del heap disponible
  bajo carga normal.

- Medir con Android Studio Memory Profiler: heap snapshots, allocation
  tracking.

**5.4 Batería y Trabajo en Segundo Plano**

- Toda tarea en background debe usar WorkManager (el único gestor de
  background work recomendado por Google para trabajo diferible y
  garantizado).

- Definir constraints en WorkManager: NetworkType.CONNECTED,
  requiresCharging() para tareas pesadas.

- Evitar wakelocks manuales: WorkManager los gestiona automáticamente.

- Respetar Doze Mode y App Standby: las apps en background son
  restringidas por Android; no intentar circunvalar estas
  restricciones.

- Notificaciones push: usar FCM (Firebase Cloud Messaging) en lugar de
  polling permanente.

- GPS/Location: usar batched updates y geofencing en lugar de updates
  continuos cuando sea posible.

- Medir con Android Studio Energy Profiler y testear con Battery
  Historian.

**5.5 Red y Almacenamiento**

- Cachear respuestas de red con OkHttp Cache o Room como cache local.

- Implementar estrategia offline-first: leer de la DB local primero,
  sincronizar en background.

- Paginar datos remotos con Paging 3 Library: no cargar toda la base
  de datos de una vez.

- Comprimir payloads: usar Gzip/Brotli en las respuestas HTTP
  (configurar en el servidor).

- DataStore (Proto o Preferences) en lugar de SharedPreferences para
  settings y datos simples.

- Room para datos estructurados: definir migrations correctamente para
  evitar pérdida de datos en updates.

---

**SECCIÓN 6 --- TESTING**

---

**6. Testing**

Una estrategia de testing robusta es indispensable para mantener la
calidad en el tiempo. Android recomienda la pirámide de tests: muchos
unit tests, algunos integration tests, pocos E2E tests.

---

**Tipo de Test** **Herramientas y Cobertura**

Unit Tests JUnit 5, MockK, Turbine (flows). Cubrir
ViewModels, Use Cases, Repositories, Utils.

Integration Tests Hilt Testing, Room in-memory DB,
MockWebServer. Cubrir flujos de datos
completos.

UI Tests (Compose) Compose Testing API (composeTestRule). Testear
estados de UI, interacciones básicas.

E2E Tests Espresso + UiAutomator o Maestro. Flows
críticos: login, carga de transacciones,
balance.

Performance Tests Macrobenchmark (startup, scrolling).
Microbenchmark (funciones críticas aisladas).

Screenshot Tests Paparazzi o Roborazzi. Detección de
regresiones visuales en componentes UI.

---

- Cobertura mínima recomendada: \> 70% en lógica de negocio y capa de
  datos.

- Usar fake implementations en lugar de mocks cuando sea posible para
  tests más robustos.

- CI/CD: correr unit tests y lint en cada PR. UI tests en nightly
  builds o pre-release.

- Firebase Test Lab: testear en dispositivos físicos reales en la nube
  antes de cada release a producción.

- Espresso idling resources: registrar recursos asíncronos para evitar
  flaky tests.

---

**SECCIÓN 7 --- PUBLICACIÓN EN GOOGLE PLAY**

---

**7. Publicación en Google Play**

**7.1 Android App Bundle**

- Publicar siempre como .aab (Android App Bundle), no como .apk
  directo.

- El Bundle permite Google Play optimizar la descarga: solo el código
  y recursos necesarios para el dispositivo.

- Reducción de tamaño típica: 15-35% respecto al APK universal.

- Habilitar Play Asset Delivery para recursos grandes opcionales (ej.
  packs de datos offline).

**7.2 Firma y Seguridad del Build**

- Usar Play App Signing: Google gestiona la clave de producción en sus
  servidores seguros.

- Configurar signing config en build.gradle con variables de entorno o
  keystore externo (nunca hardcodear en el repo).

- Habilitar R8 full mode en builds de release para máxima ofuscación y
  reducción.

**7.3 Políticas de Play Store**

- Cumplir todas las políticas de Google Play: privacidad de datos,
  contenido, permisos, anuncios.

- Completar la Data Safety Section en Play Console: declarar todos los
  datos recopilados, su propósito y si se comparten.

- Para apps financieras: cumplir los requisitos de la categoría
  Finance (declaraciones adicionales de privacidad y seguridad).

- Target API mínimo exigido por Google Play: actualizar cada año según
  los requisitos de la fecha de publicación.

- Usar staged rollouts (1% → 10% → 50% → 100%) para mitigar impacto de
  bugs en producción.

**7.4 Monitoreo Post-Launch**

- Android Vitals en Play Console: monitorear crash rate, ANR rate,
  startup time, excessive wakeups.

- Integrar Firebase Crashlytics para crash reporting detallado con
  stack traces.

- Integrar Firebase Performance Monitoring para métricas de red,
  traces y screen rendering.

- Configurar alertas en Play Console para métricas por debajo de los
  umbrales de calidad.

- Analizar reviews de usuarios regularmente para detectar issues de UX
  no capturados por métricas.

---

**SECCIÓN 8 --- CHECKLIST DE CERTIFICACIÓN**

---

**8. Checklist de Certificación**

Verificar todos los ítems antes de publicar una versión en producción.

**8.1 Diseño**

---

**DISEÑO --- Items a verificar antes del release**

✓ Material Design 3 implementado consistentemente en toda la app

✓ Dark mode funcional y sin elementos con contraste insuficiente

✓ UI testada en phones (360dp), tablets (600dp, 840dp) y foldables

✓ Todos los touch targets tienen mínimo 48x48dp

✓ Splash Screen implementada con SplashScreen API (Android 12+)

✓ Animaciones fluidas, sin jank visible

✓ Estados vacío, cargando y error implementados en todas las pantallas

---

**8.2 Arquitectura y Código**

---

**ARQUITECTURA --- Items a verificar antes del release**

✓ MVVM con ViewModel + StateFlow en todas las pantallas

✓ Ninguna referencia a Context/Activity en ViewModels o Repositories

✓ Hilt configurado correctamente con scopes apropiados

✓ Navigation Component con back stack correcto en todos los flujos

✓ Toda operación de red/disco en Dispatchers.IO, no en Main Thread

✓ WorkManager para todas las tareas en background

✓ Room con migrations definidas para todos los cambios de esquema

---

**8.3 Calidad y Accesibilidad**

---

**CALIDAD --- Items a verificar antes del release**

✓ Crash-free rate \> 99.5% en versiones de prueba

✓ TalkBack: navegar toda la app con lector de pantalla sin bloquearse

✓ contentDescription en todos los elementos interactivos sin texto
visible

✓ Escala de fuente 200% no rompe ningún layout

✓ App funcional sin conexión o con mensaje claro de error de red

✓ No hay pantallas en blanco durante la carga

✓ Unit tests y UI tests pasan en CI sin fallos

---

**8.4 Seguridad**

---

**SEGURIDAD --- Items a verificar antes del release**

✓ HTTPS en todas las llamadas de red. cleartext deshabilitado en
producción

✓ Datos sensibles cifrados (EncryptedSharedPreferences / Room cifrado)

✓ No hay logs con datos sensibles en builds de producción

✓ Permisos solicitados justificados y con rationale al usuario

✓ Play Integrity API integrada para verificar integridad del APK

✓ BiometricPrompt implementado para acciones sensibles

✓ Tokens de sesión con TTL y revocación en logout

---

**8.5 Rendimiento**

---

**RENDIMIENTO --- Items a verificar antes del release**

✓ Cold start \< 500ms en dispositivos de gama media (medido con
Profiler)

✓ Sin frames dropped \> 16ms en scrolling de listas (LazyColumn)

✓ Sin memory leaks detectados por LeakCanary

✓ Baseline Profile generado y empaquetado en el release AAB

✓ R8/ProGuard habilitado en release con reglas keep correctas

✓ App Bundle publicado (no APK universal)

✓ Métricas de Android Vitals en verde antes de aumentar el rollout

---

**Referencias Oficiales**

---

**Recurso** **URL**

Design & Plan Overview developer.android.com/design

Material Design 3 for developer.android.com/design/ui/mobile
Android

App Architecture Guide developer.android.com/topic/architecture/intro

App Quality Overview developer.android.com/quality

Technical Quality developer.android.com/quality/technical

Security Overview developer.android.com/security

Privacy Guide developer.android.com/privacy

Performance Overview developer.android.com/topic/performance/overview

Accessibility Guide developer.android.com/guide/topics/ui/accessibility

WorkManager Guide developer.android.com/develop/background-work

Modularization Guide developer.android.com/topic/modularization

Navigation Component developer.android.com/guide/navigation/navigation-principles

Play Store Policies developer.android.com/distribute/play-policies

Android Vitals developer.android.com/topic/performance/app-score

---

_--- Fin del Manifiesto ---_
