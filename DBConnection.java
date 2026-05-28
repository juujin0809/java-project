import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnection {

    // 서버를 실행하는 컴퓨터의 MySQL 정보입니다.
    // 클라이언트(Main.java)는 이 정보를 사용하지 않습니다.
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DB_NAME = "chatdb";
    private static final String USER = "root";
    //YS 서버 연결
    private static final String PASSWORD = "1234abcd";

    private static final String COMMON_OPTIONS = "?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String SERVER_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + COMMON_OPTIONS;
    private static final String DB_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME + COMMON_OPTIONS;

    private static boolean databaseChecked = false;

    // DB가 없으면 자동으로 chatdb를 생성합니다.
    private static void ensureDatabase() throws Exception {
        if (databaseChecked) return;

        try (Connection conn = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            databaseChecked = true;
        }
    }

    // DB 연결 메서드
    public static Connection getConnection() throws Exception {
        ensureDatabase();
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }
}
