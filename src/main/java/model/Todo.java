package model;

import java.time.LocalDate;

/**
 * Rappresenta un singolo todo nell'applicazione.
 * Un todo appartiene a una bacheca, ha un autore, e può essere condiviso con altri utenti.
 * È immutabile: tutti i campi vengono impostati nel costruttore.
 */
public class Todo {
    private final int id;
    private final int autoreId;
    private final int bachecaId;
    private final String titolo;
    private final String descrizione;
    private final String immagine;
    private final String url;
    private final LocalDate scadenza;
    private final String colore;
    private final boolean completato;
    private final int posizione;

    /**
     * Costruisce un todo con tutti i campi.
     *
     * @param id          l'id univoco del todo nel database (0 se non ancora salvato)
     * @param autoreId    l'id dell'utente che ha creato il todo
     * @param bachecaId   l'id della bacheca a cui appartiene il todo
     * @param titolo      il titolo del todo
     * @param descrizione una descrizione opzionale del todo
     * @param immagine    il percorso assoluto di un'immagine opzionale sul filesystem
     * @param url         un link opzionale associato al todo
     * @param scadenza    la data di scadenza opzionale del todo
     * @param colore      il colore della card in formato "#RRGGBB", oppure null
     * @param completato  true se il todo è stato completato
     * @param posizione   la posizione del todo all'interno della bacheca (usata per l'ordinamento)
     */
    public Todo(int id, int autoreId, int bachecaId, String titolo, String descrizione, String immagine,
                String url, LocalDate scadenza, String colore, boolean completato, int posizione) {
        this.id = id;
        this.autoreId = autoreId;
        this.bachecaId = bachecaId;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.immagine = immagine;
        this.url = url;
        this.scadenza = scadenza;
        this.colore = colore;
        this.completato = completato;
        this.posizione = posizione;
    }

    /** @return l'id univoco del todo */
    public int getId() { return id; }

    /** @return l'id dell'autore del todo */
    public int getAutoreId() { return autoreId; }

    /** @return l'id della bacheca a cui appartiene il todo */
    public int getBachecaId() { return bachecaId; }

    /** @return il titolo del todo */
    public String getTitolo() { return titolo; }

    /** @return la descrizione del todo, può essere null */
    public String getDescrizione() { return descrizione; }

    /** @return il percorso dell'immagine associata, può essere null */
    public String getImmagine() { return immagine; }

    /** @return l'URL associato al todo, può essere null */
    public String getUrl() { return url; }

    /** @return la data di scadenza, può essere null se non impostata */
    public LocalDate getScadenza() { return scadenza; }

    /** @return il colore della card in formato "#RRGGBB", può essere null */
    public String getColore() { return colore; }

    /** @return true se il todo è stato segnato come completato */
    public boolean isCompletato() { return completato; }

    /** @return la posizione del todo nella bacheca, usata per l'ordinamento */
    public int getPosizione() { return posizione; }
}