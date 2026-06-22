package com.notificationcapture.app.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.utils.AppResult;
import com.notificationcapture.app.exceptions.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TransactionRepositoryTest {

    private TransactionRepository repository;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        com.notificationcapture.app.database.AppDatabase.destroyInstance();
        repository = new TransactionRepository(context);
    }

    @org.junit.After
    public void teardown() {
        if (repository != null) repository.shutdown();
        com.notificationcapture.app.database.AppDatabase.destroyInstance();
    }

    @Test
    public void testMigrationFromSharedPreferences() throws InterruptedException {
        // 1. Prepare legacy data
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String legacyJson = "[{\"id\":\"legacy_1\",\"paymentMethod\":\"DEBITO\",\"title\":\"Legacy\",\"text\":\"Test\",\"timestamp\":1000,\"amount\":10.5,\"isNotification\":true}]";
        prefs.edit().putString("notifications", legacyJson).putBoolean("sqlite_migrated", false).apply();

        // 2. Re-initialize repository to trigger migration
        com.notificationcapture.app.database.AppDatabase.destroyInstance();
        repository = new TransactionRepository(context);

        // 3. Wait for migration flag to be set (poll with ShadowLooper)
        long start = System.currentTimeMillis();
        while (!prefs.getBoolean("sqlite_migrated", false) && (System.currentTimeMillis() - start) < 10000) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(100);
        }

        assertTrue("Migration never completed", prefs.getBoolean("sqlite_migrated", false));

        // 4. Verify
        CountDownLatch latch = new CountDownLatch(1);
        final List<Transaction>[] result = new List[1];
        repository.getAllTransactions(res -> {
            if (res.isSuccess()) {
                result[0] = res.getData();
            }
            latch.countDown();
        });

        while (latch.getCount() > 0) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(100);
        }
        
        assertFalse("Result list should not be null", result[0] == null);
        assertEquals("Should have 1 migrated transaction", 1, result[0].size());
        assertEquals("legacy_1", result[0].get(0).getId());
        assertFalse("Old pref should be removed", prefs.contains("notifications"));
    }

    @Test
    public void testPerformanceAndCleanup() throws InterruptedException {
        int totalToInsert = 5005;
        final List<Transaction> toInsert = new ArrayList<>();

        // 1. Prepare 5000 records from this month
        Calendar currentMonth = Calendar.getInstance();
        for (int i = 0; i < 5000; i++) {
            Debit t = new Debit("Title " + i, "Text", currentMonth.getTimeInMillis(), "wallet", true);
            t.setId("id_" + i);
            toInsert.add(t);
        }

        // 2. Prepare 5 records from 2 months ago (to be deleted)
        Calendar oldMonth = Calendar.getInstance();
        oldMonth.add(Calendar.MONTH, -2);
        for (int i = 5000; i < 5005; i++) {
            Debit t = new Debit("Old " + i, "Text", oldMonth.getTimeInMillis(), "wallet", true);
            t.setId("id_" + i);
            toInsert.add(t);
        }

        // 3. Bulk insert and wait
        long startTime = System.currentTimeMillis();
        final CountDownLatch insertLatch = new CountDownLatch(1);
        final Exception[] error = new Exception[1];
        
        repository.saveTransactions(toInsert, res -> {
            if (!res.isSuccess()) {
                error[0] = res.getError();
            }
            insertLatch.countDown();
        });

        while (insertLatch.getCount() > 0) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(100);
        }
        
        if (error[0] != null) throw new RuntimeException("Insert failed", error[0]);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Inserted 5005 records in: " + duration + "ms");
        System.out.println("The 5005 records weight: " + repository.getDatabaseSizeMb() + " MB");

        // Ultimo test dio los siguientes resultados:
        //   - Inserted 5005 records in: 1203ms
        //   - The 5005 records weight: 0.72265625 MB

        // 4. Verify total count doesn't exceed 5000
        CountDownLatch latch = new CountDownLatch(1);
        final List<Transaction>[] result = new List[1];
        repository.getAllTransactions(res -> {
            if (res.isSuccess()) {
                result[0] = res.getData();
            } else {
                error[0] = res.getError();
            }
            latch.countDown();
        });

        while (latch.getCount() > 0) {
            ShadowLooper.idleMainLooper();
            Thread.sleep(100);
        }
        
        if (error[0] != null) throw new RuntimeException("Fetch failed", error[0]);
        
        assertTrue("Count should be <= 5000 after cleanup, but was " + result[0].size(), result[0].size() <= 5000);
        
        // Verify no "Old" transaction exists
        for (Transaction t : result[0]) {
            assertFalse("Old month data should be deleted: " + t.getTitle(), t.getTitle().startsWith("Old"));
        }
    }
}
