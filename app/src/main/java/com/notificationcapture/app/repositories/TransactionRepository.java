package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.database.AppDatabase;
import com.notificationcapture.app.database.TransactionDao;
import com.notificationcapture.app.database.TransactionEntity;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.mappers.TransactionMapper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository implements GsonAccess {

    private final SharedPreferences prefs;
    private final Context context;
    private final TransactionDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    private static final String PREF_MIGRATED = "sqlite_migrated";
    private static final String PREF_MIGRATION_IN_PROGRESS = "sqlite_migration_in_progress";
    private static final String PREF_LAST_MAINTENANCE_MONTH = "last_maintenance_month";

    public interface RepositoryCallback<T> {
        void onResult(T result);
        void onError(Exception e);
    }

    public TransactionRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.dao = AppDatabase.getDatabase(context).transactionDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();

        performMonthlyMaintenance();
        checkAndPerformMigration();
    }

    private void performMonthlyMaintenance() {
        executor.execute(() -> {
            try {
                Calendar cal = Calendar.getInstance();
                int currentMonthYear = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH);
                int lastMonthYear = prefs.getInt(PREF_LAST_MAINTENANCE_MONTH, -1);

                if (currentMonthYear != lastMonthYear) {
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    
                    // Delete all PENDING records from previous months
                    dao.deleteOldPending(cal.getTimeInMillis());
                    
                    prefs.edit().putInt(PREF_LAST_MAINTENANCE_MONTH, currentMonthYear).apply();
                }
            } catch (Exception ignored) {} // Handle DB closed during shutdown
        });
    }

    private void checkAndPerformMigration() {
        if (!prefs.getBoolean(PREF_MIGRATED, false)) {
            executor.execute(() -> {
                try {
                    prefs.edit().putBoolean(PREF_MIGRATION_IN_PROGRESS, true).apply();
                    migrateLegacyList(KEY_NOTIFICATIONS);
                    migrateLegacyList(KEY_NOTIFICATIONS_NOT_FILTERED);
                    prefs.edit().putBoolean(PREF_MIGRATED, true).remove(PREF_MIGRATION_IN_PROGRESS).apply();
                } catch (Exception e) {
                    prefs.edit().putBoolean(PREF_MIGRATION_IN_PROGRESS, false).apply();
                }
            });
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void migrateLegacyList(String key) {
        String json = prefs.getString(key, null);
        if (json != null && !json.isEmpty()) {
            try {
                // Deserializamos como JsonArray para poder inspeccionar cada objeto individualmente
                // y decidir su clase concreta (Credit, Cash, Debit) basándonos en paymentMethod.
                JsonArray array = gson.fromJson(json, JsonArray.class);
                if (array != null) {
                    List<TransactionEntity> entities = new ArrayList<>();
                    String status = KEY_NOTIFICATIONS.equals(key) ? 
                            TransactionEntity.STATUS_APPROVED : TransactionEntity.STATUS_PENDING;

                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        String method = obj.has("paymentMethod") ? obj.get("paymentMethod").getAsString() : "DEBITO";
                        
                        Transaction t;
                        if ("CREDITO".equals(method)) {
                            t = gson.fromJson(obj, com.notificationcapture.app.models.Credit.class);
                        } else if ("EFECTIVO".equals(method)) {
                            t = gson.fromJson(obj, com.notificationcapture.app.models.Cash.class);
                        } else {
                            t = gson.fromJson(obj, com.notificationcapture.app.models.Debit.class);
                        }

                        if (t != null) {
                            entities.add(TransactionMapper.toEntity(t, status));
                        }
                    }
                    if (!entities.isEmpty()) {
                        dao.insertAll(entities);
                    }
                    prefs.edit().remove(key).apply();
                }
            } catch (Exception ignored) {}
        }
    }

    public void saveTransactionNotFiltered(Transaction transaction) {
        executor.execute(() -> {
            TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_PENDING);
            dao.insert(entity);
            checkCleanup();
        });
    }

    public void saveTransaction(Transaction transaction) {
        executor.execute(() -> {
            TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_APPROVED);
            dao.insert(entity);
            checkCleanup();
        });
    }

    public void saveTransactions(List<Transaction> transactions, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = new ArrayList<>();
                for (Transaction t : transactions) {
                    if (t != null) {
                        entities.add(TransactionMapper.toEntity(t, TransactionEntity.STATUS_APPROVED));
                    }
                }
                dao.insertAll(entities);
                checkCleanup();
                if (callback != null) mainHandler.post(() -> callback.onResult(null));
            } catch (Exception e) {
                if (callback != null) mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updateTransaction(Transaction transaction) {
        executor.execute(() -> {
            TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_APPROVED);
            dao.update(entity);
        });
    }

    private void checkCleanup() {
        if (dao.getCount() > 5000) {
            Long oldest = dao.getOldestRecordTimestamp();
            if (oldest != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(oldest);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long start = cal.getTimeInMillis();
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                long end = cal.getTimeInMillis();
                dao.deleteRecordsInRange(start, end);
            }
        }
    }

    public List<Transaction> getAllTransactionNotFiltered() {
        return getByStatusBlocking(TransactionEntity.STATUS_PENDING);
    }

    public List<Transaction> getAllTransactions() {
        return getByStatusBlocking(TransactionEntity.STATUS_APPROVED);
    }

    private List<Transaction> getByStatusBlocking(String status) {
        try {
            return executor.submit(() -> {
                Calendar cal = Calendar.getInstance();
                long start = 0;
                long end = Long.MAX_VALUE;
                if (TransactionEntity.STATUS_PENDING.equals(status)) { // Only current month for Pending
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    start = cal.getTimeInMillis();
                }
                List<TransactionEntity> entities = dao.getByMonthAndStatus(start, end, status);
                return TransactionMapper.fromEntityList(entities);
            }).get();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void getAllTransactions(RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = dao.getByMonthAndStatus(0, Long.MAX_VALUE, TransactionEntity.STATUS_APPROVED);
                List<Transaction> result = TransactionMapper.fromEntityList(entities);
                mainHandler.post(() -> callback.onResult(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deleteTransactionNotFiltered(String id) {
        deleteTransaction(id);
    }
    
    public void moveTransactionToApproved(String id) {
        executor.execute(() -> dao.updateStatus(id, TransactionEntity.STATUS_APPROVED));
    }

    public void clearAllTransactionNotFiltered() {
        executor.execute(dao::deleteAllPending);
    }

    public void getTransactionsByMonth(long start, long end, RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = dao.getByMonth(start, end);
                List<Transaction> result = TransactionMapper.fromEntityList(entities);
                mainHandler.post(() -> callback.onResult(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void deleteTransaction(String id) {
        executor.execute(() -> dao.deleteById(id));
    }

    public double getDatabaseSizeMb() {
        try {
            return executor.submit(() -> {
                SupportSQLiteDatabase db = AppDatabase.getDatabase(context).getOpenHelper().getReadableDatabase();
                long pageCount = db.compileStatement("PRAGMA page_count").simpleQueryForLong();
                long pageSize = db.compileStatement("PRAGMA page_size").simpleQueryForLong();

                return (pageCount * pageSize) / (1024.0 * 1024.0);
            }).get();
        } catch (Exception e) {
            return 0.0;
        }
    }

}
