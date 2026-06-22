package com.notificationcapture.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.lifecycle.LiveData;

@Dao
public interface CreditCardPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CreditCardPaymentEntity payment);

    @Query("SELECT COALESCE(SUM(montoPagado), 0) FROM credit_card_payments " +
            "WHERE creditCardId = :creditCardId " +
            "AND startTimestamp >= :start - 1000 AND startTimestamp <= :start + 1000")
    double getTotalPagadoEnPeriodo(String creditCardId, long start);

    @Query("SELECT COALESCE(SUM(montoPagado), 0) FROM credit_card_payments " +
            "WHERE creditCardId = :creditCardId " +
            "AND startTimestamp < :start")
    double getPagosAnteriores(String creditCardId, long start);

    @Query("SELECT COALESCE(SUM(montoPagado), 0) FROM credit_card_payments " +
            "WHERE creditCardId = :creditCardId " +
            "AND startTimestamp >= :start - 1000 AND startTimestamp <= :start + 1000")
    LiveData<Double> getTotalPagadoEnPeriodoLiveData(String creditCardId, long start);

    @Query("SELECT COALESCE(SUM(montoPagado), 0) FROM credit_card_payments " +
            "WHERE creditCardId = :creditCardId " +
            "AND startTimestamp < :start")
    LiveData<Double> getPagosAnterioresLiveData(String creditCardId, long start);
}
