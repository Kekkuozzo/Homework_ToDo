package ImplementazionePostgresDAO;

import DAO.UtenteDAO;
import database.ConnessioneDb;
import model.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Implementazione PostgreSQL di {@link UtenteDAO}.
 * Gestisce autenticazione e ricerca degli utenti nel database.
 */
public class UtentePostgresDAO implements UtenteDAO {

    /**
     * Cerca un utente nel database tramite il suo login.
     *
     * @param login il nome utente da cercare
     * @return un {@link Optional} con l'utente trovato, oppure vuoto se non esiste
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public Optional<Utente> findByLogin(String login) throws SQLException {
        String sql = "SELECT id, login FROM utenti WHERE login = ?";
        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new Utente(rs.getInt("id"), rs.getString("login")));
                return Optional.empty();
            }
        }
    }

    /**
     * Verifica le credenziali di un utente confrontando login e password nel database.
     * La password viene confrontata in chiaro, quindi si assume che sia già nel formato
     * corretto (eventualmente hashata prima della chiamata).
     *
     * @param login    il nome utente
     * @param password la password da verificare
     * @return un {@link Optional} con l'utente se le credenziali sono corrette, altrimenti vuoto
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public Optional<Utente> login(String login, String password) throws SQLException {
        String sql = "SELECT id, login FROM utenti WHERE login = ? AND password = ?";

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, login);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Utente(rs.getInt("id"), rs.getString("login")));
                }
                return Optional.empty();
            }
        }
    }
}