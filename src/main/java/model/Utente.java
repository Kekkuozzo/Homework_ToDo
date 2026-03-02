package model;

/**
 * Rappresenta un utente registrato nell'applicazione.
 * Contiene solo i dati essenziali: id e login.
 * La password non viene mai memorizzata in questo oggetto per sicurezza.
 */
public class Utente {
    private final int id;
    private final String login;

    /**
     * Costruisce un utente con id e login.
     *
     * @param id    l'id univoco dell'utente nel database
     * @param login il nome utente con cui accede all'applicazione
     */
    public Utente(int id, String login) {
        this.id = id;
        this.login = login;
    }

    /** @return l'id univoco dell'utente */
    public int getId() { return id; }

    /** @return il nome utente (login) */
    public String getLogin() { return login; }
}