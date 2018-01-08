package com.google.fidel.shiprating;

import java.util.HashMap;
import java.util.List;

/**
 * Created by root on 1/3/18.
 */

public class ClientPreference {
    private double longitude;
    private double latitude;
    private List<String> ingredientList;
    private Object peticionTime;
    public  ClientPreference(){}

    public ClientPreference(double longitude, double latitude, List<String> ingredientList, Object peticionTime) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.ingredientList = ingredientList;
        this.peticionTime = peticionTime;
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

    public List<String> getIngredientList() {
        return ingredientList;
    }

    public void setIngredientList(List<String> ingredientList) {
        this.ingredientList = ingredientList;
    }

    public Object getPeticionTime() {
        return peticionTime;
    }

    public void setPeticionTime(Object peticionTime) {
        this.peticionTime = peticionTime;
    }
}
