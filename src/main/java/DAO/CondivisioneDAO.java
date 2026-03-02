package DAO;

import model.Utente;

import java.sql.SQLException;
import java.util.List;

/**
 * Interfaccia DAO per la gestione delle condivisioni dei todo tra utenti.
 * Permette di sapere con chi è condiviso un todo, e di aggiungere o rimuovere utenti.
 */
public interface CondivisioneDAO {

    /**
     * Restituisce la lista degli utenti con cui è condiviso un determinato todo.
     *
     * @param todoId l'id del todo di cui si vogliono le condivisioni
     * @return lista di {@link Utente} che possono vedere il todo
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    List<Utente> listCondivisi(int todoId) throws SQLException;

    /**
     * Aggiunge un utente alla lista di condivisione di un todo.
     * Solo l'autore del todo può eseguire questa operazione.
     *
     * @param todoId              l'id del todo da condividere
     * @param utenteIdDaAggiungere l'id dell'utente a cui dare accesso
     * @param autoreCheOpera      l'id dell'utente che sta eseguendo l'operazione (deve essere l'autore)
     * @throws SQLException      in caso di errori durante l'accesso al database
     * @throws SecurityException se {@code autoreCheOpera} non è l'autore del todo
     */
    void aggiungiCondivisione(int todoId, int utenteIdDaAggiungere, int autoreCheOpera) throws SQLException;

    /**
     * Rimuove un utente dalla lista di condivisione di un todo.
     * Solo l'autore del todo può eseguire questa operazione.
     *
     * @param todoId             l'id del todo
     * @param utenteIdDaRimuovere l'id dell'utente a cui revocare l'accesso
     * @param autoreCheOpera     l'id dell'utente che sta eseguendo l'operazione (deve essere l'autore)
     * @throws SQLException      in caso di errori durante l'accesso al database
     * @throws SecurityException se {@code autoreCheOpera} non è l'autore del todo
     */
    void rimuoviCondivisione(int todoId, int utenteIdDaRimuovere, int autoreCheOpera) throws SQLException;
}