package com.notificationcapture.app.database;

import androidx.room.TypeConverter;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;

public class Converters {
    @TypeConverter
    public static String fromIngresoOEgreso(IngresoOEgreso value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static IngresoOEgreso toIngresoOEgreso(String value) {
        return value == null ? null : IngresoOEgreso.valueOf(value);
    }

    @TypeConverter
    public static String fromPaymentMethod(PaymentMethod value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static PaymentMethod toPaymentMethod(String value) {
        return value == null ? null : PaymentMethod.valueOf(value);
    }
}
