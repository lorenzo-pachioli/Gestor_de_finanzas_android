package com.notificationcapture.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.notificationcapture.app.database.CreditCardPaymentDao;
import com.notificationcapture.app.database.TransactionDao;
import com.notificationcapture.app.models.ResumenDeudaTarjeta;

import java.math.BigDecimal;

public class ResumenDeudaViewModel extends ViewModel {

    private final MediatorLiveData<ResumenDeudaTarjeta> resumenLiveData = new MediatorLiveData<>();
    
    private LiveData<BigDecimal> gastosMes;
    private LiveData<BigDecimal> gastosPasados;
    private LiveData<BigDecimal> pagosMes;
    private LiveData<BigDecimal> pagosPasados;

    private String creditCardId;
    private String cardName;
    private long start;
    private long end;

    public void init(String creditCardId, String cardName, long start, long end, 
                     TransactionDao transactionDao, CreditCardPaymentDao paymentDao) {
        this.creditCardId = creditCardId;
        this.cardName = cardName;
        this.start = start;
        this.end = end;

        if (gastosMes != null) resumenLiveData.removeSource(gastosMes);
        if (gastosPasados != null) resumenLiveData.removeSource(gastosPasados);
        if (pagosMes != null) resumenLiveData.removeSource(pagosMes);
        if (pagosPasados != null) resumenLiveData.removeSource(pagosPasados);

        gastosMes = transactionDao.getMontoResumenLiveData(creditCardId, start, end);
        gastosPasados = transactionDao.getMontoResumenLiveData(creditCardId, 0, start - 1);
        pagosMes = paymentDao.getTotalPagadoEnPeriodoLiveData(creditCardId, start);
        pagosPasados = paymentDao.getPagosAnterioresLiveData(creditCardId, start);

        Observer<BigDecimal> observer = value -> {
            BigDecimal gM = gastosMes.getValue();
            BigDecimal gP = gastosPasados.getValue();
            BigDecimal pM = pagosMes.getValue();
            BigDecimal pP = pagosPasados.getValue();

            BigDecimal valGastosMes = gM != null ? gM : BigDecimal.ZERO;
            BigDecimal valGastosPasados = gP != null ? gP : BigDecimal.ZERO;
            BigDecimal valPagosMes = pM != null ? pM : BigDecimal.ZERO;
            BigDecimal valPagosPasados = pP != null ? pP : BigDecimal.ZERO;

            BigDecimal arrastre = valGastosPasados.subtract(valPagosPasados);
            if (arrastre.compareTo(BigDecimal.ZERO) < 0) {
                arrastre = BigDecimal.ZERO;
            }

            resumenLiveData.setValue(new ResumenDeudaTarjeta(
                    creditCardId, cardName, valGastosMes, arrastre, valPagosMes, start, end
            ));
        };

        resumenLiveData.addSource(gastosMes, observer);
        resumenLiveData.addSource(gastosPasados, observer);
        resumenLiveData.addSource(pagosMes, observer);
        resumenLiveData.addSource(pagosPasados, observer);
    }

    public LiveData<ResumenDeudaTarjeta> getResumen() {
        return resumenLiveData;
    }
}
