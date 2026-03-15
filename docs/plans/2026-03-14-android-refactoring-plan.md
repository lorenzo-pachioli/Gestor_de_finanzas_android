# Android Architecture and UI Refactoring Implementation Plan

**Goal:** Refactor the existing application to fully adhere to modern Android Java Best Practices (MVVM, Material 3, Unidirectional Data Flow, Security, and Main-Thread performance safety).

**Architecture:** Transition from direct Repository access via `submit().get()` (which blocks the Main Thread) and `BroadcastReceivers` for updates to a robust MVVM pattern using `ViewModel` and `LiveData` from Room. Replace standard SharedPreferences with `EncryptedSharedPreferences`.

**Tech Stack:** Java, Android SDK, AndroidX lifecycle (ViewModel, LiveData), Room Database, Material Components 3, EncryptedSharedPreferences.

---

## Batch 1: Data Layer and Architecture Setup (MVVM Foundation)

### Task 1: Refactor TransactionDao to use LiveData

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/database/TransactionDao.java`
- Modify: `app/build.gradle.kts` (Ensure dependencies)

**Step 1: Write the changes in Dao to emit LiveData instead of synchronous lists**

```java
package com.notificationcapture.app.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.notificationcapture.app.enums.IngresoOEgreso;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TransactionEntity transaction);

    // ... other methods ...

    @Query("SELECT * FROM transactions WHERE status = 'APPROVED' AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getByMonthLiveData(long start, long end);

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getAllByStatusLiveData(String status);
}
```

---

### Task 2: Refactor TransactionRepository to expose LiveData

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/repositories/TransactionRepository.java`

**Step 1: Expose LiveData without blocking the Main Thread**

```java
// Inside TransactionRepository.java
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

public LiveData<List<Transaction>> getAllTransactionsLiveData() {
    return Transformations.map(dao.getAllByStatusLiveData(TransactionEntity.STATUS_APPROVED), 
        TransactionMapper::fromEntityList);
}

// Remove getByStatusBlocking and any executor.submit(..).get() code that blocked the main thread.
```

---

### Task 3: Create MainViewModel

**Files:**
- Create: `app/src/main/java/com/notificationcapture/app/viewmodels/MainViewModel.java`

**Step 1: Implement the ViewModel**

```java
package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> allApprovedTransactions;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        allApprovedTransactions = repository.getAllTransactionsLiveData();
    }

    public LiveData<List<Transaction>> getAllApprovedTransactions() {
        return allApprovedTransactions;
    }

    public void deleteTransaction(String id) {
        repository.deleteTransaction(id);
    }
}
```

---

## Batch 2: UI Migration (Fragments) -> Unidirectional Data Flow

### Task 4: Refactor InicioFragment to observe ViewModel

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/fragments/InicioFragment.java`

**Step 1: Remove BroadcastReceiver and query directly using ViewModel**

```java
// In InicioFragment.java
import androidx.lifecycle.ViewModelProvider;
import com.notificationcapture.app.viewmodels.MainViewModel;

public class InicioFragment extends Fragment {
    private MainViewModel viewModel;
    // Remove BroadcastReceiver and repository from class fields.

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // ... set adapter ...

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Observe LiveData
        viewModel.getAllApprovedTransactions().observe(getViewLifecycleOwner(), allNotifications -> {
            List<Transaction> currentMonthNotifications = getCurrentMonthNotifications(allNotifications);
            adapter.updateData(currentMonthNotifications);
            
            if (currentMonthNotifications.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            updateFinancialSummary(currentMonthNotifications);
        });
        
        // Remove old direct loadNotifications calls and intent filters.
    }
}
```

---

## Batch 3: Security & Data Integrity

### Task 5: Implement EncryptedSharedPreferences

**Files:**
- Modify: `app/src/main/java/com/notificationcapture/app/repositories/TransactionRepository.java`

**Step 1: Change SharedPreferences initialization to use EncryptedSharedPreferences**

```java
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public TransactionRepository(Context context) {
    this.context = context.getApplicationContext();
    
    try {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        this.prefs = EncryptedSharedPreferences.create(
                context,
                "encrypted_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    } catch (Exception e) {
        throw new RuntimeException("Could not create EncryptedSharedPreferences", e);
    }
    
    this.dao = AppDatabase.getDatabase(context).transactionDao();
    // ...
}
```

---

## Batch 4: Material 3 Design Improvements

### Task 6: Apply Material Design 3 and Touch Targets

**Files:**
- Modify: `app/src/main/res/values/themes.xml` (Ensure `Theme.Material3.DayNight.NoActionBar` is used)
- Modify: Layout files (`fragment_inicio.xml`, etc.)

**Step 1: Check Component Types**
- Ensure `<Button>` declarations are using `<com.google.android.material.button.MaterialButton>` under the hood (Material Components theme handles this, but verify styles).
- Ensure any `minWidth` and `minHeight` for touchable icons/buttons are `48dp` for accessibility.
- Update `contentDescription` for all `ImageViews` or icons used in the layout for screen readers.
