package com.notificationcapture.app.services;

import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.models.Transaction;
import com.notificationcapture.app.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionService {

    private final TransactionRepository repo;

    TransactionService(TransactionRepository repo) {
        this.repo = repo;
    }

    public List<Transaction> filterByMonth(List<Transaction> all, int month, int year) {
        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (Transaction t : all) {
            cal.setTimeInMillis(t.getTimestamp());
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Transaction> filterCurrentMonth(List<Transaction> all) {
        Calendar now = Calendar.getInstance();
        return filterByMonth(all, now.get(Calendar.MONTH), now.get(Calendar.YEAR));
    }

    public BigDecimal sumByType(List<Transaction> transactions, IngresoOEgreso type) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : transactions) {
            if (t.getType() == type
                    && t.hasAmount()
                    && t.getPaymentMethod() != PaymentMethod.CREDITO) {
                total = total.add(t.getAmount());
            }
        }
        return total;
    }

    public Map<String, BigDecimal> groupByCategoryId(List<Transaction> transactions, IngresoOEgreso type) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() != type || !t.hasAmount()) continue;
            String catId = t.getCategoryId() != null ? t.getCategoryId() : "unknown";
            BigDecimal current = result.getOrDefault(catId, BigDecimal.ZERO);
            result.put(catId, current.add(t.getAmount()));
        }
        return result;
    }

    public BigDecimal calculateBalance(List<Transaction> transactions) {
        BigDecimal ingresos = sumByType(transactions, IngresoOEgreso.INGRESO);
        BigDecimal egresos = sumByType(transactions, IngresoOEgreso.EGRESO);
        return ingresos.subtract(egresos);
    }
}
