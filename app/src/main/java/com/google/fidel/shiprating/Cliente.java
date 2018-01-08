package com.google.fidel.shiprating;

/**
 * Created by root on 1/1/18.
 */

public class Cliente {
    private String email;
    private ClientType clientType;

    public Cliente(){}

    public Cliente(String email, ClientType clientType) {
        this.email = email;
        this.clientType = clientType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }
}
