package GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class Login extends JFrame {
    private JTextPane emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel message;
    private JPanel login;

    public static void main(String[] args) {
        JFrame frame = new Login();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new Login().login);
        frame.pack();
        frame.setVisible(true);

    }

    public Login() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String user = emailField.getText();
                String pass = String.valueOf(passwordField.getPassword());

                if ("admin".equals(user) && "forzanapoli".equals(pass) ) {
                    message.setText("Terracciano e Pistone ti danno il benvenuto!");
                    message.setForeground(Color.green);
                } else {
                    message.setText("Credenziali errate :(");
                    message.setForeground(Color.red);
                }
            }
        });
    }

}


