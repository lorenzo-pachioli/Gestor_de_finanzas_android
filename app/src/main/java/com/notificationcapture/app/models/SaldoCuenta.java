package com.notificationcapture.app.models;

public class SaldoCuenta {
    private String nombreCuenta;
    private String tipoCuenta;
    private double saldo;

    public SaldoCuenta(String nombreCuenta, String tipoCuenta, double saldo) {
        this.nombreCuenta = nombreCuenta;
        this.tipoCuenta = tipoCuenta;
        this.saldo = saldo;
    }

    public String getNombreCuenta() {
        return nombreCuenta;
    }

    public String getTipoCuenta() {
        return tipoCuenta;
    }

    public double getSaldo() {
        return saldo;
    }
}
