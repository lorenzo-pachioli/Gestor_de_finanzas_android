package com.notificationcapture.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import androidx.lifecycle.LiveData;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.models.SaldoCuenta;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TransactionEntity> transactions);

    @Query("DELETE FROM transactions WHERE title LIKE 'Saldo Impago%' AND paymentMethod = 'CREDITO'")
    void deleteLegacyArrastres();

    @Update
    void update(TransactionEntity transaction);

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    void updateStatus(String id, String status);

    @Query("SELECT * FROM transactions WHERE status = 'APPROVED' AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<TransactionEntity> getByMonth(long start, long end);

    @Query("SELECT * FROM transactions WHERE status = :status AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<TransactionEntity> getByMonthAndStatus(long start, long end, String status);

    @Query("SELECT * FROM transactions WHERE status = 'APPROVED' AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getByMonthLiveData(long start, long end);

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getAllByStatusLiveData(String status);

    @Query("DELETE FROM transactions WHERE status = 'PENDING' AND timestamp < :timestamp")
    void deleteOldPending(long timestamp);

    @Query("DELETE FROM transactions WHERE status = 'PENDING'")
    void deleteAllPending();

    @Query("SELECT DISTINCT strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch')) as month FROM transactions WHERE status = 'APPROVED' ORDER BY timestamp DESC")
    List<String> getAvailableMonths();

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp BETWEEN :start AND :end AND type = :type AND status = 'APPROVED'")
    Double getSumByMonthAndType(long start, long end, IngresoOEgreso type);

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp BETWEEN :start AND :end AND status = 'APPROVED'")
    Double getTotalByMonth(long start, long end);

    @Query("SELECT MIN(timestamp) FROM transactions")
    Long getOldestRecordTimestamp();

    @Query("DELETE FROM transactions WHERE timestamp BETWEEN :start AND :end")
    void deleteRecordsInRange(long start, long end);

    @Query("SELECT SUM(amount) FROM transactions WHERE creditCardId = :creditCardId AND timestamp BETWEEN :start AND :end AND type = 'EGRESO' AND status = 'APPROVED'")
    Double getMontoResumen(String creditCardId, long start, long end);

    @Query("SELECT SUM(amount) FROM transactions WHERE creditCardId = :creditCardId AND timestamp BETWEEN :start AND :end AND type = 'EGRESO' AND status = 'APPROVED'")
    LiveData<Double> getMontoResumenLiveData(String creditCardId, long start, long end);

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM transactions")
    int getCount();

    @Query("SELECT nombreCuenta, tipoCuenta, saldo, sourceId FROM (" +
           "  SELECT " +
           "  CASE " +
           "    WHEN paymentMethod = 'EFECTIVO' THEN 'Efectivo' " +
           "    WHEN paymentMethod = 'DEBITO' THEN IFNULL(walletId, 'Desconocida') " +
           "    WHEN paymentMethod = 'CREDITO' THEN IFNULL(creditCardId, 'Desconocida') " +
           "    ELSE 'Desconocida' " +
           "  END AS nombreCuenta, " +
           "  paymentMethod AS tipoCuenta, " +
           "  CASE " +
           "    WHEN paymentMethod = 'EFECTIVO' THEN 'EFECTIVO' " +
           "    WHEN paymentMethod = 'DEBITO' THEN walletId " +
           "    WHEN paymentMethod = 'CREDITO' THEN creditCardId " +
           "    ELSE NULL " +
           "  END AS sourceId, " +
           "  CAST(SUM(CASE WHEN type = 'INGRESO' THEN amount ELSE -amount END) " +
           "    + COALESCE((SELECT SUM(cp.montoPagado) FROM credit_card_payments cp WHERE cp.creditCardId = transactions.creditCardId AND cp.timestampPago <= :maxTimestamp), 0) AS REAL) AS saldo " +
           "  FROM transactions " +
           "  WHERE status = 'APPROVED' AND timestamp <= :maxTimestamp " +
           "  GROUP BY nombreCuenta, tipoCuenta, sourceId " +
           ") WHERE ROUND(saldo, 2) != 0 " +
           "ORDER BY tipoCuenta, nombreCuenta")
    List<SaldoCuenta> getSaldosPorCuenta(long maxTimestamp);
}
