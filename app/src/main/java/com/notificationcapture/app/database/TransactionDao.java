package com.notificationcapture.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import androidx.lifecycle.LiveData;
import com.notificationcapture.app.enums.IngresoOEgreso;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TransactionEntity> transactions);

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

    @Query("DELETE FROM transactions WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM transactions")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM transactions")
    int getCount();
}
