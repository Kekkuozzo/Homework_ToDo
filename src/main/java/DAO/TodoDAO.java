package DAO;

import model.Todo;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia DAO per la gestione dei todo.
 * Copre le operazioni CRUD e le query necessarie alla visualizzazione nella home.
 */
public interface TodoDAO {

    /**
     * Restituisce tutti i todo presenti in una specifica bacheca, ordinati per posizione.
     *
     * @param bachecaId l'id della bacheca di cui si vogliono i todo
     * @return lista di {@link Todo} appartenenti alla bacheca
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    List<Todo> findByBachecaId(int bachecaId) throws SQLException;

    /**
     * Restituisce tutti i todo visibili da un utente in una determinata categoria.
     * Include sia i todo di cui l'utente è autore, sia quelli condivisi con lui.
     *
     * @param userId l'id dell'utente
     * @param tipo   il tipo di bacheca (es. "UNIVERSITA", "LAVORO", "TEMPO_LIBERO")
     * @return lista di {@link Todo} visibili dall'utente in quella categoria
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    List<Todo> findVisibiliByUserAndTipo(int userId, String tipo) throws SQLException;

    /**
     * Inserisce un nuovo todo nel database.
     *
     * @param t il todo da creare (l'id può essere 0, viene assegnato dal DB)
     * @return l'id generato dal database per il nuovo todo
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    int crea(Todo t) throws SQLException;

    /**
     * Aggiorna un todo esistente nel database con i nuovi valori forniti.
     *
     * @param t il todo con i dati aggiornati (l'id deve corrispondere a un record esistente)
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    void aggiorna(Todo t) throws SQLException;

    /**
     * Elimina un todo dal database tramite il suo id.
     *
     * @param todoId l'id del todo da eliminare
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    void elimina(int todoId) throws SQLException;
}