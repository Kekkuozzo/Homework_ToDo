package ImplementazionePostgresDAO;

import DAO.TodoDAO;
import database.ConnessioneDb;
import model.Todo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione PostgreSQL di {@link TodoDAO}.
 * Si occupa di tutte le operazioni CRUD sui todo, usando JDBC direttamente.
 */
public class TodoPostgresDAO implements TodoDAO {

    /**
     * Restituisce tutti i todo di una bacheca, ordinati per posizione.
     *
     * @param bachecaId l'id della bacheca
     * @return lista di {@link Todo} appartenenti alla bacheca
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public List<Todo> findByBachecaId(int bachecaId) throws SQLException {
        String sql = """
                SELECT id, autore_id, bacheca_id, titolo, descrizione,
                       immagine, url, scadenza, colore, completato, posizione
                FROM todo
                WHERE bacheca_id = ?
                ORDER BY posizione
                """;

        List<Todo> res = new ArrayList<>();

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, bachecaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
        }

        return res;
    }

    /**
     * Restituisce tutti i todo visibili da un utente in una categoria specifica.
     * Include i todo di cui è autore e quelli condivisi con lui.
     *
     * @param userId l'id dell'utente
     * @param tipo   la categoria della bacheca ("UNIVERSITA", "LAVORO", "TEMPO_LIBERO")
     * @return lista di {@link Todo} visibili dall'utente in quella categoria
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public List<Todo> findVisibiliByUserAndTipo(int userId, String tipo) throws SQLException {
        String sql = """
                SELECT t.id, t.autore_id, t.bacheca_id, t.titolo, t.descrizione,
                       t.immagine, t.url, t.scadenza, t.colore, t.completato, t.posizione
                FROM todo t
                JOIN bacheche b ON b.id = t.bacheca_id
                LEFT JOIN todo_condivisi c
                    ON c.todo_id = t.id
                    AND c.utenti_id = ?
                WHERE b.tipo = ?
                  AND (b.user_id = ? OR c.utenti_id IS NOT NULL)
                ORDER BY t.posizione
                """;

        List<Todo> res = new ArrayList<>();

        try (Connection conn = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, tipo);
            ps.setInt(3, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(map(rs));
                }
            }
        }

        return res;
    }

    /**
     * Inserisce un nuovo todo nel database e restituisce l'id generato.
     *
     * @param t il todo da creare
     * @return l'id assegnato dal database al nuovo todo
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public int crea(Todo t) throws SQLException {
        String sql = """
                INSERT INTO todo(
                    autore_id, bacheca_id, titolo, descrizione,
                    immagine, url, scadenza, colore, completato, posizione
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            fillInsert(ps, t);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Aggiorna un todo esistente nel database con i valori del todo passato.
     * L'id del todo viene usato per identificare il record da modificare.
     *
     * @param t il todo con i dati aggiornati
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public void aggiorna(Todo t) throws SQLException {
        String sql = """
                UPDATE todo
                SET bacheca_id = ?,
                    titolo      = ?,
                    descrizione = ?,
                    immagine    = ?,
                    url         = ?,
                    scadenza    = ?,
                    colore      = ?,
                    completato  = ?,
                    posizione   = ?
                WHERE id = ?
                """;

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            fillUpdate(ps, t);
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un todo dal database tramite il suo id.
     *
     * @param todoId l'id del todo da eliminare
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public void elimina(int todoId) throws SQLException {
        String sql = "DELETE FROM todo WHERE id = ?";

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, todoId);
            ps.executeUpdate();
        }
    }

    /**
     * Converte una riga del ResultSet in un oggetto {@link Todo}.
     * Gestisce il caso in cui la scadenza sia null.
     *
     * @param rs il ResultSet posizionato sulla riga da leggere
     * @return il {@link Todo} costruito con i dati della riga
     * @throws SQLException in caso di errori durante la lettura del ResultSet
     */
    private Todo map(ResultSet rs) throws SQLException {
        Date d = rs.getDate("scadenza");

        return new Todo(
                rs.getInt("id"),
                rs.getInt("autore_id"),
                rs.getInt("bacheca_id"),
                rs.getString("titolo"),
                rs.getString("descrizione"),
                rs.getString("immagine"),
                rs.getString("url"),
                d == null ? null : d.toLocalDate(),
                rs.getString("colore"),
                rs.getBoolean("completato"),
                rs.getInt("posizione")
        );
    }

    /**
     * Popola i parametri del PreparedStatement per INSERT.
     * L'ordine dei parametri corrisponde alla query in {@link #crea(Todo)}.
     *
     * @param ps il PreparedStatement da popolare
     * @param t  il todo da cui prendere i valori
     * @throws SQLException in caso di errori durante la scrittura dei parametri
     */
    private void fillInsert(PreparedStatement ps, Todo t) throws SQLException {
        ps.setInt(1, t.getAutoreId());
        ps.setInt(2, t.getBachecaId());
        ps.setString(3, t.getTitolo());
        ps.setString(4, t.getDescrizione());
        ps.setString(5, t.getImmagine());
        ps.setString(6, t.getUrl());

        if (t.getScadenza() == null) {
            ps.setNull(7, Types.DATE);
        } else {
            ps.setDate(7, Date.valueOf(t.getScadenza()));
        }

        ps.setString(8, t.getColore());
        ps.setBoolean(9, t.isCompletato());
        ps.setInt(10, t.getPosizione());
    }

    /**
     * Popola i parametri del PreparedStatement per UPDATE.
     * L'ordine dei parametri corrisponde alla query in {@link #aggiorna(Todo)}.
     *
     * @param ps il PreparedStatement da popolare
     * @param t  il todo da cui prendere i valori
     * @throws SQLException in caso di errori durante la scrittura dei parametri
     */
    private void fillUpdate(PreparedStatement ps, Todo t) throws SQLException {
        ps.setInt(1, t.getBachecaId());
        ps.setString(2, t.getTitolo());
        ps.setString(3, t.getDescrizione());
        ps.setString(4, t.getImmagine());
        ps.setString(5, t.getUrl());

        if (t.getScadenza() == null) {
            ps.setNull(6, Types.DATE);
        } else {
            ps.setDate(6, Date.valueOf(t.getScadenza()));
        }

        ps.setString(7, t.getColore());
        ps.setBoolean(8, t.isCompletato());
        ps.setInt(9, t.getPosizione());
        ps.setInt(10, t.getId());
    }
}