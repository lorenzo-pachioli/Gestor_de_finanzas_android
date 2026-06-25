package com.notificationcapture.app.interfaces;

import java.math.BigDecimal;

public interface TransferListener {
    void onTransferConfirmed(String origenId, String destinoId, BigDecimal monto);
}
