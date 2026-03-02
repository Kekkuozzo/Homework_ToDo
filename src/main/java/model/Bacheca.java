package model;

/**
 * Rappresenta una bacheca dell'applicazione ToDo.
 * Ogni utente ha tre bachece di default: Università, Lavoro e Tempo Libero.
 * Una bacheca contiene una lista di todo ed è identificata da un tipo univoco per utente.
 */
public class Bacheca {
    private final int id;
    private final int userId;
    private final String tipo;
    private final String titolo;
    private final String descrizione;

    /**
     * Costruisce una bacheca con tutti i campi.
     *
     * @param id          l'id univoco della bacheca nel database
     * @param userId      l'id dell'utente proprietario
     * @param tipo        il tipo della bacheca (es. "UNIVERSITA", "LAVORO", "TEMPO_LIBERO")
     * @param titolo      il nome leggibile della bacheca mostrato nella UI
     * @param descrizione una breve descrizione del contenuto della bacheca
     */
    public Bacheca(int id, int userId, String tipo, String titolo, String descrizione) {
        this.id = id;
        this.userId = userId;
        this.tipo = tipo;
        this.titolo = titolo;
        this.descrizione = descrizione;
    }

    /**
     * @return l'id univoco della bacheca
     */
    public int getId() { return id; }

    /**
     * @return la descrizione della bacheca
     */
    public String getDescrizione() { return descrizione; }

    /**
     * @return il titolo della bacheca
     */
    public String getTitolo() { return titolo; }

    /**
     * @return il tipo della bacheca (es. "LAVORO")
     */
    public String getTipo() { return tipo; }

    /**
     * @return l'id dell'utente proprietario
     */
    public int getUserId() { return userId; }
}