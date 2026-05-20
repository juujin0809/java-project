import java.sql.*;

public class UserDAO {

    // 회원가입 (중복 체크 추가)
    public void register(String username, String password) {

        String checkSql = "SELECT * FROM users WHERE username=?";
        String insertSql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection()) {

            // 🔹 중복 체크
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                System.out.println("이미 존재하는 아이디입니다.");
                return;
            }

            // 🔹 회원가입 진행
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.executeUpdate();

            System.out.println("회원가입 성공!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 로그인 (그대로 유지)
    public boolean login(String username, String password) {

        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            return rs.next(); // 있으면 true

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
    
    // user id 얻기 (그대로 유지)
    public int getUserId(String username, String password) {

        String sql = "SELECT id FROM users WHERE username=? AND password=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id"); // 핵심🔥
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; // 실패
    }
}
