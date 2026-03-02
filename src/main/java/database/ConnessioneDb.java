package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestisce la connessione al database PostgreSQL tramite il pattern Singleton.
 * In questo modo esiste una sola istanza della connessione per tutta l'applicazione,
 * e viene ricreata automaticamente se risulta chiusa o nulla.
 */
public class ConnessioneDb {

    private static ConnessioneDb instance;

    /** La connessione JDBC attiva. */
    public Connection connection = null;

    private String user = "todo_app";
    private String password = "mimmo";
    private String url = "jdbc:postgresql://localhost:5432/todo_db";

    /**
     * Costruttore privato: apre la connessione al database.
     * Non può essere chiamato dall'esterno, si usa {@link #getInstance()}.
     *
     * @throws SQLException se la connessione al database fallisce
     */
    private ConnessioneDb() throws SQLException {
        this.connection = DriverManager.getConnection(url, user, password);
    }

    /**
     * Restituisce l'istanza unica di ConnessioneDb.
     * Se l'istanza non esiste ancora, o la connessione è chiusa, ne crea una nuova.
     * Il metodo è sincronizzato per evitare problemi in caso di accesso da più thread.
     *
     * @return l'istanza singleton di ConnessioneDb
     * @throws SQLException se la connessione al database fallisce
     */
    public static synchronized ConnessioneDb getInstance() throws SQLException {
        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
            instance = new ConnessioneDb();
        }
        return instance;
    }

    /**
     * Restituisce la connessione JDBC attiva.
     *
     * @return la {@link Connection} corrente
     */
    public Connection getConnection() {
        return connection;
    }
}