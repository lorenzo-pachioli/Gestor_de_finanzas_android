package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.interfaces.SpinnerDisplayable;
import com.notificationcapture.app.enums.TransactionType;

import java.io.Serializable;

public class Category implements Serializable, SpinnerDisplayable {
    private String name;
    private CatColors color; // Color int value
    private TransactionType type;

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Integer getDisplayColor() {
        return color.getColor();
    }

    public Category(String name, TransactionType type) {
        this.name = name;
        this.color = CatColors.getOneColorByType(type, 0);
        this.type = type;
    }

    public Category(String name, int color, TransactionType type) {
        this.name = name;
        this.color = CatColors.fromColor(color);
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CatColors getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = CatColors.fromColor(color);
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }
}
