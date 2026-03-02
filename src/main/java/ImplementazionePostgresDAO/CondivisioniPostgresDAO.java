package ImplementazionePostgresDAO;

import DAO.CondivisioneDAO;
import database.ConnessioneDb;
import model.Utente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione PostgreSQL di {@link CondivisioneDAO}.
 * Gestisce le condivisioni dei todo tra utenti, con controllo sull'autore.
 */
public class CondivisioniPostgresDAO implements CondivisioneDAO {

    /**
     * Restituisce la lista degli utenti con cui è condiviso un todo, ordinati per login.
     *
     * @param todoId l'id del todo di cui si vogliono le condivisioni
     * @return lista di {@link Utente} che hanno accesso al todo
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public List<Utente> listCondivisi(int todoId) throws SQLException {
        String sql = """
            SELECT u.id, u.login
            FROM todo_condivisi c
            JOIN utenti u ON u.id = c.utenti_id
            WHERE c.todo_id = ?
            ORDER BY u.login
        """;

        List<Utente> res = new ArrayList<>();

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, todoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new Utente(rs.getInt("id"), rs.getString("login")));
                }
            }
        }

        return res;
    }

    /**
     * Aggiunge un utente alla condivisione di un todo.
     * Prima verifica che {@code autoreCheOpera} sia l'autore del todo.
     * Se la condivisione esiste già, non fa nulla.
     *
     * @param todoId               l'id del todo da condividere
     * @param utenteIdDaAggiungere l'id dell'utente a cui dare accesso
     * @param autoreCheOpera       l'id dell'utente che esegue l'operazione
     * @throws SQLException      in caso di errori durante l'accesso al database
     * @throws SecurityException se {@code autoreCheOpera} non è l'autore del todo
     */
    @Override
    public void aggiungiCondivisione(int todoId, int utenteIdDaAggiungere, int autoreCheOpera) throws SQLException {
        ensureAutore(todoId, autoreCheOpera);

        String sql = """
            INSERT INTO todo_condivisi(todo_id, utenti_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
        """;

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, todoId);
            ps.setInt(2, utenteIdDaAggiungere);
            ps.executeUpdate();
        }
    }

    /**
     * Rimuove un utente dalla condivisione di un todo.
     * Prima verifica che {@code autoreCheOpera} sia l'autore del todo.
     *
     * @param todoId              l'id del todo
     * @param utenteIdDaRimuovere l'id dell'utente a cui revocare l'accesso
     * @param autoreCheOpera      l'id dell'utente che esegue l'operazione
     * @throws SQLException      in caso di errori durante l'accesso al database
     * @throws SecurityException se {@code autoreCheOpera} non è l'autore del todo
     */
    @Override
    public void rimuoviCondivisione(int todoId, int utenteIdDaRimuovere, int autoreCheOpera) throws SQLException {
        ensureAutore(todoId, autoreCheOpera);

        String sql = "DELETE FROM todo_condivisi WHERE todo_id = ? AND utenti_id = ?";

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, todoId);
            ps.setInt(2, utenteIdDaRimuovere);
            ps.executeUpdate();
        }
    }

    /**
     * Verifica che l'utente specificato sia l'autore del todo.
     * Se non lo è, lancia una {@link SecurityException}.
     * Usato come guardia prima delle operazioni di modifica delle condivisioni.
     *
     * @param todoId         l'id del todo da controllare
     * @param autoreCheOpera l'id dell'utente che vuole eseguire l'operazione
     * @throws SQLException      in caso di errori durante l'accesso al database
     * @throws SecurityException se l'utente non è l'autore del todo
     */
    private void ensureAutore(int todoId, int autoreCheOpera) throws SQLException {
        String check = "SELECT 1 FROM todo WHERE id = ? AND autore_id = ?";

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(check)) {

            ps.setInt(1, todoId);
            ps.setInt(2, autoreCheOpera);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SecurityException("Solo l'autore può gestire le condivisioni.");
                }
            }
        }
    }
}