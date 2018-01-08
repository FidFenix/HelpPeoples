package com.google.fidel.shiprating;

/**
 * Created by root on 1/2/18.
 */

public class Restaurant {
    private String email;
    private String nombre;
    private String fecha;
    private double longitude;
    private double latitude;
    public Restaurant(){}

    public Restaurant(String email, String nombre, String fecha, double longitude, double latitude) {
        this.email = email;
        this.nombre = nombre;
        this.fecha = fecha;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
}
