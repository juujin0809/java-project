import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    // DB 주소
    private static final String URL = "jdbc:mysql://localhost:3306/chatdb?serverTimezone=UTC";

    // DB 계정
    private static final String USER = "root";

    // DB 비밀번호
    private static final String PASSWORD = "1234";

    // DB 연결 메서드 (다른 클래스에서 공통 사용)
    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
