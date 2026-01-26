package com.notificationcapture.app.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;

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

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TransactionRepositoryTest {

    private TransactionRepository repository;
    private Context context;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        repository = new TransactionRepository(context);
    }

    @Test
    public void testMigrationFromSharedPreferences() throws InterruptedException {
        // 1. Prepare legacy data
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String legacyJson = "[{\"id\":\"legacy_1\",\"paymentMethod\":\"DEBITO\",\"title\":\"Legacy\",\"text\":\"Test\",\"timestamp\":1000,\"amount\":10.5,\"isNotification\":true}]";
        prefs.edit().putString("notifications", legacyJson).putBoolean("sqlite_migrated", false).apply();

        // 2. Re-initialize repository to trigger migration
        repository = new TransactionRepository(context);

        // 3. Wait for migration (async)
        Thread.sleep(1000); 

        // 4. Verify
        CountDownLatch latch = new CountDownLatch(1);
        final List<Transaction>[] result = new List[1];
        repository.getAllTransactions(new TransactionRepository.RepositoryCallback<List<Transaction>>() {
            @Override
            public void onResult(List<Transaction> res) {
                result[0] = res;
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals("Should have 1 migrated transaction", 1, result[0].size());
        assertEquals("legacy_1", result[0].get(0).getId());
        assertFalse("Old pref should be removed", prefs.contains("notifications"));
    }

    @Test
    public void testPerformanceAndCleanup() throws InterruptedException {
        int totalToInsert = 5005; // Force cleanup
        long startTime = System.currentTimeMillis();

        // 1. Insert 5000 records from this month
        Calendar currentMonth = Calendar.getInstance();
        for (int i = 0; i < 5000; i++) {
            Debit t = new Debit("Title " + i, "Text", currentMonth.getTimeInMillis(), "wallet", true);
            t.setId("id_" + i);
            repository.saveTransaction(t);
        }

        // 2. Insert 5 records from 2 months ago (to be deleted)
        Calendar oldMonth = Calendar.getInstance();
        oldMonth.add(Calendar.MONTH, -2);
        for (int i = 5000; i < 5005; i++) {
            Debit t = new Debit("Old " + i, "Text", oldMonth.getTimeInMillis(), "wallet", true);
            t.setId("id_" + i);
            repository.saveTransaction(t);
        }

        // Wait for all async tasks to finish
        Thread.sleep(3000);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Inserted 5005 records in: " + duration + "ms");

        // 3. Verify total count doesn't exceed 5000 (roughly, since cleanup deletes a whole month)
        CountDownLatch latch = new CountDownLatch(1);
        final List<Transaction>[] result = new List[1];
        repository.getAllTransactions(new TransactionRepository.RepositoryCallback<List<Transaction>>() {
            @Override
            public void onResult(List<Transaction> res) {
                result[0] = res;
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                latch.countDown();
            }
        });

        latch.await(5, TimeUnit.SECONDS);
        
        // After cleanup of the oldest month, we should have 5000 records (the current ones)
        // and 0 from the old month.
        assertTrue("Count should be <= 5000 after cleanup", result[0].size() <= 5000);
        
        // Verify no "Old" transaction exists
        for (Transaction t : result[0]) {
            assertFalse("Old month data should be deleted", t.getTitle().startsWith("Old"));
        }
    }
}
