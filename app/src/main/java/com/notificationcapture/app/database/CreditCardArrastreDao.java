package com.notificationcapture.app.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CreditCardArrastreDao {

       @Insert(onConflict = OnConflictStrategy.REPLACE)
       void insert(CreditCardArrastreEntity arrastre);

       // Arrastres que "caen" en el mes actual (mesDestinoStart/End coincide con el
       // mes abierto)
       @Query("SELECT COALESCE(SUM(montoArrastre), 0) FROM credit_card_arrastres " +
                     "WHERE creditCardId = :creditCardId " +
                     "AND mesDestinoStart = :start AND mesDestinoEnd = :end")
       double getArrastreParaMes(String creditCardId, long start, long end);

       // Para el BottomSheet: suma total de arrastres pendientes no cubiertos por
       // pagos
       // (se usa en combinación con la query de getSaldosPorCuenta)
       @Query("SELECT COALESCE(SUM(montoArrastre), 0) FROM credit_card_arrastres " +
                     "WHERE creditCardId = :creditCardId " +
                     "AND mesDestinoEnd <= :maxTimestamp")
       double getArrastreTotalHistorico(String creditCardId, long maxTimestamp);

}
