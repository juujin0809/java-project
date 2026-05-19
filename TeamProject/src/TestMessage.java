public class TestMessage {
    public static void main(String[] args) {

        MessageDAO dao = new MessageDAO();

        dao.sendMessage(1, 1, "안녕하세요!");
    }
}