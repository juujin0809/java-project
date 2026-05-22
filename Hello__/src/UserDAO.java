import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;

public class UserDAO {

    private String lastErrorMessage = "";

    public UserDAO() {
        createUserTableIfNotExists();
    }

    // users 테이블이 없으면 자동 생성
    // 이미 테이블이 있으면 아무 일도 하지 않음
    private void createUserTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT PRIMARY KEY AUTO_INCREMENT, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "password VARCHAR(100) NOT NULL"
                + ") CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeUpdate();
            lastErrorMessage = "";

        } catch (Exception e) {
            saveError("users 테이블 확인/생성 실패", e);
        }
    }

    // Main.java에서 로그인/회원가입 전에 DB 연결 상태를 확인할 때 사용
    public boolean checkConnection() {
        String sql = "SELECT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.executeQuery();
            lastErrorMessage = "";
            return true;

        } catch (Exception e) {
            saveError("DB 연결 실패", e);
            return false;
        }
    }

    // 회원가입: users 테이블에 새 유저 저장
    public boolean register(String id, String pw) {
        id = clean(id);

        if (id.isEmpty() || pw == null || pw.isEmpty()) {
            lastErrorMessage = "아이디와 비밀번호를 입력하세요.";
            return false;
        }

        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, pw);

            int result = pstmt.executeUpdate();
            lastErrorMessage = "";
            return result == 1;

        } catch (SQLIntegrityConstraintViolationException e) {
            lastErrorMessage = "이미 존재하는 ID입니다.";
            return false;

        } catch (Exception e) {
            saveError("회원가입 DB 저장 실패", e);
            return false;
        }
    }

    // 아이디 존재 여부 확인
    public boolean exists(String id) {
        id = clean(id);

        String sql = "SELECT id FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                lastErrorMessage = "";
                return rs.next();
            }

        } catch (Exception e) {
            saveError("아이디 조회 실패", e);
            return false;
        }
    }

    // 로그인 확인
    public boolean login(String id, String pw) {
        id = clean(id);

        String sql = "SELECT password FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    lastErrorMessage = "가입되지 않은 유저입니다.";
                    return false;
                }

                String dbPassword = rs.getString("password");
                boolean success = dbPassword.equals(pw);

                if (success) {
                    lastErrorMessage = "";
                } else {
                    lastErrorMessage = "비밀번호가 틀렸습니다.";
                }

                return success;
            }

        } catch (Exception e) {
            saveError("로그인 조회 실패", e);
            return false;
        }
    }

    // 추후 MessageDAO와 연결할 때 username으로 user_id를 찾기 위해 사용 가능
    public int getUserId(String username) {
        username = clean(username);

        String sql = "SELECT id FROM users WHERE username = ? LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    lastErrorMessage = "";
                    return rs.getInt("id");
                }
            }

            lastErrorMessage = "해당 유저를 찾을 수 없습니다.";
            return -1;

        } catch (Exception e) {
            saveError("user_id 조회 실패", e);
            return -1;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    // ChatServer.java에서 짧은 이름으로 오류 메시지를 가져올 때 사용
    public String getLastError() {
        return lastErrorMessage;
    }

    private String clean(String value) {
        if (value == null) return "";
        return value.trim();
    }

    private void saveError(String message, Exception e) {
        lastErrorMessage = message + " : " + e.getMessage();
        e.printStackTrace();
    }
}
