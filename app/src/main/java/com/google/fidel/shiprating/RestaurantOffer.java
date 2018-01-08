package com.google.fidel.shiprating;

import java.util.List;

/**
 * Created by root on 1/4/18.
 */

public class RestaurantOffer {
    private List<String> ingredients;
    private Object offerTime;

    public RestaurantOffer(){}
    public RestaurantOffer(List<String> ingredients, Object offerTime) {
        this.ingredients = ingredients;
        this.offerTime = offerTime;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public Object getOfferTime() {
        return offerTime;
    }

    public void setOfferTime(Object offerTime) {
        this.offerTime = offerTime;
    }
}
