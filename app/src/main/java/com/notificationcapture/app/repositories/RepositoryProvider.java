package com.notificationcapture.app.repositories;

import android.content.Context;

/**
 * Singleton que provee acceso único y centralizado a todos los repositorios de la aplicación.
 * Garantiza que solo exista una instancia de cada repositorio durante el ciclo de vida de la app.
 *
 * Patrón utilizado: Singleton con Lazy Loading
 *
 * @author Tu Nombre
 * @version 1.0
 */
public class RepositoryProvider {

    private static RepositoryProvider instance;

    // Instancias de repositorios (lazy loading)
    private CategoryRepository categoryRepository;
    private TransactionRepository transactionRepository;
    private CreditCardRepository creditCardRepository;
    private WalletRepository walletRepository;

    private final Context context;

    /**
     * Constructor privado para evitar instanciación externa.
     * Implementa el patrón Singleton.
     *
     * @param context Contexto de la aplicación Android
     */
    private RepositoryProvider(Context context) {
        // Usa ApplicationContext para evitar memory leaks
        this.context = context.getApplicationContext();
    }

    /**
     * Inicializa el RepositoryProvider con el contexto de la aplicación.
     * Este método debe ser llamado UNA SOLA VEZ, típicamente en el Application.onCreate()
     * o en la primera Activity que se lance.
     *
     * @param context Contexto de la aplicación
     * @throws IllegalArgumentException si el contexto es null
     */
    public static synchronized void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("El contexto no puede ser null");
        }
        if (instance == null) {
            instance = new RepositoryProvider(context);
        }
    }

    /**
     * Obtiene la instancia única del RepositoryProvider.
     * Debe ser inicializado primero con initialize().
     *
     * @return La instancia singleton del RepositoryProvider
     * @throws IllegalStateException si no ha sido inicializado previamente
     */
    public static synchronized RepositoryProvider getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "RepositoryProvider no ha sido inicializado. " +
                            "Llama a RepositoryProvider.initialize(context) primero en tu Application o MainActivity."
            );
        }
        return instance;
    }

    /**
     * Verifica si el RepositoryProvider ha sido inicializado.
     *
     * @return true si está inicializado, false en caso contrario
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    // ==================== GETTERS DE REPOSITORIOS ====================

    /**
     * Obtiene la instancia única del CategoryRepository.
     * Implementa lazy loading: crea la instancia solo cuando se solicita por primera vez.
     *
     * @return CategoryRepository para gestión de categorías
     */
    public CategoryRepository getCategoryRepository() {
        if (categoryRepository == null) {
            categoryRepository = new CategoryRepository(context);
        }
        return categoryRepository;
    }

    /**
     * Obtiene la instancia única del TransactionRepository.
     * Implementa lazy loading: crea la instancia solo cuando se solicita por primera vez.
     *
     * @return TransactionRepository para gestión de transacciones
     */
    public TransactionRepository getTransactionRepository() {
        if (transactionRepository == null) {
            transactionRepository = new TransactionRepository(context);
        }
        return transactionRepository;
    }

    /**
     * Obtiene la instancia única del CreditCardRepository.
     * Implementa lazy loading: crea la instancia solo cuando se solicita por primera vez.
     *
     * @return CreditCardRepository para gestión de tarjetas de crédito
     */
    public CreditCardRepository getCreditCardRepository() {
        if (creditCardRepository == null) {
            creditCardRepository = new CreditCardRepository(context);
        }
        return creditCardRepository;
    }

    /**
     * Obtiene la instancia única del WalletRepository.
     * Implementa lazy loading: crea la instancia solo cuando se solicita por primera vez.
     *
     * @return WalletRepository para gestión de billeteras/métodos de pago
     */
    public WalletRepository getWalletRepository() {
        if (walletRepository == null) {
            walletRepository = new WalletRepository(context);
        }
        return walletRepository;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Limpia todas las instancias de repositorios sin destruir el RepositoryProvider.
     * Los repositorios se volverán a crear en el próximo acceso (lazy loading).
     * Útil para refrescar el estado sin reiniciar la aplicación.
     */
    public void clearRepositories() {
        categoryRepository = null;
        transactionRepository = null;
        creditCardRepository = null;
        walletRepository = null;
    }

    /**
     * Destruye completamente la instancia del RepositoryProvider.
     * Solo usar en casos excepcionales como:
     * - Testing unitario
     * - Cierre completo de sesión
     * - Reinicio total de la aplicación
     *
     * Después de llamar a este método, se debe volver a inicializar con initialize().
     */
    public static synchronized void destroy() {
        if (instance != null) {
            instance.clearRepositories();
            instance = null;
        }
    }

    /**
     * Obtiene el contexto de la aplicación utilizado por el provider.
     * Útil para casos especiales donde se necesita el contexto.
     *
     * @return Context de la aplicación
     */
    public Context getContext() {
        return context;
    }
}
