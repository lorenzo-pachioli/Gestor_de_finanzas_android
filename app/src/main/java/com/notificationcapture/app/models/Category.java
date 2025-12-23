package com.notificationcapture.app.models;

import com.notificationcapture.app.NotificationItem;
import com.notificationcapture.app.SpinnerDisplayable;

import java.io.Serializable;

public class Category implements Serializable, SpinnerDisplayable {
    private String name;
    private int color; // Color int value
    private NotificationItem.TransactionType type;

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public Integer getDisplayColor() {
        return color;
    }

    public Category(String name, int color, NotificationItem.TransactionType type) {
        this.name = name;
        this.color = color;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public NotificationItem.TransactionType getType() {
        return type;
    }

    public void setType(NotificationItem.TransactionType type) {
        this.type = type;
    }
}
