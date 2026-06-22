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
    private final LiveData<List<Transaction>> allPendingTransactions;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        allApprovedTransactions = repository.getAllTransactionsLiveData();
        allPendingTransactions = repository.getAllTransactionNotFilteredLiveData();
    }

    public LiveData<List<Transaction>> getAllApprovedTransactions() {
        return allApprovedTransactions;
    }

    public LiveData<List<Transaction>> getAllPendingTransactions() {
        return allPendingTransactions;
    }

    public void deleteTransaction(String id) {
        repository.deleteTransaction(id);
    }

    public void getSaldosPorCuenta(com.notificationcapture.app.repositories.TransactionRepository.RepositoryCallback<java.util.List<com.notificationcapture.app.models.SaldoCuenta>> callback) {
        repository.getSaldosPorCuenta(callback);
    }
}
