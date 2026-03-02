package DAO;

import model.Utente;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Interfaccia DAO per la gestione degli utenti.
 * Si occupa dell'autenticazione e della ricerca degli utenti nel database.
 */
public interface UtenteDAO {

    /**
     * Verifica le credenziali di un utente e lo restituisce se il login ha successo.
     *
     * @param login    il nome utente inserito
     * @param password la password inserita (in chiaro o hash, dipende dall'implementazione)
     * @return un {@link Optional} contenente l'utente se le credenziali sono corrette,
     *         oppure {@link Optional#empty()} se non trovato o password errata
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    Optional<Utente> login(String login, String password) throws SQLException;

    /**
     * Cerca un utente nel database tramite il suo nome di login.
     * Usato principalmente per la gestione delle condivisioni.
     *
     * @param login il nome utente da cercare
     * @return un {@link Optional} contenente l'utente se trovato,
     *         oppure {@link Optional#empty()} se non esiste
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    Optional<Utente> findByLogin(String login) throws SQLException;
}