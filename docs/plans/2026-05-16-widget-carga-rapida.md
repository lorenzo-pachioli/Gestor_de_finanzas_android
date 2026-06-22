# Widget Carga Rápida Implementation Plan

**Goal:** Implementar un widget de Android que permita cargar rápidamente una transacción (ingreso o egreso) mediante una activity transparente flotante, reutilizando el repositorio de transacciones existente.

**Architecture:** El usuario tocará el widget, lo cual lanzará una `FastChargeActivity` con fondo transparente, dando la ilusión de un diálogo sobre la pantalla de inicio. Esta activity recogerá el monto, título y tipo (Ingreso/Egreso), y utilizará `RepositoryProvider.getInstance().getTransactionRepository()` para persistir el dato. Luego notificará al widget para su actualización y se cerrará.

**Tech Stack:** Android SDK (AppWidgets), Java, MVVM, Room (TransactionEntity, RepositoryProvider).

---

### Task 1: Tema Transparente

**Files:**
- Modify: `app/src/main/res/values/themes.xml`

**Step 1: Write the minimal implementation**

Añadir el tema `Theme.NotificationCapture.Transparent` en `themes.xml`:

```xml
    <!-- Tema para la Activity flotante del Widget -->
    <style name="Theme.NotificationCapture.Transparent" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:windowIsTranslucent">true</item>
        <item name="android:windowBackground">@android:color/transparent</item>
        <item name="android:windowContentOverlay">@null</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowIsFloating">true</item>
        <item name="android:backgroundDimEnabled">true</item>
        <item name="android:backgroundDimAmount">0.5</item>
    </style>
```

**Step 2: Commit the changes**

```bash
git add app/src/main/res/values/themes.xml
git commit -m "feat: agregar tema transparente para FastChargeActivity"
```

---

### Task 2: Layout del Widget y Provider Info

**Files:**
- Create: `app/src/main/res/layout/widget_fast_charge.xml`
- Create: `app/src/main/res/xml/widget_fast_charge_info.xml`

**Step 1: Crear el layout del widget**

Crear `app/src/main/res/layout/widget_fast_charge.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_spinner_field"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="+"
        android:textColor="@color/accent_main"
        android:textSize="32sp"
        android:textStyle="bold" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Carga Rápida"
        android:textColor="@color/text_primary"
        android:textSize="14sp" />
</LinearLayout>
```

**Step 2: Crear el info del widget**

Crear `app/src/main/res/xml/widget_fast_charge_info.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/widget_fast_charge"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
```

**Step 3: Commit the changes**

```bash
git add app/src/main/res/layout/widget_fast_charge.xml app/src/main/res/xml/widget_fast_charge_info.xml
git commit -m "feat: crear layouts y xml de configuracion del widget de carga rapida"
```

---

### Task 3: El Widget Provider

**Files:**
- Create: `app/src/main/java/com/notificationcapture/app/widget/FastChargeWidgetProvider.java`

**Step 1: Write the minimal implementation**

Crear la clase `FastChargeWidgetProvider`:

```java
package com.notificationcapture.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.notificationcapture.app.R;
import com.notificationcapture.app.views.FastChargeActivity;

public class FastChargeWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_fast_charge);

        // Lanzar FastChargeActivity al tocar
        Intent intent = new Intent(context, FastChargeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
```

**Step 2: Commit the changes**

```bash
git add app/src/main/java/com/notificationcapture/app/widget/FastChargeWidgetProvider.java
git commit -m "feat: agregar provider para el widget"
```

---

### Task 4: Fast Charge Activity UI

**Files:**
- Create: `app/src/main/res/layout/activity_fast_charge.xml`

**Step 1: Write the minimal implementation**

Crear layout flotante que actúe como un Dialog:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="24dp"
    app:cardCornerRadius="16dp"
    app:cardBackgroundColor="@color/background_card"
    app:cardElevation="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Carga Rápida"
            android:textSize="20sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"
            android:layout_marginBottom="16dp" />

        <EditText
            android:id="@+id/etFastChargeTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Descripción"
            android:inputType="textCapSentences"
            android:layout_marginBottom="12dp" />

        <EditText
            android:id="@+id/etFastChargeAmount"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="$ 0.00"
            android:inputType="numberDecimal"
            android:layout_marginBottom="16dp" />

        <RadioGroup
            android:id="@+id/rgFastChargeType"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="24dp">

            <RadioButton
                android:id="@+id/rbEgreso"
                android:layout_width="0dp"
                android:layout_weight="1"
                android:layout_height="wrap_content"
                android:text="Egreso"
                android:checked="true" />

            <RadioButton
                android:id="@+id/rbIngreso"
                android:layout_width="0dp"
                android:layout_weight="1"
                android:layout_height="wrap_content"
                android:text="Ingreso" />
        </RadioGroup>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end">

            <Button
                android:id="@+id/btnCancelFastCharge"
                style="@style/Widget.Material3.Button.TextButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Cancelar" />

            <Button
                android:id="@+id/btnSaveFastCharge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="8dp"
                android:text="Guardar" />
        </LinearLayout>

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

**Step 2: Commit the changes**

```bash
git add app/src/main/res/layout/activity_fast_charge.xml
git commit -m "feat: crear layout de activity de carga rapida"
```

---

### Task 5: Fast Charge Activity Logic

**Files:**
- Create: `app/src/main/java/com/notificationcapture/app/views/FastChargeActivity.java`

**Step 1: Write the minimal implementation**

Asegurarnos de que inicializa `RepositoryProvider` (por si la app estaba muerta en background).

```java
package com.notificationcapture.app.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.notificationcapture.app.R;
import com.notificationcapture.app.database.TransactionEntity;
import com.notificationcapture.app.enums.IngresoOEgreso;
import com.notificationcapture.app.enums.PaymentMethod;
import com.notificationcapture.app.repositories.RepositoryProvider;

import java.util.UUID;
import java.util.concurrent.Executors;

public class FastChargeActivity extends AppCompatActivity {

    private EditText etTitle, etAmount;
    private RadioGroup rgType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Inicializar RepositoryProvider en caso de que la app haya sido matada
        if (!RepositoryProvider.isInitialized()) {
            RepositoryProvider.initialize(getApplicationContext());
        }

        setContentView(R.layout.activity_fast_charge);

        etTitle = findViewById(R.id.etFastChargeTitle);
        etAmount = findViewById(R.id.etFastChargeAmount);
        rgType = findViewById(R.id.rgFastChargeType);

        Button btnCancel = findViewById(R.id.btnCancelFastCharge);
        Button btnSave = findViewById(R.id.btnSaveFastCharge);

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Por favor completa los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        IngresoOEgreso type = rgType.getCheckedRadioButtonId() == R.id.rbIngreso ? IngresoOEgreso.INGRESO : IngresoOEgreso.EGRESO;

        TransactionEntity entity = new TransactionEntity(
                UUID.randomUUID().toString(),
                PaymentMethod.CASH, // Default para carga rapida
                title,
                "Carga rápida desde Widget",
                System.currentTimeMillis(),
                amount,
                type,
                "other", // ID default de categoria
                false,
                TransactionEntity.STATUS_APPROVED
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            RepositoryProvider.getInstance().getTransactionRepository().insertTransaction(entity);
            runOnUiThread(() -> {
                Toast.makeText(this, "Guardado exitosamente", Toast.LENGTH_SHORT).show();
                // Opcional: Actualizar el widget aquí si mostramos info
                finish();
            });
        });
    }
}
```

**Step 2: Commit the changes**

```bash
git add app/src/main/java/com/notificationcapture/app/views/FastChargeActivity.java
git commit -m "feat: implementar logica para guardar desde carga rapida"
```

---

### Task 6: Registrar en Manifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

**Step 1: Añadir componentes al manifest**

Dentro de la etiqueta `<application>`, agregar el receiver del widget y la Activity flotante:

```xml
        <activity
            android:name=".views.FastChargeActivity"
            android:theme="@style/Theme.NotificationCapture.Transparent"
            android:exported="false"
            android:excludeFromRecents="true" />

        <receiver
            android:name=".widget.FastChargeWidgetProvider"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/widget_fast_charge_info" />
        </receiver>
```

**Step 2: Commit the changes**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: registrar widget provider y fast charge activity en el manifest"
```

---
