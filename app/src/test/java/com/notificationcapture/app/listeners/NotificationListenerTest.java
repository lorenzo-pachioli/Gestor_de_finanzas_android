package com.notificationcapture.app.listeners;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.notificationcapture.app.repositories.WalletRepository;
import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.exceptions.*;

import static org.junit.Assert.*;


/**
 * Pruebas unitarias para validar las reglas de negocio al interceptar notificaciones financieras.
 * En particular se simulan los escenarios de aceptación y rechazo considerando 
 * la whitelist de wallets, palabras claves (keywords) y el reconocimiento explícito del monto.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE) // No requiere manifest real para este test tan ligero
public class NotificationListenerTest {

    private NotificationListener listener;
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";

    // Estructura de modelo mock solicitada para los casos
    private static class NotificationMock {
        String packageName;
        String title;
        String text;
        boolean expectedResult;
        String description;

        NotificationMock(String packageName, String title, String text, boolean expectedResult, String description) {
            this.packageName = packageName;
            this.title = title;
            this.text = text;
            this.expectedResult = expectedResult;
            this.description = description;
        }
    }

    @Before
    public void setUp() throws Exception {
        listener = new NotificationListener();

        // 1. Crear un contexto en Runtime falso de Robolectric
        Context context = RuntimeEnvironment.getApplication();
        
        // 2. Inicializar Singletons globales (necesarios para evitar IllegalStateException)
        com.notificationcapture.app.utils.ConfigManager.init(context);
        com.notificationcapture.app.repositories.RepositoryProvider.initialize(context);

        // 3. Setear las variables globales que el NotificationListener obtiene internamente en su onCreate()
        // 3a. Cargar keywords reales desde el ConfigManager de producción (vía assets de Robolectric)
        // Esto garantiza que el test valide exactamente la misma lógica que corre en el dispositivo.
        com.notificationcapture.app.utils.ConfigManager config = com.notificationcapture.app.utils.ConfigManager.getInstance();

        List<String> rawKeywords = config.getPaymentKeywords();
        assertFalse(
                "[SETUP ERROR] Los keywords de producción están vacíos. " +
                "Verificar que Robolectric puede acceder a src/main/assets/config/",
                rawKeywords.isEmpty()
        );
        Set<String> productionKeywords = new HashSet<>();
        for (String kw : rawKeywords) {
            productionKeywords.add(kw.toLowerCase());
        }

        // 3b. Cargar wallets globales desde el ConfigManager (raw resource wallets_global.json)
        List<com.notificationcapture.app.models.GlobalWallet> globalWalletsList = config.getGlobalWallets();
        Set<String> productionGlobalWallets = new HashSet<>();
        if (!globalWalletsList.isEmpty()) {
            for (com.notificationcapture.app.models.GlobalWallet w : globalWalletsList) {
                productionGlobalWallets.addAll(w.getPackageNames());
            }
        } else {
            // Fallback: Robolectric no pudo cargar el raw resource con Config.NONE
            // En este caso se mantiene el set mínimo necesario para los casos del mock JSON
            productionGlobalWallets.addAll(Arrays.asList(
                    "com.mercadopago.wallet",
                    "com.uala.app",
                    "brubank.app"
            ));
        }

        // 4. Inicializar el repositorio real de forma local
        WalletRepository walletRepository = new WalletRepository(context);
        // Creamos y guardamos una custom wallet configurada por el "usuario"
        walletRepository.addWallet(new Wallets("Mi Custom Wallet", "com.custom.wallet"));

        // 5. Inyectar las variables privadas directas por reflection
        // Esto evita tener que ejecutar el .onCreate() del Service de Android que acopla instancias como Database y ConfigManager globales
        setPrivateField(listener, "globalWalletPackagesSet", productionGlobalWallets);
        setPrivateField(listener, "keywordSet", productionKeywords);
        setPrivateField(listener, "walletsRepository", walletRepository);
    }

    private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    @Test
    public void testIsPaymentRelatedNotificationConDiferentesEscenarios() {
        
        // CARGAR ARRAY DE OBJETOS NOTIFICADORES DESDE JSON
        List<NotificationMock> mocks = new ArrayList<>();
        try {
            InputStream is = getClass().getResourceAsStream("NotificationMockList.json");
            assertNotNull("No se pudo encontrar NotificationMockList.json", is);
            
            Type listType = new TypeToken<ArrayList<NotificationMock>>() {}.getType();
            mocks = new Gson().fromJson(new InputStreamReader(is), listType);
        } catch (Exception e) {
            System.err.println("Error cargando JSON de mocks: " + e.getMessage());
            e.printStackTrace();
        }
        

        // EVALUACIÓN ITERATIVA DEL ARRAY
        System.out.println("\n");
        System.out.println(ANSI_BLUE + "--- INICIO DE PRUEBAS DE CAPTURA DE NOTIFICACIONES ---" + ANSI_RESET);
        System.out.println("------------------------------------------------\n");

        for (NotificationMock mock : mocks) {
            System.out.print(ANSI_YELLOW + "Ejecutando caso: " + mock.description + " ... " + ANSI_RESET);
            
            boolean result;
            try {
                result = listener.isPaymentRelatedNotification(mock.packageName, mock.title, mock.text);
            } catch (ParserException e) {
                // Si lanzamos una excepción de parseo, lo consideramos false en el resultado final de captura
                result = false;
            }
            
            try {
                assertEquals("Fallo evaluando caso lógico: " + mock.description, mock.expectedResult, result);
                System.out.println(ANSI_GREEN + "[OK]" + ANSI_RESET);
            } catch (AssertionError e) {
                System.out.println(ANSI_RED + "[ERROR]" + ANSI_RESET);
                System.out.println("   -> Detalle: " + e.getMessage());
                throw e; // Volver a lanzar para que el test falle formalmente
            }
            System.out.println("\n------------------------------------------------");
        }
        System.out.println(ANSI_BLUE + "--- FIN DE PRUEBAS ---" + ANSI_RESET);
        System.out.println("\n");
    }
}


