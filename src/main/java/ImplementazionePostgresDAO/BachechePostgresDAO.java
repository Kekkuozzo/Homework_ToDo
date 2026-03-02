package ImplementazionePostgresDAO;

import DAO.BachecheDAO;
import database.ConnessioneDb;
import model.Bacheca;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementazione PostgreSQL di {@link BachecheDAO}.
 * Gestisce il recupero e la creazione delle bachece nel database.
 */
public class BachechePostgresDAO implements BachecheDAO {

    /**
     * Restituisce tutte le bachece associate a un utente, ordinate per id.
     *
     * @param userId l'id dell'utente di cui si vogliono le bachece
     * @return lista di {@link Bacheca} appartenenti all'utente
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public List<Bacheca> findByUserId(int userId) throws SQLException {
        String sql = "SELECT id, user_id, tipo, titolo, descrizione FROM bacheche WHERE user_id = ? ORDER BY id";
        List<Bacheca> res = new ArrayList<>();

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new Bacheca(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("tipo"),
                            rs.getString("titolo"),
                            rs.getString("descrizione")
                    ));
                }
            }
        }
        return res;
    }

    /**
     * Crea le tre bachece di default (Università, Lavoro, Tempo Libero) per un utente.
     * Se una bacheca esiste già per quell'utente, viene ignorata grazie alla clausola
     * {@code ON CONFLICT DO NOTHING}.
     *
     * @param userId l'id dell'utente per cui creare le bachece
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    @Override
    public void creaDefaultSeMancano(int userId) throws SQLException {
        String sql = """
            INSERT INTO bacheche(user_id, tipo, titolo, descrizione)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id, tipo) DO NOTHING
        """;

        try (Connection c = ConnessioneDb.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            insert(ps, userId, "UNIVERSITA", "Università", "Cose da fare");
            insert(ps, userId, "LAVORO", "Lavoro", "Cose da fare");
            insert(ps, userId, "TEMPO_LIBERO", "Tempo Libero", "Cose da fare");
        }
    }

    /**
     * Esegue l'inserimento di una singola bacheca riutilizzando il PreparedStatement.
     *
     * @param ps          il PreparedStatement già preparato per la INSERT
     * @param userId      l'id dell'utente proprietario
     * @param tipo        il tipo della bacheca (es. "UNIVERSITA")
     * @param titolo      il titolo da mostrare nella UI
     * @param descrizione una breve descrizione della bacheca
     * @throws SQLException in caso di errori durante l'accesso al database
     */
    private void insert(PreparedStatement ps, int userId, String tipo, String titolo, String descrizione) throws SQLException {
        ps.setInt(1, userId);
        ps.setString(2, tipo);
        ps.setString(3, titolo);
        ps.setString(4, descrizione);
        ps.executeUpdate();
    }
}