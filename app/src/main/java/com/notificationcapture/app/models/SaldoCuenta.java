package com.notificationcapture.app.models;

import androidx.room.Ignore;

public class SaldoCuenta {
    private String nombreCuenta;
    private String tipoCuenta;
    private double saldo;

    private String sourceId;

    public SaldoCuenta(String nombreCuenta, String tipoCuenta, double saldo, String sourceId) {
        this.nombreCuenta = nombreCuenta;
        this.tipoCuenta = tipoCuenta;
        this.saldo = saldo;
        this.sourceId = sourceId;
    }

    @Ignore
    public SaldoCuenta(String nombreCuenta, String tipoCuenta, double saldo) {
        this(nombreCuenta, tipoCuenta, saldo, null);
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public void setNombreCuenta(String nombreCuenta) {
        this.nombreCuenta = nombreCuenta;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(String tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}
