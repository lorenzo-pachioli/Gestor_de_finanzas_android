# App-Wide Optimization & Architecture Implementation Plan

**Goal:** Thoroughly optimize the entire NotificationCapture application. Fix critical bugs related to category recognition and custom wallets, fix performance bottlenecks (O(N) operations in main thread), eliminate sequential I/O blocking from repositories by adding memory caching/Room migration, optimize the notification recognition algorithm, and strictly migrate all Fragments (`Categorias`, `Perfil`) to the MVVM unidirectional flow.

**Architecture:** MVVM layer extension for all fragments. Memory caching for configuration repositories (`Category`, `Wallet`, `CreditCard`). Algorithmic optimization leveraging `HashSet` / Maps instead of nested loops.

**Tech Stack:** Java, AndroidX Lifecycle, Android SDK, EncryptedSharedPreferences, ViewPager2.

---

## Batch 0: Bugfixes Críticos (Categorías y Wallets)

### Task 0.1: Fix Category ID mismatch (Color gris en UI)
**Root Cause:** Los objetos `Transaction` nuevos asignan por defecto `other_income` u `other_outcome` como ID de categoría. Sin embargo, los usuarios que tenían instalaciones previas generaron las suyas mediante la migración con el ID `Otros_INGRESO` y `Otros_EGRESO`, provocando que el Adapter devuelva null y un color gris (LTGRAY).
**Files:** `app/src/main/java/com/notificationcapture/app/repositories/CategoryRepository.java`
**Action:** Modificar el método `getCategoryById` para que actúe con "fallbacks" seguros que mapeen la selección de IDs nuevos sobre nombres antiguos (Si `other_income` no existe, buscar una categoría que se llame `Otros` de tipo `INGRESO`).

### Task 0.2: Recognize user custom Wallets in Listener
**Root Cause:** El `NotificationListener` estaba validando solo contra la lista fija en crudo del `ConfigManager` global (`globalWallets` y `walletApps`), ignorando por completo la lista local del usuario guardada en el `WalletRepository`.
**Files:** `app/src/main/java/com/notificationcapture/app/listeners/NotificationListener.java`
**Action:** Cargar en memoria las wallets creadas y personalizadas por el usuario e iterar/buscar coincidencias (`packageName`) contra esta lista también.
---

## Batch 1: Algorithmic Optimization (Notification Listener)

### Task 1: Optimize NotificationListener.java pattern matching

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/listeners/NotificationListener.java`

**Step 1: Write the optimized algorithm in NotificationListener**

```java
package com.notificationcapture.app.listeners;

// ... imports ...
import java.util.HashSet;
import java.util.Set;

public class NotificationListener extends NotificationListenerService {
    // ... previous fields
    private Set<String> globalWalletPackagesSet;
    private Set<String> keywordSet;

    @Override
    public void onCreate() {
        super.onCreate();
        // ... load dependencies ...
        
        // Cache configs in HashSets for O(1) matching where possible
        globalWalletPackagesSet = new HashSet<>();
        for (com.notificationcapture.app.models.Wallets w : globalWallets) {
            globalWalletPackagesSet.add(w.getPackageName());
        }
        
        keywordSet = new HashSet<>();
        for (String kw : paymentKeywords) {
            keywordSet.add(kw.toLowerCase());
        }
    }

    boolean isPaymentRelatedNotification(String packageName, String title, String text) {
        // 1. O(1) Check for exact package match
        if (globalWalletPackagesSet.contains(packageName)) {
            return true;
        }

        // 2. Contains check for fallback packages - Still O(N) but reduced N
        boolean isWalletApp = false;
        String packageNameLower = packageName.toLowerCase();
        for (String wallet : walletApps) {
            if (packageNameLower.contains(wallet)) {
                isWalletApp = true;
                break;
            }
        }

        if (!isWalletApp) {
            return false;
        }

        // 3. Keyword check - Optimized with whitespace splitting or direct Map matching
        String fullText = (title + " " + text).toLowerCase();
        for (String keyword : keywordSet) {
            if (fullText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
```

---

## Batch 2: I/O Bottlenecks & Data Memory Caching

### Task 2: Implement In-Memory caching for CategoryRepository

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/repositories/CategoryRepository.java`

**Step 1: Write caching logic to prevent disk reads on every call**

```java
// Inside CategoryRepository.java

public class CategoryRepository implements GsonAccess {
    private SharedPreferences prefs;
    private Gson gson;
    private List<Category> cachedCategories = null; // Memory Cache

    // ... Constructor ...

    public List<Category> getAllCategories() {
        if (cachedCategories != null) {
            return new ArrayList<>(cachedCategories); // Return copy
        }
        
        try {
            String json = prefs.getString(KEY_CATEGORIES, null);
            if (json == null) return new ArrayList<>();
            Type type = new TypeToken<List<Category>>() {}.getType();
            List<Category> cats = gson.fromJson(json, type);

            // Migrations ...
            
            cachedCategories = cats; // Cache it
            return new ArrayList<>(cachedCategories);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveCategories(List<Category> categories) {
        try {
            cachedCategories = new ArrayList<>(categories); // Update Memory
            prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
        } catch (Exception e) {
            AppLogger.e("CategoryRepository", "Error saving categories", e);
        }
    }
    
    // ... Implement similar caching for WalletRepository and CreditCardRepository ...
}
```

---

## Batch 3: MVVM for Analytics (CategoriasFragment)

### Task 3: Create CategoriasViewModel and refactor UI

**Files:**
- Create: `app/src/main/java/com/notificationcapture/app/viewmodels/CategoriasViewModel.java`
- Modify: `app/src/main/java/com/notificationcapture/app/fragments/CategoriasFragment.java`

**Step 1: Write the CategoriasViewModel**

```java
package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;

import java.util.List;

public class CategoriasViewModel extends AndroidViewModel {
    private final LiveData<List<Transaction>> allTransactions;

    public CategoriasViewModel(@NonNull Application application) {
        super(application);
        // Bind to LiveData directly from Room to avoid blocking UI during calculations
        allTransactions = RepositoryProvider.getInstance().getTransactionRepository().getAllTransactionsLiveData();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }
}
```

**Step 2: Update CategoriasFragment to use the ViewModel and remove sync calls**

```java
// Inside CategoriasFragment.java

private CategoriasViewModel viewModel;

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initViews(view);
    setupAdapters();
    setupPeriodSpinner();
    
    viewModel = new ViewModelProvider(this).get(CategoriasViewModel.class);
    viewModel.getAllTransactions().observe(getViewLifecycleOwner(), transactions -> {
        // Compute and update summary UI using current inputs (type, month, year)
        refreshDataWithList(transactions); 
    });
}

// Modify loadDataForPeriod and refreshData to accept the List<Transaction> instead of asking the Repository synchronously.
```

---

## Batch 4: MVVM and UI Cleanups (PerfilFragment)

### Task 4: Prevent Memory Leaks and I/O on Perfil

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/fragments/PerfilFragment.java`

**Step 1: Abstract synchronous setup logic**

```java
// Remove synchronous Load calls directly from onViewCreated.
// Encrypted Preferences should be injected natively via ViewModel or Repository, never straight SharedPrefs.

// Enforce modern switch behavior
private void loadCategories() {
    // With Batch 2 implemented, this call is now returning from O(1) Memory Cache 
    // instead of an O(N) SharedPreferences File I/O block!
    currentCategories = categoryRepository.getCategories(type);
    // ... adapter logic remains but is now lighting fast 
}
```
