package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import androidx.sqlite.db.SupportSQLiteDatabase;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.notificationcapture.app.database.AppDatabase;
import com.notificationcapture.app.database.TransactionDao;
import com.notificationcapture.app.database.TransactionEntity;
import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.ResumenDeudaTarjeta;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.mappers.TransactionMapper;
import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.repositories.CreditCardRepository;
import com.notificationcapture.app.exceptions.*;
import com.notificationcapture.app.utils.AppLogger;
import com.notificationcapture.app.utils.AppResult;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TransactionRepository implements GsonAccess {

    private final SharedPreferences prefs;
    private final Context context;
    private final TransactionDao dao;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;
    private final WalletRepository walletRepo;
    private final CreditCardRepository creditCardRepo;

    private static final int MAX_RECORDS = 5000;

    private static final String PREF_MIGRATED = "sqlite_migrated";
    private static final String PREF_MIGRATION_IN_PROGRESS = "sqlite_migration_in_progress";
    private static final String PREF_LAST_MAINTENANCE_MONTH = "last_maintenance_month";

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public interface RepositoryCallback<T> {
        void onResult(AppResult<T> result);
    }

    public TransactionRepository(Context context) {

        this.context = context.getApplicationContext();

        SharedPreferences securePrefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "encrypted_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback for extreme cases mapping to an unencrypted pref just to not crash
            // completely,
            // though normally it should just fail securely.
            securePrefs = context.getSharedPreferences(GsonAccess.PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = securePrefs;

        this.dao = AppDatabase.getDatabase(context).transactionDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.walletRepo = RepositoryProvider.getInstance().getWalletRepository();
        this.creditCardRepo = RepositoryProvider.getInstance().getCreditCardRepository();

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
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error in monthly maintenance: " + e.getClass().getSimpleName(), e);
            }
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
                    AppLogger.e("TransactionRepository", "Migration failed", e);
                    prefs.edit().putBoolean(PREF_MIGRATION_IN_PROGRESS, false).apply();
                }
            });
        }
    }

    private long getEndOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

private void migrateLegacyList(String key) {
        String json = prefs.getString(key, null);
        if (json != null && !json.isEmpty()) {
            try {
                JsonArray array = gson.fromJson(json, JsonArray.class);
                if (array != null) {
                    List<TransactionEntity> entities = new ArrayList<>();
                    String status = KEY_NOTIFICATIONS.equals(key) ? TransactionEntity.STATUS_APPROVED
                            : TransactionEntity.STATUS_PENDING;

                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        String method = obj.has("paymentMethod") ? obj.get("paymentMethod").getAsString() : PaymentMethod.TYPE_DEBIT;

                        Transaction t;
                        if (PaymentMethod.TYPE_CREDIT.equals(method)) {
                            t = gson.fromJson(obj, com.notificationcapture.app.models.Credit.class);
                        } else if (PaymentMethod.TYPE_CASH.equals(method)) {
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
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error migrating legacy list: " + key, e);
            }
        }
    }

    public void saveTransactionNotFiltered(Transaction transaction) {
        executor.execute(() -> {
            try {
                TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_PENDING);
                dao.insert(entity);
                checkCleanup();
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error saving non-filtered transaction", e);
            }
        });
    }

    public void saveTransaction(Transaction transaction) {
        executor.execute(() -> {
            try {
                TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_APPROVED);
                dao.insert(entity);
                checkCleanup();
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error saving transaction", e);
            }
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
                if (callback != null)
                    mainHandler.post(() -> callback.onResult(AppResult.success(null)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error saving transactions", e);
                if (callback != null)
                    mainHandler.post(() -> callback
                            .onResult(AppResult.failure(new DatabaseException("Fallo al guardar transacciones", e))));
            }
        });
    }

    public void updateTransaction(Transaction transaction) {
        executor.execute(() -> {
            try {
                TransactionEntity entity = TransactionMapper.toEntity(transaction, TransactionEntity.STATUS_APPROVED);
                dao.update(entity);
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error updating transaction", e);
            }
        });
    }

    private void checkCleanup() {
        if (dao.getCount() > MAX_RECORDS) {
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

    public LiveData<List<Transaction>> getAllTransactionsLiveData() {
        return Transformations.map(dao.getAllByStatusLiveData(TransactionEntity.STATUS_APPROVED),
                TransactionMapper::fromEntityList);
    }

    public LiveData<List<Transaction>> getAllTransactionNotFilteredLiveData() {
        return Transformations.map(dao.getAllByStatusLiveData(TransactionEntity.STATUS_PENDING),
                TransactionMapper::fromEntityList);
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
            AppLogger.e("TransactionRepository", "Error in blocking getByStatus", e);
            return new ArrayList<>();
        }
    }

    public void getAllTransactions(RepositoryCallback<List<Transaction>> callback) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = dao.getByMonthAndStatus(0, Long.MAX_VALUE,
                        TransactionEntity.STATUS_APPROVED);
                List<Transaction> result = TransactionMapper.fromEntityList(entities);
                mainHandler.post(() -> callback.onResult(AppResult.success(result)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error getting all transactions", e);
                mainHandler.post(() -> callback
                        .onResult(AppResult.failure(new DatabaseException("Error al obtener transacciones", e))));
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
                mainHandler.post(() -> callback.onResult(AppResult.success(result)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error getting transactions by month", e);
                mainHandler.post(() -> callback.onResult(
                        AppResult.failure(new DatabaseException("Error al obtener transacciones del mes", e))));
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
            AppLogger.e("TransactionRepository", "Error getting database size", e);
            return 0.0;
        }
    }

    public void getSaldosPorCuenta(
            RepositoryCallback<java.util.List<com.notificationcapture.app.models.SaldoCuenta>> callback) {
        executor.execute(() -> {
            try {
                long endOfMonth = getEndOfCurrentMonth();
                java.util.List<com.notificationcapture.app.models.SaldoCuenta> result = dao
                        .getSaldosPorCuenta(endOfMonth);

                java.util.Map<String, String> walletNames = new java.util.HashMap<>();
                for (com.notificationcapture.app.models.Wallets w : walletRepo.getAllWallets()) {
                    walletNames.put(w.getId(), w.getName());
                }

                java.util.Map<String, String> cardNames = new java.util.HashMap<>();
                for (com.notificationcapture.app.models.CreditCard c : creditCardRepo.getAllCreditCards()) {
                    cardNames.put(c.getId(), c.getName() + " (*" + c.getLast4() + ")");
                }

                java.util.List<com.notificationcapture.app.models.SaldoCuenta> finalResult = new java.util.ArrayList<>();
                for (com.notificationcapture.app.models.SaldoCuenta sc : result) {
                    String finalName = sc.getNombreCuenta();
                    BigDecimal saldo = sc.getSaldo();

                    if (PaymentMethod.TYPE_DEBIT.equals(sc.getTipoCuenta())) {
                        if (walletNames.containsKey(sc.getSourceId())) {
                            finalName = walletNames.get(sc.getSourceId());
                        } else if (sc.getSourceId() == null || PaymentMethod.TYPE_CASH.equals(sc.getSourceId())) {
                            finalName = PaymentMethod.DISPLAY_CASH;
                        } else {
                            finalName = "Billetera (" + sc.getSourceId() + ")";
                        }
                    } else if (PaymentMethod.TYPE_CREDIT.equals(sc.getTipoCuenta())) {
                        if (cardNames.containsKey(sc.getSourceId())) {
                            finalName = cardNames.get(sc.getSourceId());
                        } else {
                            finalName = "Tarjeta (" + sc.getSourceId() + ")";
                        }

                    } else if (PaymentMethod.TYPE_CASH.equals(sc.getTipoCuenta())) {
                        finalName = PaymentMethod.DISPLAY_CASH;
                    }
                    finalResult.add(new com.notificationcapture.app.models.SaldoCuenta(finalName, sc.getTipoCuenta(),
                            saldo, sc.getSourceId()));
                }

                mainHandler.post(() -> callback.onResult(AppResult.success(finalResult)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error getting saldos por cuenta", e);
                mainHandler.post(() -> callback
                        .onResult(AppResult.failure(new DatabaseException("Error al obtener saldos", e))));
            }
        });
    }

    public void efectuarPagoTarjeta(com.notificationcapture.app.database.CreditCardPaymentEntity pago,
            TransactionEntity gastoDeBilletera,
            RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                AppDatabase.getDatabase(context).runInTransaction(() -> {
                    AppDatabase.getDatabase(context).creditCardPaymentDao().insert(pago);
                    dao.insert(gastoDeBilletera);
                });
                mainHandler.post(() -> callback.onResult(AppResult.success(null)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error al procesar el pago de la tarjeta", e);
                mainHandler.post(
                        () -> callback.onResult(AppResult.failure(new DatabaseException("Error procesando pago", e))));
            }
        });
    }

/**
      * Source of truth for historical carryover calculation (unpaid debt).
      * Carryover = (Expenses with timestamp < MonthStart) - (Payments with startTimestamp < MonthStart)
      */
    public BigDecimal getArrastreHistoricoBlocking(String creditCardId, long startOfMonth) {
        try {
            BigDecimal gastosAnteriores = dao.getMontoResumen(creditCardId, 0, startOfMonth - 1);
            BigDecimal totalGastado = gastosAnteriores != null ? gastosAnteriores : BigDecimal.ZERO;

            BigDecimal totalPagado = AppDatabase.getDatabase(context)
                    .creditCardPaymentDao()
                    .getPagosAnteriores(creditCardId, startOfMonth);

            BigDecimal balance = totalGastado.subtract(totalPagado);
            return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
        } catch (Exception e) {
            AppLogger.e("TransactionRepository", "Error calculating historic carryover", e);
            return BigDecimal.ZERO;
        }
    }

    public void getResumenDeudaTarjeta(String creditCardId, String cardName,
            long start, long end,
            RepositoryCallback<ResumenDeudaTarjeta> callback) {
        executor.execute(() -> {
            try {
                long cleanStart = (start / 1000) * 1000;
                long cleanEnd = ((end / 1000) * 1000) + 999;

                BigDecimal gastos = dao.getMontoResumen(creditCardId, cleanStart, cleanEnd);
                BigDecimal gastosDelMes = gastos != null ? gastos : BigDecimal.ZERO;

                BigDecimal arrastre = getArrastreHistoricoBlocking(creditCardId, cleanStart);

                BigDecimal pagos = AppDatabase.getDatabase(context)
                        .creditCardPaymentDao()
                        .getTotalPagadoEnPeriodo(creditCardId, cleanStart);

                ResumenDeudaTarjeta resumen = new ResumenDeudaTarjeta(
                        creditCardId, cardName,
                        gastosDelMes, arrastre, pagos,
                        cleanStart, cleanEnd);

                mainHandler.post(() -> callback.onResult(AppResult.success(resumen)));
            } catch (Exception e) {
                AppLogger.e("TransactionRepository", "Error al calcular resumen deuda", e);
                mainHandler.post(() -> callback.onResult(
                        AppResult.failure(new DatabaseException("Error al calcular deuda", e))));
            }
        });
    }

    public void getMontoResumenTarjeta(String creditCardId, long start, long end, RepositoryCallback<BigDecimal> callback) {
        executor.execute(() -> {
            try {
                BigDecimal total = dao.getMontoResumen(creditCardId, start, end);
                mainHandler.post(() -> callback.onResult(AppResult.success(total == null ? new BigDecimal("0.00") : total)));
            } catch (Exception e) {
                mainHandler.post(() -> callback
                        .onResult(AppResult.failure(new DatabaseException("Error al cargar resumen", e))));
            }
        });
    }

    public void getTotalPagadoTarjeta(String creditCardId, long start, long end, RepositoryCallback<BigDecimal> callback) {
        executor.execute(() -> {
            try {
                BigDecimal total = AppDatabase.getDatabase(context).creditCardPaymentDao()
                        .getTotalPagadoEnPeriodo(creditCardId, start);
                mainHandler.post(() -> callback.onResult(AppResult.success(total)));
            } catch (Exception e) {
                mainHandler.post(() -> callback
                        .onResult(AppResult.failure(new DatabaseException("Error al ver pagos resúmen", e))));
            }
        });
    }

    public void saveTransfer(String origenId, String destinoId, BigDecimal monto, long timestamp,
                             RepositoryCallback<Void> callback) {
        // Generate a groupId to link both transactions
        String groupId = java.util.UUID.randomUUID().toString();

        // Outgoing debit from origin
        com.notificationcapture.app.models.Debit egreso = new com.notificationcapture.app.models.Debit(
                "Transferencia", "", timestamp,
                com.notificationcapture.app.enums.IngresoOEgreso.EGRESO,
                com.notificationcapture.app.repositories.CategoryRepository.TRANSFER_OUT_ID,
                origenId, false);
        egreso.setAmount(monto);

        // Incoming credit to destination
        com.notificationcapture.app.models.Debit ingreso = new com.notificationcapture.app.models.Debit(
                "Transferencia", "", timestamp,
                com.notificationcapture.app.enums.IngresoOEgreso.INGRESO,
                com.notificationcapture.app.repositories.CategoryRepository.TRANSFER_IN_ID,
                destinoId, false);
        ingreso.setAmount(monto);

        // Save both as approved transactions
        List<Transaction> transferPair = new java.util.ArrayList<>();
        transferPair.add(egreso);
        transferPair.add(ingreso);

        saveTransactions(transferPair, callback);
    }

}
