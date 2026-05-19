import java.util.ArrayList;

public class TestSelect {
    public static void main(String[] args) {

        MessageDAO dao = new MessageDAO();

        ArrayList<String> list = dao.getMessages(1);

        for(String msg : list) {
            System.out.println(msg);
        }
    }
}