package com.notificationcapture.app.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.startup.Initializer;
import com.notificationcapture.app.repositories.RepositoryProvider;
import java.util.Collections;
import java.util.List;

public class AppInitializer implements Initializer<Void> {
    @NonNull
    @Override
    public Void create(@NonNull Context context) {
        // Initialize repository provider off the main thread if possible, 
        // though RepositoryProvider.initialize usually creates the DB.
        RepositoryProvider.initialize(context);
        return null;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
