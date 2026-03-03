package com.smartcommerce.model;

public enum UserRole {
    CUSTOMER("ROLE_CUSTOMER"),
    ADMIN("ROLE_ADMIN");

    private final String value;

    UserRole(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
