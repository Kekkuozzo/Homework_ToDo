package DAO;

import model.Bacheca;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia DAO per la gestione delle bachece.
 * Definisce le operazioni di accesso al database relative alle bachece degli utenti.
 */
public interface BachecheDAO {

    /**
     * Restituisce tutte le bachece associate a un determinato utente.
     *
     * @param userId l'id dell'utente di cui si vogliono le bachece
     * @return lista di {@link Bacheca} appartenenti all'utente
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    List<Bacheca> findByUserId(int userId) throws SQLException;

    /**
     * Crea le bachece di default (Università, Lavoro, Tempo Libero) per un utente,
     * ma solo se non esistono già. Viene chiamata al primo accesso.
     *
     * @param userId l'id dell'utente per cui creare le bachece
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    void creaDefaultSeMancano(int userId) throws SQLException;
}