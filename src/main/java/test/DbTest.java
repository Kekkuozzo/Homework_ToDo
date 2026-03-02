package test;
import DAO.UtenteDAO;
import ImplementazionePostgresDAO.UtentePostgresDAO;

public class DbTest {
    public static void main(String[] args) throws Exception {
        UtenteDAO dao = new UtentePostgresDAO();
        System.out.println(dao.login("Mario", "m123").isPresent() ? "LOGIN OK" : "LOGIN FAIL");
    }
}
