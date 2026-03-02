package GUI;

import DAO.UtenteDAO;
import ImplementazionePostgresDAO.UtentePostgresDAO;
import model.Utente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Finestra di login dell'applicazione ToDo.
 * Mostra un form con campo login e password, verifica le credenziali
 * e apre la {@link Home} in caso di successo.
 */
public class Login extends JFrame {
    private JTextPane emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel message;
    private JPanel login;

    private final UtenteDAO utenteDAO = new UtentePostgresDAO();

    /**
     * Entry point dell'applicazione.
     * Avvia la finestra di login sul thread EDT di Swing.
     *
     * @param args argomenti da riga di comando (non utilizzati)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Login frame = new Login();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(frame.login);
            frame.pack();
            frame.setVisible(true);
        });
    }

    /**
     * Costruttore della finestra di login.
     * Collega il bottone di login all'action handler e imposta il bottone di default.
     */
    public Login() {
        loginButton.addActionListener(this::onLogin);
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Gestisce il click sul bottone di login.
     * Legge le credenziali inserite, le valida e interroga il database.
     * Se il login ha successo apre la {@link Home} e chiude questa finestra.
     *
     * @param e l'evento generato dal click sul bottone
     */
    private void onLogin(ActionEvent e) {
        String user = emailField.getText().trim();
        String pass = String.valueOf(passwordField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            setMsg("Inserisci login e password", Color.RED);
            return;
        }

        try {
            Optional<Utente> u = utenteDAO.login(user, pass);

            if (u.isPresent()) {
                setMsg("Benvenuto, " + u.get().getLogin() + "!", new Color(0, 150, 0));

                Home home = new Home(u.get());
                home.setVisible(true);
                this.dispose();

            } else {
                setMsg("Credenziali errate :(", Color.RED);
            }

        } catch (SQLException ex) {
            setMsg("Errore DB: " + ex.getMessage(), Color.RED);
            ex.printStackTrace();
        }
    }

    /**
     * Aggiorna il testo e il colore della label di messaggio nella UI.
     *
     * @param text  il messaggio da mostrare
     * @param color il colore del testo (es. {@link Color#RED} per errori)
     */
    private void setMsg(String text, Color color) {
        message.setText(text);
        message.setForeground(color);
    }
}