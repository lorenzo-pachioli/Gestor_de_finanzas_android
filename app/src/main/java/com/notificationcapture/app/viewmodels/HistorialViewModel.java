package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;

import java.util.List;

public class HistorialViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> pendingTransactions;

    public HistorialViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        pendingTransactions = repository.getAllPendingAndUnrecognizedLiveData();
    }

    public LiveData<List<Transaction>> getPendingTransactions() {
        return pendingTransactions;
    }

    public void deleteTransaction(String id) {
        repository.deleteTransactionNotFiltered(id);
    }

    public void approveUnrecognized(String id) {
        repository.moveTransactionToApproved(id);
    }

    public void moveTransactionToApproved(String id) {
        repository.moveTransactionToApproved(id);
    }

    public void clearAllTransactions() {
        repository.clearAllTransactionNotFiltered();
    }
}
