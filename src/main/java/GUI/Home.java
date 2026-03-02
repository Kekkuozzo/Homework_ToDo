package GUI;

import DAO.BachecheDAO;
import DAO.CondivisioneDAO;
import DAO.TodoDAO;
import DAO.UtenteDAO;
import ImplementazionePostgresDAO.BachechePostgresDAO;
import ImplementazionePostgresDAO.CondivisioniPostgresDAO;
import ImplementazionePostgresDAO.TodoPostgresDAO;
import ImplementazionePostgresDAO.UtentePostgresDAO;
import database.ConnessioneDb;
import model.Bacheca;
import model.Todo;
import model.Utente;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Finestra principale dell'applicazione ToDo.
 * Mostra tre colonne (Università, Lavoro, Tempo Libero) con le relative bachece
 * e permette di creare, modificare, eliminare e spostare i todo.
 */
public class Home extends JFrame {

    private final Utente utente;

    private final BachecheDAO bachecheDAO = new BachechePostgresDAO();
    private final TodoDAO todoDAO = new TodoPostgresDAO();
    private final CondivisioneDAO condivisioneDAO = new CondivisioniPostgresDAO();
    private final UtenteDAO utenteDAO = new UtentePostgresDAO();

    private final JTextField searchField = new JTextField();
    private final JCheckBox onlyOverdue = new JCheckBox("Solo scaduti");
    private final JCheckBox onlyDueSoon = new JCheckBox("In scadenza (7g)");
    private final int DUE_SOON_DAYS = 7;

    private final Map<String, BoardUI> boards = new LinkedHashMap<>();
    private final Map<String, Integer> bachecaIdByTipo = new HashMap<>();

    /**
     * Costruttore della finestra principale.
     *
     * @param utente l'utente attualmente loggato
     */
    public Home(Utente utente) {
        super("ToDo - " + utente.getLogin());
        this.utente = utente;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1350, 780);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setOpaque(false);

        root.add(buildTopBar(), BorderLayout.NORTH);
        root.add(buildColumns(), BorderLayout.CENTER);

        setContentPane(root);

        SwingUtilities.invokeLater(this::reloadAll);
    }

    /**
     * Costruisce la barra superiore con il titolo, la ricerca e i filtri.
     *
     * @return il pannello della top bar
     */
    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);

        JLabel title = new JLabel("My ToDo Board");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(Color.WHITE);
        top.add(title, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        searchField.putClientProperty("JTextField.placeholderText", "Cerca ToDo...");
        searchField.setColumns(26);

        onlyOverdue.setOpaque(false);
        onlyDueSoon.setOpaque(false);
        onlyOverdue.setForeground(Color.WHITE);
        onlyDueSoon.setForeground(Color.WHITE);

        JButton refresh = new JButton("Refresh");
        refresh.setFocusable(false);

        right.add(searchField);
        right.add(onlyOverdue);
        right.add(onlyDueSoon);
        right.add(refresh);

        top.add(right, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { applyFilters(); }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });

        onlyOverdue.addActionListener(e -> applyFilters());
        onlyDueSoon.addActionListener(e -> applyFilters());
        refresh.addActionListener(e -> reloadAll());

        return top;
    }

    /**
     * Applica il filtro di ricerca testuale a tutte le colonne.
     */
    private void applyFilters() {
        String q = searchField.getText().trim().toLowerCase();
        boards.values().forEach(b -> b.setFilter(q));
    }

    /**
     * Costruisce il pannello con le tre colonne delle bachece.
     *
     * @return il pannello con le colonne
     */
    private JComponent buildColumns() {
        JPanel columns = new JPanel(new GridLayout(1, 3, 12, 0));
        columns.setOpaque(false);

        boards.put("UNIVERSITA", new BoardUI("Università", "UNIVERSITA"));
        boards.put("LAVORO", new BoardUI("Lavoro", "LAVORO"));
        boards.put("TEMPO_LIBERO", new BoardUI("Tempo Libero", "TEMPO_LIBERO"));

        for (BoardUI b : boards.values()) columns.add(b.root);
        return columns;
    }

    /**
     * Ricarica tutti i todo dal database e aggiorna l'interfaccia.
     * Viene chiamata all'avvio e dopo ogni operazione che modifica i dati.
     */
    public void reloadAll() {
        try {
            bachecheDAO.creaDefaultSeMancano(utente.getId());
            List<Bacheca> list = bachecheDAO.findByUserId(utente.getId());

            bachecaIdByTipo.clear();
            for (Bacheca b : list) bachecaIdByTipo.put(b.getTipo(), b.getId());

            for (Map.Entry<String, BoardUI> entry : boards.entrySet()) {
                String tipo = entry.getKey();
                BoardUI ui = entry.getValue();

                Integer bid = bachecaIdByTipo.get(tipo);
                ui.bachecaId = (bid == null) ? -1 : bid;

                List<Todo> todos = todoDAO.findVisibiliByUserAndTipo(utente.getId(), tipo);
                ui.setTodos(todos);
            }

            applyFilters();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Rappresenta visivamente una singola colonna/bacheca (es. "Università").
     * Gestisce la lista interna dei todo e il loro rendering.
     */
    private class BoardUI {
        final String titolo;
        final String tipo;

        int bachecaId = -1;

        final JPanel root = new JPanel(new BorderLayout(8, 8));
        final JLabel countLabel = new JLabel("0");

        final JPanel listPanel = new JPanel();
        final java.util.List<Todo> allTodos = new ArrayList<>();
        String filter = "";

        /**
         * Crea la colonna con intestazione, lista scrollabile e bottone di aggiunta.
         *
         * @param titolo il nome da mostrare nella UI
         * @param tipo   il tipo corrispondente nel DB (es. "UNIVERSITA")
         */
        BoardUI(String titolo, String tipo) {
            this.titolo = titolo;
            this.tipo = tipo;

            root.setOpaque(true);
            root.setBackground(new Color(28, 28, 30));
            root.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(80, 80, 80)),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));

            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setOpaque(false);

            JLabel titleLabel = new JLabel(titolo);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            titleLabel.setForeground(Color.WHITE);
            header.add(titleLabel, BorderLayout.WEST);

            countLabel.setForeground(new Color(160, 160, 160));
            header.add(countLabel, BorderLayout.CENTER);

            JButton addBtn = new JButton("+");
            addBtn.setFocusable(false);
            addBtn.setMargin(new Insets(4, 10, 4, 10));
            header.add(addBtn, BorderLayout.EAST);

            root.add(header, BorderLayout.NORTH);

            listPanel.setOpaque(false);
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

            JScrollPane scroll = new JScrollPane(listPanel);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setOpaque(false);
            scroll.setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(14);

            root.add(scroll, BorderLayout.CENTER);

            addBtn.addActionListener(e -> openTodoDialog(this, null));
            refresh();
        }

        /**
         * Sostituisce la lista di todo corrente con quella fornita e ridisegna la colonna.
         *
         * @param todos la nuova lista di todo da visualizzare
         */
        void setTodos(List<Todo> todos) {
            allTodos.clear();
            if (todos != null) allTodos.addAll(todos);
            refresh();
        }

        /**
         * Aggiorna il filtro di ricerca testuale e ridisegna la colonna.
         *
         * @param q la stringa di ricerca (case-insensitive)
         */
        void setFilter(String q) {
            filter = q == null ? "" : q;
            refresh();
        }

        /**
         * Controlla se un todo soddisfa i filtri attivi (testo, scadenza, ecc.).
         *
         * @param t il todo da valutare
         * @return true se il todo deve essere mostrato, false altrimenti
         */
        boolean matches(Todo t) {
            if (!filter.isEmpty()) {
                String ti = t.getTitolo() == null ? "" : t.getTitolo().toLowerCase();
                String de = t.getDescrizione() == null ? "" : t.getDescrizione().toLowerCase();
                if (!(ti.contains(filter) || de.contains(filter))) return false;
            }

            LocalDate due = t.getScadenza();
            LocalDate today = LocalDate.now();

            if (onlyOverdue.isSelected()) {
                if (due == null) return false;
                if (!due.isBefore(today)) return false;
                if (t.isCompletato()) return false;
            }

            if (onlyDueSoon.isSelected()) {
                if (due == null) return false;
                if (t.isCompletato()) return false;
                LocalDate limit = today.plusDays(DUE_SOON_DAYS);
                if (due.isBefore(today) || due.isAfter(limit)) return false;
            }

            return true;
        }

        /**
         * Ridisegna la lista dei todo visibili applicando i filtri correnti.
         * Aggiorna anche il contatore nell'intestazione della colonna.
         */
        void refresh() {
            listPanel.removeAll();
            int visible = 0;

            for (Todo t : allTodos) {
                if (!matches(t)) continue;
                visible++;
                listPanel.add(new TodoCardUI(this, t).root);
                listPanel.add(Box.createVerticalStrut(10));
            }

            countLabel.setText(String.valueOf(visible));
            listPanel.revalidate();
            listPanel.repaint();
        }
    }

    /**
     * Rappresenta visivamente una singola card di un todo all'interno di una colonna.
     * Contiene titolo, descrizione, scadenza e tutti i bottoni di azione.
     */
    private class TodoCardUI {
        final BoardUI owner;
        final Todo todo;

        final JPanel root = new JPanel(new BorderLayout(8, 6));

        /**
         * Costruisce la card per il todo specificato.
         *
         * @param owner la colonna a cui appartiene questa card
         * @param todo  il todo da visualizzare
         */
        TodoCardUI(BoardUI owner, Todo todo) {
            this.owner = owner;
            this.todo = todo;

            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            root.setOpaque(true);

            Color cardColor = parseHex(todo.getColore());
            if (cardColor != null) root.setBackground(cardColor.darker());
            else root.setBackground(new Color(40, 40, 44));

            if (todo.isCompletato()) {
                root.setBackground(new Color(45, 45, 45));
            }

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);

            JLabel title = new JLabel(todo.getTitolo() == null ? "(senza titolo)" : todo.getTitolo());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            title.setForeground(Color.WHITE);

            if (todo.isCompletato()) {
                title.setForeground(new Color(170, 170, 170));
                title.setText("✅ " + title.getText());
            }

            if (!todo.isCompletato() && todo.getScadenza() != null && todo.getScadenza().isBefore(LocalDate.now())) {
                title.setForeground(new Color(220, 80, 80));
            }

            top.add(title, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);

            JButton up = smallBtn("↑");
            JButton down = smallBtn("↓");
            JButton edit = smallBtn("✎");
            JButton link = smallBtn("🔗");
            JButton img = smallBtn("🖼");
            JButton share = smallBtn("👥");
            JButton move = smallBtn("↪");
            JButton del = smallBtn("✕");

            link.setEnabled(todo.getUrl() != null && !todo.getUrl().isBlank());
            img.setEnabled(todo.getImmagine() != null && !todo.getImmagine().isBlank());

            actions.add(up);
            actions.add(down);
            actions.add(edit);
            actions.add(link);
            actions.add(img);
            actions.add(share);
            actions.add(move);
            actions.add(del);

            top.add(actions, BorderLayout.EAST);
            root.add(top, BorderLayout.NORTH);

            if (todo.getDescrizione() != null && !todo.getDescrizione().isBlank()) {
                JLabel desc = new JLabel("<html><body style='width:280px'>" + escape(todo.getDescrizione()) + "</body></html>");
                desc.setForeground(todo.isCompletato() ? new Color(160, 160, 160) : new Color(210, 210, 210));
                root.add(desc, BorderLayout.CENTER);
            }

            JPanel footer = new JPanel(new BorderLayout());
            footer.setOpaque(false);

            if (todo.getScadenza() != null) {
                JLabel due = new JLabel("Scadenza: " + todo.getScadenza());
                due.setForeground(new Color(170, 170, 170));
                footer.add(due, BorderLayout.WEST);
            }

            if (todo.getAutoreId() != utente.getId()) {
                JLabel shared = new JLabel("Condiviso");
                shared.setForeground(new Color(160, 160, 160));
                footer.add(shared, BorderLayout.EAST);
            }

            root.add(footer, BorderLayout.SOUTH);

            up.addActionListener(e -> moveRelative(-1));
            down.addActionListener(e -> moveRelative(1));
            edit.addActionListener(e -> openTodoDialog(owner, todo));
            link.addActionListener(e -> openLink(todo.getUrl()));
            img.addActionListener(e -> openImage(todo.getImmagine()));
            share.addActionListener(e -> openShareDialog(todo));
            del.addActionListener(e -> deleteTodo());

            JPopupMenu moveMenu = new JPopupMenu();
            move.addActionListener(e -> {
                moveMenu.removeAll();
                for (String tipo : boards.keySet()) {
                    if (tipo.equals(owner.tipo)) continue;
                    JMenuItem it = new JMenuItem("Sposta in " + tipo);
                    it.addActionListener(ev -> moveTodoTo(tipo));
                    moveMenu.add(it);
                }
                moveMenu.show(move, 0, move.getHeight());
            });
        }

        /**
         * Crea un bottone piccolo con testo/emoji, usato per le azioni sulla card.
         *
         * @param text il testo (o emoji) del bottone
         * @return il bottone configurato
         */
        private JButton smallBtn(String text) {
            JButton b = new JButton(text);
            b.setFocusable(false);
            b.setMargin(new Insets(2, 8, 2, 8));
            return b;
        }

        /**
         * Chiede conferma e poi elimina questo todo dal database.
         */
        private void deleteTodo() {
            int choice = JOptionPane.showConfirmDialog(
                    Home.this,
                    "Eliminare questo ToDo?",
                    "Conferma",
                    JOptionPane.YES_NO_OPTION
            );
            if (choice != JOptionPane.YES_OPTION) return;

            try {
                todoDAO.elimina(todo.getId());
                reloadAll();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(Home.this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

        /**
         * Sposta questo todo in un'altra bacheca, assegnandogli l'ultima posizione disponibile.
         *
         * @param targetTipo il tipo della bacheca di destinazione (es. "LAVORO")
         */
        private void moveTodoTo(String targetTipo) {
            Integer targetBachecaId = bachecaIdByTipo.get(targetTipo);
            if (targetBachecaId == null) return;

            try {
                int newPos = nextPos(targetBachecaId);

                Todo updated = new Todo(
                        todo.getId(),
                        todo.getAutoreId(),
                        targetBachecaId,
                        todo.getTitolo(),
                        todo.getDescrizione(),
                        todo.getImmagine(),
                        todo.getUrl(),
                        todo.getScadenza(),
                        todo.getColore(),
                        todo.isCompletato(),
                        newPos
                );

                todoDAO.aggiorna(updated);
                reloadAll();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(Home.this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

        /**
         * Sposta il todo su o giù di una posizione nella stessa colonna.
         * Aggiorna le posizioni nel database e ridisegna la colonna senza ricaricare tutto.
         *
         * @param delta -1 per salire, +1 per scendere
         */
        private void moveRelative(int delta) {
            try {
                List<Todo> list = owner.allTodos;
                int idx = list.indexOf(todo);
                if (idx == -1) return;

                int newIdx = idx + delta;
                if (newIdx < 0 || newIdx >= list.size()) return;

                Todo other = list.get(newIdx);

                swapPositions(todo.getId(), todo.getPosizione(), other.getId(), other.getPosizione());

                Collections.swap(list, idx, newIdx);

                owner.refresh();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(Home.this, "Errore spostamento: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

        /**
         * Esegue l'escape dei caratteri HTML speciali per evitare problemi nel rendering della JLabel.
         *
         * @param s la stringa da sanificare
         * @return la stringa con i caratteri speciali sostituiti dalle entità HTML
         */
        private String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }
    }

    /**
     * Apre il dialog per creare un nuovo todo o modificarne uno esistente.
     * Se {@code existing} è null, crea un nuovo todo nella bacheca specificata.
     *
     * @param board    la colonna in cui creare/modificare il todo
     * @param existing il todo da modificare, oppure null se si sta creando
     */
    private void openTodoDialog(BoardUI board, Todo existing) {
        if (board.bachecaId <= 0) {
            JOptionPane.showMessageDialog(this, "Bacheca non valida (id). Fai Refresh.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isEdit = (existing != null);

        JTextField titolo = new JTextField(isEdit ? safe(existing.getTitolo()) : "");
        JTextArea descr = new JTextArea(isEdit ? safe(existing.getDescrizione()) : "", 4, 24);
        descr.setLineWrap(true);
        descr.setWrapStyleWord(true);

        JTextField scadenza = new JTextField(isEdit && existing.getScadenza() != null ? existing.getScadenza().toString() : "");
        scadenza.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD (opzionale)");

        JTextField url = new JTextField(isEdit ? safe(existing.getUrl()) : "");
        url.putClientProperty("JTextField.placeholderText", "https://... (opzionale)");

        final String[] selectedImg = new String[]{ isEdit ? safe(existing.getImmagine()) : "" };

        JTextField imgPath = new JTextField(selectedImg[0]);
        JButton browse = new JButton("Browse...");
        browse.setFocusable(false);

        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (!imgPath.getText().trim().isEmpty()) {
                File f = new File(imgPath.getText().trim());
                if (f.getParentFile() != null && f.getParentFile().exists()) fc.setCurrentDirectory(f.getParentFile());
            }
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                selectedImg[0] = fc.getSelectedFile().getAbsolutePath();
                imgPath.setText(selectedImg[0]);
            }
        });

        JPanel imgRow = new JPanel(new BorderLayout(8, 0));
        imgRow.add(imgPath, BorderLayout.CENTER);
        imgRow.add(browse, BorderLayout.EAST);

        JCheckBox completato = new JCheckBox("Completato", isEdit && existing.isCompletato());

        final Color[] selectedColor = new Color[]{ isEdit ? parseHex(existing.getColore()) : null };

        JPanel colorRow = new JPanel(new BorderLayout(8, 0));
        colorRow.setOpaque(false);

        JPanel preview = new JPanel();
        preview.setPreferredSize(new Dimension(36, 18));
        preview.setBackground(selectedColor[0] != null ? selectedColor[0] : new Color(60, 60, 60));
        preview.setBorder(BorderFactory.createLineBorder(new Color(120, 120, 120)));

        JButton pick = new JButton("Scegli...");
        pick.setFocusable(false);

        JButton clear = new JButton("Reset");
        clear.setFocusable(false);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(pick);
        rightBtns.add(clear);

        colorRow.add(preview, BorderLayout.WEST);
        colorRow.add(rightBtns, BorderLayout.CENTER);

        pick.addActionListener(ev -> {
            Color c = JColorChooser.showDialog(this, "Scegli colore", selectedColor[0] != null ? selectedColor[0] : Color.WHITE);
            if (c != null) {
                selectedColor[0] = c;
                preview.setBackground(c);
            }
        });

        clear.addActionListener(ev -> {
            selectedColor[0] = null;
            preview.setBackground(new Color(60, 60, 60));
        });

        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.add(new JLabel("Titolo"));
        form.add(titolo);
        form.add(new JLabel("Descrizione"));
        form.add(new JScrollPane(descr));
        form.add(new JLabel("Scadenza"));
        form.add(scadenza);
        form.add(new JLabel("Link URL"));
        form.add(url);
        form.add(new JLabel("Immagine (path)"));
        form.add(imgRow);
        form.add(new JLabel("Colore"));
        form.add(colorRow);
        form.add(completato);

        int res = JOptionPane.showConfirmDialog(
                this,
                form,
                (isEdit ? "Modifica ToDo" : "Crea ToDo") + " (" + board.titolo + ")",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (res != JOptionPane.OK_OPTION) return;

        String t = titolo.getText().trim();
        if (t.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Il titolo non può essere vuoto.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocalDate due = null;
        String dueTxt = scadenza.getText().trim();
        if (!dueTxt.isEmpty()) {
            try {
                due = LocalDate.parse(dueTxt);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Formato scadenza non valido. Usa YYYY-MM-DD.", "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String link = url.getText().trim();
        if (link.isEmpty()) link = null;

        String img = imgPath.getText().trim();
        if (img.isEmpty()) img = null;

        String col = null;
        if (selectedColor[0] != null) {
            col = String.format("#%02X%02X%02X", selectedColor[0].getRed(), selectedColor[0].getGreen(), selectedColor[0].getBlue());
        }

        try {
            if (!isEdit) {
                int pos = nextPos(board.bachecaId);

                Todo newTodo = new Todo(
                        0,
                        utente.getId(),
                        board.bachecaId,
                        t,
                        descr.getText(),
                        img,
                        link,
                        due,
                        col,
                        completato.isSelected(),
                        pos
                );

                todoDAO.crea(newTodo);
            } else {
                Todo updated = new Todo(
                        existing.getId(),
                        existing.getAutoreId(),
                        existing.getBachecaId(),
                        t,
                        descr.getText(),
                        img,
                        link,
                        due,
                        col,
                        completato.isSelected(),
                        existing.getPosizione()
                );

                todoDAO.aggiorna(updated);
            }

            reloadAll();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Apre il dialog per gestire le condivisioni di un todo.
     * Solo l'autore può aggiungere o rimuovere utenti.
     *
     * @param todo il todo di cui gestire le condivisioni
     */
    private void openShareDialog(Todo todo) {
        boolean isAuthor = (todo.getAutoreId() == utente.getId());

        DefaultListModel<Utente> listModel = new DefaultListModel<>();
        JList<Utente> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTextField loginField = new JTextField();
        loginField.putClientProperty("JTextField.placeholderText", "login utente da aggiungere");

        JButton addBtn = new JButton("Aggiungi");
        JButton removeBtn = new JButton("Rimuovi selezionato");
        addBtn.setFocusable(false);
        removeBtn.setFocusable(false);

        loginField.setEnabled(isAuthor);
        addBtn.setEnabled(isAuthor);
        removeBtn.setEnabled(isAuthor);

        JLabel info = new JLabel(isAuthor ? "Gestisci condivisioni" : "Solo l'autore può modificare le condivisioni");
        info.setForeground(isAuthor ? new Color(170, 170, 170) : new Color(220, 80, 80));

        Runnable reload = () -> {
            try {
                listModel.clear();
                List<Utente> condivisi = condivisioneDAO.listCondivisi(todo.getId());
                for (Utente u : condivisi) listModel.addElement(u);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        };

        addBtn.addActionListener(e -> {
            String login = loginField.getText().trim();
            if (login.isEmpty()) return;

            try {
                Optional<Utente> target = utenteDAO.findByLogin(login);
                if (target.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Utente non trovato: " + login, "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (target.get().getId() == utente.getId()) {
                    JOptionPane.showMessageDialog(this, "Non puoi condividere con te stesso.", "Info", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                condivisioneDAO.aggiungiCondivisione(todo.getId(), target.get().getId(), utente.getId());
                loginField.setText("");
                reload.run();
            } catch (SecurityException se) {
                JOptionPane.showMessageDialog(this, se.getMessage(), "Permessi", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        removeBtn.addActionListener(e -> {
            Utente sel = list.getSelectedValue();
            if (sel == null) return;

            try {
                condivisioneDAO.rimuoviCondivisione(todo.getId(), sel.getId(), utente.getId());
                reload.run();
            } catch (SecurityException se) {
                JOptionPane.showMessageDialog(this, se.getMessage(), "Permessi", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Errore DB: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(info, BorderLayout.NORTH);

        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.add(loginField, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);
        top.add(addRow, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        panel.add(removeBtn, BorderLayout.SOUTH);

        reload.run();

        JOptionPane.showMessageDialog(this, panel, "Condivisioni", JOptionPane.PLAIN_MESSAGE);

        reloadAll();
    }

    /**
     * Apre il browser di sistema all'URL associato al todo.
     *
     * @param url l'URL da aprire
     */
    private void openLink(String url) {
        if (url == null || url.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nessun link.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(url.trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Link non valido: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Mostra l'immagine associata al todo in una finestra di dialogo con scroll.
     *
     * @param path il percorso assoluto dell'immagine sul filesystem
     */
    private void openImage(String path) {
        if (path == null || path.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nessuna immagine.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            ImageIcon icon = new ImageIcon(path.trim());
            if (icon.getIconWidth() <= 0) throw new RuntimeException("Immagine non caricabile");

            JLabel label = new JLabel(icon);
            JScrollPane sp = new JScrollPane(label);
            sp.setPreferredSize(new Dimension(700, 450));

            JOptionPane.showMessageDialog(this, sp, "Immagine", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Errore immagine: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Calcola la prossima posizione disponibile in una bacheca (MAX + 1).
     *
     * @param bachecaId l'id della bacheca
     * @return la prossima posizione intera disponibile
     * @throws SQLException in caso di errori DB
     */
    private int nextPos(int bachecaId) throws SQLException {
        try (var c = ConnessioneDb.getInstance().getConnection();
             var ps = c.prepareStatement("SELECT COALESCE(MAX(posizione),0)+1 FROM todo WHERE bacheca_id=?")) {
            ps.setInt(1, bachecaId);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Scambia le posizioni di due todo nel database usando un valore temporaneo
     * per evitare conflitti di unicità.
     *
     * @param todoIdA id del primo todo
     * @param posA    posizione attuale del primo todo
     * @param todoIdB id del secondo todo
     * @param posB    posizione attuale del secondo todo
     * @throws SQLException in caso di errori DB
     */
    private void swapPositions(int todoIdA, int posA, int todoIdB, int posB) throws SQLException {
        try (var c = ConnessioneDb.getInstance().getConnection()) {
            try (var ps1 = c.prepareStatement("UPDATE todo SET posizione = -1 WHERE id = ?")) {
                ps1.setInt(1, todoIdA);
                ps1.executeUpdate();
            }
            try (var ps2 = c.prepareStatement("UPDATE todo SET posizione = ? WHERE id = ?")) {
                ps2.setInt(1, posA);
                ps2.setInt(2, todoIdB);
                ps2.executeUpdate();
            }
            try (var ps3 = c.prepareStatement("UPDATE todo SET posizione = ? WHERE id = ?")) {
                ps3.setInt(1, posB);
                ps3.setInt(2, todoIdA);
                ps3.executeUpdate();
            }
        }
    }

    /**
     * Converte una stringa esadecimale nel formato "#RRGGBB" in un oggetto Color.
     * Ritorna null se la stringa non è valida.
     *
     * @param s la stringa colore da convertire
     * @return il Color corrispondente, oppure null se non parsabile
     */
    private Color parseHex(String s) {
        if (s == null) return null;
        s = s.trim();
        if (!s.startsWith("#") || s.length() != 7) return null;
        try {
            int r = Integer.parseInt(s.substring(1, 3), 16);
            int g = Integer.parseInt(s.substring(3, 5), 16);
            int b = Integer.parseInt(s.substring(5, 7), 16);
            return new Color(r, g, b);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Restituisce la stringa passata, oppure una stringa vuota se è null.
     * Utile per popolare i campi del form senza NullPointerException.
     *
     * @param s la stringa da controllare
     * @return la stringa originale, oppure "" se null
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }
}