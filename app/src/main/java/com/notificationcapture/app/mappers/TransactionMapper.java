package com.notificationcapture.app.mappers;

import com.notificationcapture.app.database.TransactionEntity;
import com.notificationcapture.app.models.Cash;
import com.notificationcapture.app.models.Credit;
import com.notificationcapture.app.models.Debit;
import com.notificationcapture.app.models.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionMapper {

    /**
     * Converts a domain Transaction model to a database TransactionEntity.
     *
     * @param t      The domain transaction.
     * @param status The status for the entity (e.g., APPROVED, PENDING).
     * @return A TransactionEntity ready for database persistence.
     */
    public static TransactionEntity toEntity(Transaction t, String status) {
        if (t == null) return null;

        TransactionEntity e = new TransactionEntity(
                t.getId(),
                t.getPaymentMethod(),
                t.getTitle(),
                t.getText(),
                t.getTimestamp(),
                t.getAmount(),
                t.getType(),
                t.getCategoryId(),
                t.isNotification(),
                status
        );
        e.setExpanded(t.isExpanded());
        e.setRawNotification(t.getText()); // Defaulting text as raw

        if (t instanceof Credit) {
            Credit c = (Credit) t;
            e.setCreditCardId(c.getCreditCardId());
            e.setInstallments(c.getInstallments());
            e.setCurrentInstallment(c.getCurrentInstallment());
            e.setInstallmentGroupId(c.getInstallmentGroupId());
        } else if (t instanceof Debit) {
            Debit d = (Debit) t;
            e.setWalletId(d.getWalletId());
        }
        return e;
    }

    /**
     * Converts a database TransactionEntity to a domain Transaction model.
     *
     * @param e The database entity.
     * @return The corresponding domain model.
     */
    public static Transaction fromEntity(TransactionEntity e) {
        if (e == null) return null;

        Transaction t;
        switch (e.getPaymentMethod()) {
            case CREDITO:
                t = new Credit(e.getTitle(), e.getText(), e.getTimestamp(), e.getCreditCardId(),
                        e.getInstallments(), e.getCurrentInstallment(), e.getInstallmentGroupId(), e.isNotification());
                break;
            case EFECTIVO:
                t = new Cash(e.getTitle(), e.getText(), e.getTimestamp(), e.isNotification());
                break;
            case DEBITO:
            default:
                t = new Debit(e.getTitle(), e.getText(), e.getTimestamp(), e.getWalletId(), e.isNotification());
                break;
        }
        t.setId(e.getId());
        t.setAmount(e.getAmount());
        t.setType(e.getType());
        t.setCategoryId(e.getCategoryId());
        t.setExpanded(e.isExpanded());
        return t;
    }

    /**
     * Converts a list of entities to a list of domain models.
     */
    public static List<Transaction> fromEntityList(List<TransactionEntity> entities) {
        List<Transaction> result = new ArrayList<>();
        if (entities != null) {
            for (TransactionEntity e : entities) {
                result.add(fromEntity(e));
            }
        }
        return result;
    }
}
