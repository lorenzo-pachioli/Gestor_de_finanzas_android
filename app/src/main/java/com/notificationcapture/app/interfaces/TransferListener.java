package com.notificationcapture.app.interfaces;

public interface TransferListener {
    void onTransferConfirmed(String origenId, String destinoId, double monto);
}
