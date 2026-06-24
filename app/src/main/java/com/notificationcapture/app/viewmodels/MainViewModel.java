package com.notificationcapture.app.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.notificationcapture.app.models.SaldoCuenta;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.RepositoryProvider;
import com.notificationcapture.app.repositories.TransactionRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final TransactionRepository repository;
    private final LiveData<List<Transaction>> allApprovedTransactions;
    private final LiveData<List<Transaction>> allPendingTransactions;

    // Saldos expuestos como LiveData para que los observers del Fragment
    // siempre corran en el hilo principal, sin necesidad de runOnUiThread.
    private final MutableLiveData<List<SaldoCuenta>> saldosPorCuenta = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = RepositoryProvider.getInstance().getTransactionRepository();
        allApprovedTransactions = repository.getAllTransactionsLiveData();
        allPendingTransactions = repository.getAllTransactionNotFilteredLiveData();

        // Carga inicial de saldos
        refreshSaldos();
    }

    public LiveData<List<Transaction>> getAllApprovedTransactions() {
        return allApprovedTransactions;
    }

    public LiveData<List<Transaction>> getAllPendingTransactions() {
        return allPendingTransactions;
    }

    public LiveData<List<SaldoCuenta>> getSaldosPorCuenta() {
        return saldosPorCuenta;
    }

    /**
     * Fuerza un recálculo de saldos. Llamar después de guardar o eliminar transacciones
     * cuando se necesite refrescar la vista de disponible/deuda.
     */
    public void refreshSaldos() {
        repository.getSaldosPorCuenta(result -> {
            if (result.isSuccess()) {
                saldosPorCuenta.postValue(result.getData());
            }
        });
    }

    public void deleteTransaction(String id) {
        repository.deleteTransaction(id);
        refreshSaldos();
    }
}