package com.notificationcapture.app.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.notificationcapture.app.constants.DatabaseConstants;


@Database(entities = {TransactionEntity.class, CreditCardPaymentEntity.class}, version = DatabaseConstants.DB_VERSION, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    public abstract TransactionDao transactionDao();
    public abstract CreditCardPaymentDao creditCardPaymentDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, DatabaseConstants.DB_NAME)
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Crear la nueva tabla
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS credit_card_arrastres (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "creditCardId TEXT, " +
                            "mesOrigenStart INTEGER NOT NULL, " +
                            "mesOrigenEnd INTEGER NOT NULL, " +
                            "mesDestinoStart INTEGER NOT NULL, " +
                            "mesDestinoEnd INTEGER NOT NULL, " +
                            "montoArrastre REAL NOT NULL, " +
                            "timestampCreacion INTEGER NOT NULL)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_credit_card_arrastres_creditCardId " +
                            "ON credit_card_arrastres (creditCardId)"
            );
            // 2. Eliminar arrastres legacy de transactions
            database.execSQL(
                    "DELETE FROM transactions WHERE title LIKE 'Saldo Impago%' AND paymentMethod = 'CREDITO'"
            );
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 1. Eliminar la tabla de arrastres ya no necesaria
            database.execSQL("DROP TABLE IF EXISTS credit_card_arrastres");
            
            // 2. Crear indices de performance para multitarjeta y busquedas de deuda
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_creditCardId ON transactions(creditCardId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_creditCardId ON credit_card_payments(creditCardId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_startTimestamp ON credit_card_payments(startTimestamp)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Migrar credit_card_payments: columnas de monto a TEXT
            database.execSQL("ALTER TABLE credit_card_payments RENAME TO credit_card_payments_old");
            database.execSQL(
                    "CREATE TABLE credit_card_payments (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "creditCardId TEXT, " +
                            "startTimestamp INTEGER NOT NULL, " +
                            "endTimestamp INTEGER NOT NULL, " +
                            "montoTotalResumen TEXT NOT NULL, " +
                            "montoPagado TEXT NOT NULL, " +
                            "walletIdPago TEXT, " +
                            "timestampPago INTEGER NOT NULL)"
            );
            database.execSQL(
                    "INSERT INTO credit_card_payments " +
                            "SELECT id, creditCardId, startTimestamp, endTimestamp, " +
                            "CAST(montoTotalResumen AS TEXT), CAST(montoPagado AS TEXT), " +
                            "walletIdPago, timestampPago FROM credit_card_payments_old"
            );
            database.execSQL("DROP TABLE credit_card_payments_old");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_creditCardId ON credit_card_payments(creditCardId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_startTimestamp ON credit_card_payments(startTimestamp)");

            // Migrar transactions: columnas amount a TEXT
            database.execSQL("ALTER TABLE transactions RENAME TO transactions_old");
            database.execSQL(
                    "CREATE TABLE transactions (" +
                            "id TEXT PRIMARY KEY NOT NULL, " +
                            "paymentMethod TEXT, " +
                            "title TEXT, " +
                            "text TEXT, " +
                            "timestamp INTEGER NOT NULL, " +
                            "amount TEXT NOT NULL, " +
                            "type TEXT, " +
                            "categoryId TEXT, " +
                            "isNotification INTEGER, " +
                            "status TEXT, " +
                            "expanded INTEGER, " +
                            "creditCardId TEXT, " +
                            "installments INTEGER, " +
                            "currentInstallment INTEGER, " +
                            "installmentGroupId TEXT, " +
                            "walletId TEXT, " +
                            "rawNotification TEXT" +
                            ")"
            );
            database.execSQL(
                    "INSERT INTO transactions " +
                            "SELECT id, paymentMethod, title, text, timestamp, " +
                            "CAST(amount AS TEXT), type, categoryId, isNotification, status, " +
                            "expanded, creditCardId, installments, currentInstallment, installmentGroupId, " +
                            "walletId, rawNotification FROM transactions_old"
            );
            database.execSQL("DROP TABLE transactions_old");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_status ON transactions(status)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_creditCardId ON transactions(creditCardId)");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN sourcePackageName TEXT");
        }
    };
    
    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
