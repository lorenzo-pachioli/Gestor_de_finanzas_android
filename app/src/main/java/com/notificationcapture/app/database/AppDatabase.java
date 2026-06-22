package com.notificationcapture.app.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TransactionEntity.class, CreditCardPaymentEntity.class}, version = 4, exportSchema = true)
@androidx.room.TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;
    public abstract TransactionDao transactionDao();
    public abstract CreditCardPaymentDao creditCardPaymentDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "notification_database")
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    static final androidx.room.migration.Migration MIGRATION_2_3 = new androidx.room.migration.Migration(2, 3) {
        @Override
        public void migrate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
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

    static final androidx.room.migration.Migration MIGRATION_3_4 = new androidx.room.migration.Migration(3, 4) {
        @Override
        public void migrate(@androidx.annotation.NonNull androidx.sqlite.db.SupportSQLiteDatabase database) {
            // 1. Eliminar la tabla de arrastres ya no necesaria
            database.execSQL("DROP TABLE IF EXISTS credit_card_arrastres");
            
            // 2. Crear indices de performance para multitarjeta y busquedas de deuda
            database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_creditCardId ON transactions(creditCardId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_creditCardId ON credit_card_payments(creditCardId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_payments_startTimestamp ON credit_card_payments(startTimestamp)");
        }
    };
    
    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
