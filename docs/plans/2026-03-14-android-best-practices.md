# Mejora Continua Android: Implementación de la Skill @android-java-best-practices

**Goal:** Transformar la aplicación NotificationCapture para que cumpla rigurosamente con los estándares oficiales de Android (MVVM, Seguridad con SharedPreferences Encriptadas, y App Startup), todo manteniendo Java puro.

**Architecture:** Mover el manejo del estado a la capa de presentacion usando `ViewModel` y `LiveData`. Migrar almacenamiento de configuración (modo oscuro/tema) a `EncryptedSharedPreferences`. Usar `App Startup` de Jetpack para la inicialización y evitar el bloqueo de `onCreate` en el Main Thread.

**Tech Stack:** Java 11, Jetpack Lifecycle (ViewModel, LiveData), Jetpack Security (security-crypto), Jetpack App Startup, JUnit + Robolectric.

---

## Estrategia Orientada a "Batches" (Lotes de Trabajo)
Para asegurar que el progreso pueda pausarse y reanudarse sin dejar el proyecto incompilable, la implementación está dividida estrictamente en tareas de 2-5 minutos que encapsulan flujos terminados. 

### BATCH 1: Configuración de Dependencias e Inserción Base (MVVM)

#### Task 1.1: Agregar dependencias fundamentales a Gradle
**Files:**
- Modify: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/build.gradle.kts`

**Step 1:** Agregar las dependencias necesarias de Lifecycle, Security y App Startup.

```kotlin
// Dentro del bloque dependencies {
    // ViewModel y LiveData (MVVM)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    
    // Security Crypto para EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // App Startup para inicialización escalable
    implementation("androidx.startup:startup-runtime:1.1.1")
```

**Step 2:** Sincronizar Gradle construyendo el build manual.
Run: `.\gradlew build --dry-run`
Expected output: SUCCESS (Gradle sincroniza sin conflictos de versiones)


#### Task 1.2: Crear el SettingsViewModel (Manejo Global del Estado)
**Files:**
- Create: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/viewmodels/SettingsViewModel.java`
- Create: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/test/java/com/notificationcapture/app/viewmodels/SettingsViewModelTest.java`

**Step 1: Write the failing test**
```java
// Archivo: SettingsViewModelTest.java
package com.notificationcapture.app.viewmodels;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SettingsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void testLanguageStateUpdate() {
        SettingsViewModel viewModel = new SettingsViewModel();
        viewModel.setLanguage("en");
        assertEquals("en", viewModel.getLanguageState().getValue());
    }
}
```

**Step 2: Run test to verify it fails**
Run: `.\gradlew testDebugUnitTest --tests "com.notificationcapture.app.viewmodels.SettingsViewModelTest"`
Expected output: FAIL ("SettingsViewModel no está definido")

**Step 3: Write minimal implementation**
```java
// Archivo: SettingsViewModel.java
package com.notificationcapture.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsViewModel extends ViewModel {
    private final MutableLiveData<String> languageState = new MutableLiveData<>();
    private final MutableLiveData<Integer> nightModeState = new MutableLiveData<>(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    public LiveData<String> getLanguageState() {
        return languageState;
    }

    public void setLanguage(String langCode) {
        languageState.setValue(langCode);
    }

    public LiveData<Integer> getNightModeState() {
        return nightModeState;
    }

    public void setNightMode(int mode) {
        nightModeState.setValue(mode);
    }
}
```

**Step 4: Run test to verify it passes**
Run: `.\gradlew testDebugUnitTest --tests "com.notificationcapture.app.viewmodels.SettingsViewModelTest"`
Expected output: PASS

**Step 5: Commit the changes**
`git add ...` 
`git commit -m "feat: Add MVVM dependencies and SettingsViewModel"`

---

### BATCH 2: Migración de MainActivity a MVVM

#### Task 2.1: Suscribir MainActivity al SettingsViewModel
**Files:**
- Modify: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/MainActivity.java`

**Step 1: Implementar ViewModelProvider y Observer**
Remover la lógica pesada de lectura de SharedPreferences del `onCreate` y reemplazarla por el ViewModel.

```java
// En onCreate de MainActivity.java
import androidx.lifecycle.ViewModelProvider;
import com.notificationcapture.app.viewmodels.SettingsViewModel;

// Declarar
private SettingsViewModel settingsViewModel;

// En onCreate() borrar el código de getSharedPreferences y agregar:
settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

settingsViewModel.getNightModeState().observe(this, mode -> {
    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
});

settingsViewModel.getLanguageState().observe(this, lang -> {
    setLocale(lang);
});
```

**Step 2: Run build to verify code is valid**
Run: `.\gradlew assembleDebug`
Expected output: BUILD SUCCESSFUL

**Step 3: Commit the changes**
`git commit -m "refactor: Migrate MainActivity to use SettingsViewModel"`

---

### BATCH 3: Seguridad de Datos Locales

#### Task 3.1: Crear SecurityPreferencesManager utilizando EncryptedSharedPreferences
**Files:**
- Create: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/utils/SecurityPreferencesManager.java`

**Step 1: Write Class wrapper around EncryptedSharedPreferences**
```java
package com.notificationcapture.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurityPreferencesManager {
    private SharedPreferences encryptedPrefs;

    public SecurityPreferencesManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_app_settings",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void saveLanguage(String lang) {
        if(encryptedPrefs != null) encryptedPrefs.edit().putString("app_language", lang).apply();
    }

    public String getLanguage(String defaultLang) {
        return encryptedPrefs != null ? encryptedPrefs.getString("app_language", defaultLang) : defaultLang;
    }
    
    // Agregar métodos equivalentes saveNightMode y getNightMode ...
}
```

**Step 2:** Conectar `SecurityPreferencesManager` con `SettingsViewModel`. 
*Nota del planificador: Podemos injectar esto en el constructor del ViewModel usando `ViewModelProvider.Factory` en la actividad.*

**Step 3: Commit the changes**
`git commit -m "feat: Implement EncryptedSharedPreferences storage"`

---

### BATCH 4: Inicialización Óptima (App Startup)

#### Task 4.1: Crear Initialization de RepositoryProvider
Actualmente, `RepositoryProvider.initialize(this)` se llama durante el `onCreate` del MainActivity, lo cual bloquea el Main Thread.

**Files:**
- Create: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/database/AppInitializer.java`
- Modify: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/AndroidManifest.xml`

**Step 1: Crear Inicializador**
```java
package com.notificationcapture.app.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.startup.Initializer;
import com.notificationcapture.app.repositories.RepositoryProvider;
import java.util.Collections;
import java.util.List;

public class AppInitializer implements Initializer<Void> {
    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        // Mover aquí la pesada inicialización del repositorio
        RepositoryProvider.initialize(context);
        return null; // Return null required by Initializer interface for Void
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList(); // No setup dependencies
    }
}
```

**Step 2: Configurar AndroidManifest.xml**
Dentro del tag `<application>`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.notificationcapture.app.database.AppInitializer"
        android:value="androidx.startup" />
</provider>
```

**Step 3: Borrar de MainActivity.java**
Eliminar la línea `RepositoryProvider.initialize(this);`

**Step 4: Verificar Compilación**
Run: `.\gradlew assembleDebug`
Expected output: BUILD SUCCESSFUL

**Step 5: Commit the changes**
`git commit -m "perf: Optimize app startup utilizing Jetpack App Startup"`
