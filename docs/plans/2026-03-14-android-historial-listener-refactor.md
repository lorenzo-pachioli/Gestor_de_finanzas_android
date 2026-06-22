# Mejora Continua Android: MVVM en Historial y Asincronía en Servicios

**Goal:** Refactorizar el fragmento del historial para utilizar el patrón MVVM (evitando operaciones síncronas de base de datos en el Main Thread) y optimizar el servicio `NotificationListener` para que las Inserciones en BD no ralenticen el sistema Android.

**Architecture:** Mover el manejo local de datos del `HistorialFragment` a un nuevo `HistorialViewModel` utilizando `LiveData` y `ExecutorService` para I/O asíncrono. Envolver el procesamiento del `NotificationListenerService` en un bloque de ejecución en segundo plano para cumplir con los estándares de "Performance & Background Work".

**Tech Stack:** Java 11, Jetpack Lifecycle (ViewModel, LiveData), `java.util.concurrent.Executors`.

---

## Estrategia Orientada a "Batches" (Lotes de Trabajo)

### BATCH 1: Creación del HistorialViewModel y Gestión Asíncrona

#### Task 1.1: Crear clase HistorialViewModel
**Files:**
- Create: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/viewmodels/HistorialViewModel.java`

**Step 1: Write the ViewModel implementation**
Implementar el ViewModel usando un `ExecutorService` de un solo hilo para I/O, en favor de RxJava (para no sumarle peso innecesario a la app con librerías externas para consultas simples).

```java
// Archivo: HistorialViewModel.java
package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistorialViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final MutableLiveData<List<Transaction>> pendingTransactions = new MutableLiveData<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public HistorialViewModel(@NonNull Application application) {
        super(application);
        // Utilizar el proveedor inicializado por el AppInitializer
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        loadNotifications();
    }

    public LiveData<List<Transaction>> getPendingTransactions() {
        return pendingTransactions;
    }

    public void loadNotifications() {
        ioExecutor.execute(() -> {
            List<Transaction> notifications = repository.getAllTransactionNotFiltered();
            // setValue() debe llamarse desde MainThread, postValue() es para hilos de fondo.
            pendingTransactions.postValue(notifications);
        });
    }

    public void deleteTransaction(long id) {
        ioExecutor.execute(() -> {
            repository.deleteTransactionNotFiltered(id);
            loadNotifications();
        });
    }

    public void moveTransactionToApproved(long id) {
        ioExecutor.execute(() -> {
            repository.moveTransactionToApproved(id);
            loadNotifications();
        });
    }

    public void clearAllTransactions() {
        ioExecutor.execute(() -> {
            repository.clearAllTransactionNotFiltered();
            loadNotifications();
        });
    }
}
```

**Step 2: Run build to verify code is valid**
Run: `.\gradlew compileDebugJavaWithJavac`
Expected output: SUCCESS (Compilación sin errores)

**Step 3: Commit the changes**
`git add ...` 
`git commit -m "feat: Add HistorialViewModel with async data fetching"`

---

### BATCH 2: Refactorizar HistorialFragment a MVVM

#### Task 2.1: Suscribir HistorialFragment al nuevo ViewModel
**Files:**
- Modify: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/fragments/HistorialFragment.java`

**Step 1: Reemplazar el acceso directo a repositorios por el ViewModel.**
- Borrar instancias de `TransactionRepository` y `CategoryRepository`.
- Declarar `private HistorialViewModel viewModel;`
- En `onViewCreated`, inicializar el ViewModel e invocar a sus métodos.
- Observar el `LiveData`.

```java
// En onViewCreated de HistorialFragment
import androidx.lifecycle.ViewModelProvider;
import com.notificationcapture.app.viewmodels.HistorialViewModel;

// Declarar a nivel de clase: private HistorialViewModel viewModel;

// En onViewCreated:
viewModel = new ViewModelProvider(this).get(HistorialViewModel.class);

// Modificar callbacks del Adapter
adapter = new TransactionAdapter(new ArrayList<>(), getChildFragmentManager(), (item) -> {
    viewModel.deleteTransaction(item.getId());
}, (item) -> {
    viewModel.moveTransactionToApproved(item.getId());
}, true);

// Observar los datos asincrónicos
viewModel.getPendingTransactions().observe(getViewLifecycleOwner(), notifications -> {
    adapter.updateData(notifications);
    if (notifications.isEmpty()) {
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    } else {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
});

// Modificar el click del botón Clear All
btnClearAll.setOnClickListener(v -> {
    com.notificationcapture.app.utils.Dialog.show(
        "¿Estás seguro de que deseas eliminar todas las notificaciones pendientes?",
        com.notificationcapture.app.enums.DialogType.CONFIRMATION,
        () -> {
            viewModel.clearAllTransactions();
        });
});

// Borrar el método privado loadNotifications() del fragment que ya no necesitamos.
```

**Step 2: Configurar las llamadas onResume**
Reemplazar `loadNotifications()` dentro de `onResume()` por `viewModel.loadNotifications()`.

**Step 3: Run build to verify**
Run: `.\gradlew assembleDebug`
Expected output: BUILD SUCCESSFUL

**Step 4: Commit the changes**
`git commit -m "refactor: Migrate HistorialFragment to MVVM with LiveData"`

---

### BATCH 3: Optimización del NotificationListenerService (Fondo)

#### Task 3.1: Mover I/O en onNotificationPosted a un background thread.
Al ser un Android Service, sus callbacks como `onNotificationPosted` pueden ser llamados sobre el `MainThread`, afectando la experiencia gráfica y la fluidez del dispositivo si ejecutamos queries (Room Database).

**Files:**
- Modify: `c:/Users/Lorenzo/AndroidStudioProjects/NotificationCapture/app/src/main/java/com/notificationcapture/app/listeners/NotificationListener.java`

**Step 1: Crear e implementar un Executor**
```java
// Añadir como declaración global en la clase:
private final java.util.concurrent.ExecutorService ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

// Dentro de onNotificationPosted, luego de la validación:
// if (!isPaymentRelatedNotification(packageName, title, text)) { return; }

ioExecutor.execute(() -> {
    long timestamp = sbn.getPostTime();
    
    // Consulta sincrónica enviada al Hilo en Fondo
    Wallets wallet = walletsRepository.getWalletByPackageName(title != null ? title : "", packageName);
    
    Debit item = new Debit(
            title != null ? title : "Sin título",
            text,
            timestamp,
            wallet != null ? wallet.getId() : 1, // Fix temporal por seguridad contra nulos
            true);

    // Inserción sincrónica enviada al Hilo en Fondo
    repository.saveTransactionNotFiltered(item);

    // Enviar el broadcast como normalmente lo hacíamos
    Intent intent = new Intent("com.notificationcapture.NEW_NOTIFICATION");
    sendBroadcast(intent);
});
```

**Step 2: Run build to verify**
Run: `.\gradlew assembleDebug`
Expected output: BUILD SUCCESSFUL

**Step 3: Commit the changes**
`git commit -m "perf: Optimize NotificationListener DB writes with single thread executor"`
