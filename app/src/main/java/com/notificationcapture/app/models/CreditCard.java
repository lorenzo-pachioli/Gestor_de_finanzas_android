package com.notificationcapture.app.models;

import com.notificationcapture.app.interfaces.SpinnerDisplayable;
import java.io.Serializable;
import java.util.UUID;

public class CreditCard  implements Serializable, SpinnerDisplayable {
    private String id;
    private String name;
    private int closingDate; // Day of month (1-31)
    private int color;
    private String last4;

    public CreditCard(String name, int closingDate, int color, String last4) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.closingDate = closingDate;
        this.color = color;
        this.last4 = last4 != null ? last4 : "0000";
    }

    public CreditCard(String name, int closingDate, int color) {
        this(name, closingDate, color, "0000");
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

    public String getLast4() {
        return last4;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }

    public String getDisplayName() {
        return name + (last4 != null && !last4.isEmpty() ? " (*" + last4 + ")" : "");
    }

    public Integer getDisplayColor() {
        return color;
    }
}
