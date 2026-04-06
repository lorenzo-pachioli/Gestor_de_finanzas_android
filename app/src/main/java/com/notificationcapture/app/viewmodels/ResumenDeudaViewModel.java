package com.notificationcapture.app.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.notificationcapture.app.database.CreditCardPaymentDao;
import com.notificationcapture.app.database.TransactionDao;
import com.notificationcapture.app.models.ResumenDeudaTarjeta;

public class ResumenDeudaViewModel extends ViewModel {

    private final MediatorLiveData<ResumenDeudaTarjeta> resumenLiveData = new MediatorLiveData<>();
    
    private LiveData<Double> gastosMes;
    private LiveData<Double> gastosPasados;
    private LiveData<Double> pagosMes;
    private LiveData<Double> pagosPasados;

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

        // Limpiar fuentes previas si existen (por si se llama init varias veces)
        if (gastosMes != null) resumenLiveData.removeSource(gastosMes);
        if (gastosPasados != null) resumenLiveData.removeSource(gastosPasados);
        if (pagosMes != null) resumenLiveData.removeSource(pagosMes);
        if (pagosPasados != null) resumenLiveData.removeSource(pagosPasados);

        // Inicializar LiveDatas desde DAOs
        gastosMes = transactionDao.getMontoResumenLiveData(creditCardId, start, end);
        gastosPasados = transactionDao.getMontoResumenLiveData(creditCardId, 0, start - 1);
        pagosMes = paymentDao.getTotalPagadoEnPeriodoLiveData(creditCardId, start);
        pagosPasados = paymentDao.getPagosAnterioresLiveData(creditCardId, start);

        Observer<Double> observer = value -> {
            Double gM = gastosMes.getValue();
            Double gP = gastosPasados.getValue();
            Double pM = pagosMes.getValue();
            Double pP = pagosPasados.getValue();

            double valGastosMes = gM != null ? gM : 0.0;
            double valGastosPasados = gP != null ? gP : 0.0;
            double valPagosMes = pM != null ? pM : 0.0;
            double valPagosPasados = pP != null ? pP : 0.0;

            double arrastre = Math.max(0, valGastosPasados - valPagosPasados);

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
