package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;

import java.util.List;

public class CategoriasViewModel extends AndroidViewModel {
    private final LiveData<List<Transaction>> allTransactions;

    public CategoriasViewModel(@NonNull Application application) {
        super(application);
        // Bind directly to LiveData from Room (approved transactions by default)
        allTransactions = RepositoryProvider.getInstance().getTransactionRepository().getAllTransactionsLiveData();
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        return allTransactions;
    }
}
