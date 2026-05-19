import java.sql.*;

public class UserDAO {

    // 회원가입
    public void register(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            System.out.println("회원가입 성공!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 로그인
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
    
    // user id 얻기
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