package com.notificationcapture.app.models;

import com.notificationcapture.app.enums.CatColors;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.interfaces.SpinnerDisplayable;

import java.io.Serializable;

public class Category implements Serializable, SpinnerDisplayable {
    private String id;
    private String name;
    private CatColors color; // Color int value
    private IngresoOEgreso type;

    @Override
    public String getDisplayName() {
        return name != null ? name : "Sin Nombre";
    }

    @Override
    public Integer getDisplayColor() {
        if (color == null) {
            // Default check to avoid NPE
            color = type == IngresoOEgreso.INGRESO ? CatColors.INGRESO_GREEN : CatColors.EGRESO_RED;
        }
        return color.getColor();
    }

    public Category(String name, IngresoOEgreso type) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.color = CatColors.getOneColorByType(type, 0);
        this.type = type;
    }

    public Category(String id, String name, IngresoOEgreso type) {
        this.id = id;
        this.name = name;
        this.color = CatColors.getOneColorByType(type, 0);
        this.type = type;
    }

    public Category(String name, int color, IngresoOEgreso type) {
        this.id = java.util.UUID.randomUUID().toString();
        this.name = name;
        this.color = CatColors.fromColor(color);
        this.type = type;
    }

    public Category(String id, String name, int color, IngresoOEgreso type) {
        this.id = id;
        this.name = name;
        this.color = CatColors.fromColor(color);
        this.type = type;
    }

    public String getId() {
        if (id == null) {
            // Fallback for old data: use name + type as ID
            id = name + "_" + type;
        }
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

    public CatColors getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = CatColors.fromColor(color);
    }

    public IngresoOEgreso getType() {
        return type;
    }

    public void setType(IngresoOEgreso type) {
        this.type = type;
    }
}
