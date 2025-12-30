package com.notificationcapture.app.models;

import com.notificationcapture.app.SpinnerDisplayable;
import java.io.Serializable;
import java.util.UUID;

public class CreditCard implements Serializable, SpinnerDisplayable {
    private String id;
    private String name;
    private int closingDate; // Day of month (1-31)
    private int color;

    public CreditCard(String name, int closingDate, int color) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.closingDate = closingDate;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getClosingDate() {
        return closingDate;
    }

    public void setClosingDate(int closingDate) {
        this.closingDate = closingDate;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Integer getDisplayColor() {
        return color;
    }
}
