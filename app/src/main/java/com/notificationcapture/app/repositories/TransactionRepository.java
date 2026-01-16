package com.notificationcapture.app.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.notificationcapture.app.interfaces.GsonAccess;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.utils.Dialog;

public class TransactionRepository implements GsonAccess {

    private SharedPreferences prefs;
    private Gson gson;
    private Gson plainGson; // Plain Gson to avoid recursion in adapter
    private Context context;

    public TransactionRepository(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.context = context.getApplicationContext();

        // Initialize plain Gson first
        this.plainGson = new Gson();

        // Initialize main Gson with the adapter
        this.gson = new com.google.gson.GsonBuilder()
                .registerTypeAdapter(Transaction.class, new TransactionAdapter())
                .create();
    }

    public void saveTransactionNotFiltered(Transaction transaction) {
        List<Transaction> notifications = getAllTransactionNotFiltered();

        // Agregar al inicio de la lista
        notifications.add(0, transaction);

        // Limitar el número de notificaciones guardadas
        if (notifications.size() > MAX_NOTIFICATIONS) {
            notifications = notifications.subList(0, MAX_NOTIFICATIONS);
        }

        // Guardar en SharedPreferences
        String json = gson.toJson(notifications);
        prefs.edit().putString(KEY_NOTIFICATIONS_NOT_FILTERED, json).apply();
    }

    public void saveTransaction(Transaction transaction) {
        try {
            List<Transaction> transactionList = getAllTransactions();

            // Agregar al inicio de la lista
            transactionList.add(0, transaction);

            // Limitar el número de notificaciones guardadas
            if (transactionList.size() > MAX_NOTIFICATIONS) {
                transactionList = transactionList.subList(0, MAX_NOTIFICATIONS);
            }

            // Guardar en SharedPreferences
            String json = gson.toJson(transactionList);
            prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
        } catch (Exception e) {
            // Toast.makeText(context, "Error: " + e.getMessage(), 5);
            Dialog.show("Error: " + e.getMessage());
        }
    }

    public List<Transaction> getAllTransactionNotFiltered() {
        String json = prefs.getString(KEY_NOTIFICATIONS_NOT_FILTERED, null);

        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        Type type = new TypeToken<List<Transaction>>() {
        }.getType();
        List<Transaction> transactionList = gson.fromJson(json, type);

        if (transactionList != null) {
            transactionList.removeIf(java.util.Objects::isNull);
            return transactionList;
        }
        return new ArrayList<>();
    }

    public List<Transaction> getAllTransactions() {
        try {
            String json = prefs.getString(KEY_NOTIFICATIONS, null);

            if (json == null || json.isEmpty()) {
                return new ArrayList<>();
            }

            Type type = new TypeToken<List<Transaction>>() {
            }.getType();
            List<Transaction> transactionList = gson.fromJson(json, type);

            if (transactionList != null) {
                // Filter out nulls that might have occurred during deserialization
                List<Transaction> cleanList = new ArrayList<>();
                for (Transaction t : transactionList) {
                    if (t != null) {
                        cleanList.add(t);
                    }
                }
                return cleanList;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    public void deleteTransaction(String id) {
        List<Transaction> transactionList = getAllTransactions();

        // Find the transaction to be deleted first to check for group ID
        String groupId = null;
        for (Transaction transaction : transactionList) {
            if (transaction.getId().equals(id)) {
                if (transaction instanceof Credit c)
                    groupId = c.getInstallmentGroupId();
                break;
            }
        }

        // Filtrar removiendo la transaction con el ID especificado o todas las del
        // grupo
        List<Transaction> updatedList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            boolean shouldDelete = false;

            if (groupId != null) {
                if (transaction instanceof Credit c) {
                    if (c.getInstallmentGroupId() != null && groupId.equals(c.getInstallmentGroupId())) {
                        // If part of the same group, delete it
                        shouldDelete = true;
                    }
                }
            } else if (transaction.getId().equals(id)) {
                // Standard single deletion
                shouldDelete = true;
            }

            if (!shouldDelete) {
                updatedList.add(transaction);
            }
        }

        // Guardar la lista actualizada
        String json = gson.toJson(updatedList);
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply();
    }

    public void deleteTransactionNotFiltered(String id) {
        List<Transaction> transactionList = getAllTransactionNotFiltered();

        // Find the transaction to be deleted first to check for group ID
        String groupId = null;
        for (Transaction transaction : transactionList) {
            if (transaction.getId().equals(id)) {
                if (transaction instanceof Credit c)
                    groupId = c.getInstallmentGroupId();
                break;
            }
        }

        // Filtrar removiendo la notificación con el ID especificado o todas las del
        // grupo
        List<Transaction> updatedList = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            boolean shouldDelete = false;

            if (groupId != null) {
                if (transaction instanceof Credit c) {
                    if (c.getInstallmentGroupId() != null && groupId.equals(c.getInstallmentGroupId())) {
                        // If part of the same group, delete it
                        shouldDelete = true;
                    }
                }
            } else if (transaction.getId().equals(id)) {
                shouldDelete = true;
            }

            if (!shouldDelete) {
                updatedList.add(transaction);
            }
        }

        // Guardar la lista actualizada
        String json = gson.toJson(updatedList);
        prefs.edit().putString(KEY_NOTIFICATIONS_NOT_FILTERED, json).apply();
    }

    public void updateTransaction(Transaction updatedTransaction) {
        boolean updatedInApproved = updateTransactionInSpecificList(KEY_NOTIFICATIONS, updatedTransaction);
        boolean updatedInNotFiltered = updateTransactionInSpecificList(KEY_NOTIFICATIONS_NOT_FILTERED,
                updatedTransaction);

        if (!updatedInApproved && !updatedInNotFiltered) {
            // Optional: Log or handle case where transaction wasn't found in either list
        }
    }

    private boolean updateTransactionInSpecificList(String key, Transaction updatedTransaction) {
        List<Transaction> transactionList;
        if (KEY_NOTIFICATIONS.equals(key)) {
            transactionList = getAllTransactions();
        } else {
            transactionList = getAllTransactionNotFiltered();
        }

        String groupId = updatedTransaction instanceof Credit c ? c.getInstallmentGroupId() : null;
        boolean found = false;

        for (int i = 0; i < transactionList.size(); i++) {
            Transaction current = transactionList.get(i);

            if (groupId != null && current instanceof Credit c && groupId.equals(c.getInstallmentGroupId())) {
                // It's part of the same group (or the transaction itself)
                Credit updatedCredit = (Credit) updatedTransaction;

                // Update shared fields
                c.setText(updatedCredit.getText());
                c.setCategoryId(updatedTransaction.getCategoryId());
                c.setAmount(updatedCredit.getAmount());
                c.setType(updatedCredit.getType());
                c.setPaymentMethod(updatedCredit.getPaymentMethod());
                c.setCreditCardId(updatedCredit.getCreditCardId());
                c.setInstallments(updatedCredit.getInstallments());
                c.setNotification(updatedCredit.isNotification());
                // Preserved fields: Id, Timestamp, CurrentInstallment

                found = true;
            } else if (groupId == null && current.getId().equals(updatedTransaction.getId())) {
                // Standard single update (not a credit group or no group ID)
                transactionList.set(i, updatedTransaction);
                found = true;
                break; // Unique ID match, we can stop if not updating a group
            }
        }

        if (found) {
            String json = gson.toJson(transactionList);
            prefs.edit().putString(key, json).apply();
        }
        return found;
    }

    public void clearAllTransaction() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply();
    }

    public void moveTransactionToApproved(String id) {
        List<Transaction> notFilteredList = getAllTransactionNotFiltered();
        List<Transaction> approvedList = getAllTransactions();

        // Find the transaction and check for group ID
        String groupId = null;
        for (Transaction transaction : notFilteredList) {
            if (transaction.getId().equals(id)) {
                if (transaction instanceof Credit c)
                    groupId = c.getInstallmentGroupId();
                break;
            }
        }

        // Move transaction(s) from not filtered to approved
        List<Transaction> remainingNotFiltered = new ArrayList<>();
        for (Transaction transaction : notFilteredList) {
            boolean shouldMove = false;

            if (groupId != null) {
                // If it's a credit with group, move all in the group
                if (transaction instanceof Credit c) {
                    if (c.getInstallmentGroupId() != null && groupId.equals(c.getInstallmentGroupId())) {
                        shouldMove = true;
                    }
                }
            } else if (transaction.getId().equals(id)) {
                // Standard single move
                shouldMove = true;
            }

            if (shouldMove) {
                // Set isNotification to true before adding to approved list
                transaction.setNotification(true);
                approvedList.add(0, transaction); // Add to approved list
            } else {
                remainingNotFiltered.add(transaction); // Keep in not filtered
            }
        }

        // Save both lists
        String notFilteredJson = gson.toJson(remainingNotFiltered);
        prefs.edit().putString(KEY_NOTIFICATIONS_NOT_FILTERED, notFilteredJson).apply();

        String approvedJson = gson.toJson(approvedList);
        prefs.edit().putString(KEY_NOTIFICATIONS, approvedJson).apply();
    }

    public void clearAllTransactionNotFiltered() {
        prefs.edit().remove(KEY_NOTIFICATIONS_NOT_FILTERED).apply();
    }

    // Custom Adapter for Polymorphic Transaction
    private class TransactionAdapter implements JsonDeserializer<Transaction>, JsonSerializer<Transaction> {

        @Override
        public JsonElement serialize(Transaction src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            // Use plainGson to avoid recursion
            result.add("properties", plainGson.toJsonTree(src));
            result.addProperty("type", src.getPaymentMethod().toString());
            return result;
        }

        @Override
        public Transaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject(); // The element we are looking at
            JsonElement actualData = json; // The data we will actually deserialize (properties or the object itself)
            String type = "DEBITO"; // Default fallback

            // 1. Determine if it's a wrapper or flat
            if (jsonObject.has("properties") && jsonObject.has("type")) {
                actualData = jsonObject.get("properties");
            }

            // 2. Try to find paymentMethod in the actual data (Preferred source of truth)
            if (actualData.isJsonObject() && actualData.getAsJsonObject().has("paymentMethod")) {
                type = actualData.getAsJsonObject().get("paymentMethod").getAsString();
            } else if (jsonObject.has("type")) {
                // Fallback to wrapper type if paymentMethod is missing in the data but present
                // in wrapper
                type = jsonObject.get("type").getAsString();
            }

            // 3. Deserialize based on type
            switch (type) {
                case "CREDITO":
                    return plainGson.fromJson(actualData, Credit.class);
                case "EFECTIVO":
                    return plainGson.fromJson(actualData, Cash.class);
                case "DEBITO":
                default:
                    return plainGson.fromJson(actualData, Debit.class);
            }
        }
    }
}