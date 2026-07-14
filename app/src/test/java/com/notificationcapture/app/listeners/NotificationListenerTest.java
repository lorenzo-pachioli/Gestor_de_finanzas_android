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

import com.notificationcapture.app.models.Wallets;
import com.notificationcapture.app.exceptions.*;
import com.notificationcapture.app.services.NotificationParserService;
import com.notificationcapture.app.services.ServiceProvider;

import static org.junit.Assert.*;


/**
 * Pruebas unitarias para validar las reglas de negocio al interceptar notificaciones financieras.
 * En particular se simulan los escenarios de aceptación y rechazo considerando 
 * la whitelist de wallets, palabras claves (keywords) y el reconocimiento explícito del monto.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE) // No requiere manifest real para este test tan ligero
public class NotificationListenerTest {

    private NotificationParserService parserService;
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
        // 1. Crear un contexto en Runtime falso de Robolectric
        Context context = RuntimeEnvironment.getApplication();
        
        // 2. Inicializar Singletons globales (necesarios para evitar IllegalStateException)
        com.notificationcapture.app.utils.ConfigManager.init(context);
        com.notificationcapture.app.repositories.RepositoryProvider.initialize(context);
        ServiceProvider.resetForTesting();

        // 3. Setear las variables globales que el parser obtiene internamente
        com.notificationcapture.app.utils.ConfigManager config = com.notificationcapture.app.utils.ConfigManager.getInstance();

        List<String> rawKeywords = config.getPaymentKeywords();
        assertFalse(
                "[SETUP ERROR] Los keywords de producción están vacíos. " +
                "Verificar que Robolectric puede acceder a src/main/assets/config/",
                rawKeywords.isEmpty()
        );

        parserService = ServiceProvider.getInstance().getNotificationParserService();

        // Fallback: Robolectric no pudo cargar el raw resource con Config.NONE
        // En este caso se mantiene el set mínimo necesario para los casos del mock JSON
        List<com.notificationcapture.app.models.GlobalWallet> globalWalletsList = config.getGlobalWallets();
        if (globalWalletsList.isEmpty()) {
            Set<String> fallbackWallets = new HashSet<>(Arrays.asList(
                    "com.mercadopago.wallet",
                    "com.uala.app",
                    "brubank.app"
            ));
            setPrivateField(parserService, "globalWalletPackages", fallbackWallets);
        }
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
        
        // Custom wallets expected by test
        List<Wallets> userWallets = new ArrayList<>();
        userWallets.add(new Wallets("Mi Custom Wallet", "com.custom.wallet"));

        // EVALUACIÓN ITERATIVA DEL ARRAY
        System.out.println("\n");
        System.out.println(ANSI_BLUE + "--- INICIO DE PRUEBAS DE CAPTURA DE NOTIFICACIONES ---" + ANSI_RESET);
        System.out.println("------------------------------------------------\n");

        for (NotificationMock mock : mocks) {
            System.out.print(ANSI_YELLOW + "Ejecutando caso: " + mock.description + " ... " + ANSI_RESET);
            
            boolean result;
            try {
                result = parserService.isPaymentNotification(mock.packageName, mock.title, mock.text, userWallets);
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



