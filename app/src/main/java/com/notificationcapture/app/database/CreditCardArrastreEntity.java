package com.notificationcapture.app.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "credit_card_arrastres",
        indices = {@Index(value = {"creditCardId"})})
public class CreditCardArrastreEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String creditCardId;

    // Mes de origen del arrastre (primer ms del mes que no se pagó completo)
    private long mesOrigenStart;
    private long mesOrigenEnd;

    // Mes al que se arrastra (primer ms del mes siguiente)
    private long mesDestinoStart;
    private long mesDestinoEnd;

    private double montoArrastre;
    private long timestampCreacion;

    public CreditCardArrastreEntity(String creditCardId,
                                    long mesOrigenStart, long mesOrigenEnd,
                                    long mesDestinoStart, long mesDestinoEnd,
                                    double montoArrastre, long timestampCreacion) {
        this.creditCardId = creditCardId;
        this.mesOrigenStart = mesOrigenStart;
        this.mesOrigenEnd = mesOrigenEnd;
        this.mesDestinoStart = mesDestinoStart;
        this.mesDestinoEnd = mesDestinoEnd;
        this.montoArrastre = montoArrastre;
        this.timestampCreacion = timestampCreacion;
    }

    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCreditCardId() { return creditCardId; }
    public void setCreditCardId(String creditCardId) { this.creditCardId = creditCardId; }
    public long getMesOrigenStart() { return mesOrigenStart; }
    public void setMesOrigenStart(long mesOrigenStart) { this.mesOrigenStart = mesOrigenStart; }
    public long getMesOrigenEnd() { return mesOrigenEnd; }
    public void setMesOrigenEnd(long mesOrigenEnd) { this.mesOrigenEnd = mesOrigenEnd; }
    public long getMesDestinoStart() { return mesDestinoStart; }
    public void setMesDestinoStart(long mesDestinoStart) { this.mesDestinoStart = mesDestinoStart; }
    public long getMesDestinoEnd() { return mesDestinoEnd; }
    public void setMesDestinoEnd(long mesDestinoEnd) { this.mesDestinoEnd = mesDestinoEnd; }
    public double getMontoArrastre() { return montoArrastre; }
    public void setMontoArrastre(double montoArrastre) { this.montoArrastre = montoArrastre; }
    public long getTimestampCreacion() { return timestampCreacion; }
    public void setTimestampCreacion(long timestampCreacion) { this.timestampCreacion = timestampCreacion; }

}
