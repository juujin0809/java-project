import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class FriendDAO {

    private String lastError = "";
    private UserDAO userDAO = new UserDAO();

    public FriendDAO() {
        ensureFriendTable();
    }

    private void ensureFriendTable() {
        String sql = "CREATE TABLE IF NOT EXISTS friend (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "friend_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uq_friend_pair (user_id, friend_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            lastError = "";
        } catch (Exception e) {
            lastError = "friend 테이블 준비 실패: " + e.getMessage();
            System.out.println("[FriendDAO] " + lastError);
        }
    }

    public String getLastError() {
        return lastError;
    }

    public boolean addFriend(String username, String friendUsername) {
        if (username == null || username.trim().isEmpty() || friendUsername == null || friendUsername.trim().isEmpty()) {
            lastError = "친구 ID를 입력해주세요.";
            return false;
        }

        username = username.trim();
        friendUsername = friendUsername.trim();

        if (username.equals(friendUsername)) {
            lastError = "자기 자신은 친구로 추가할 수 없습니다.";
            return false;
        }

        int userId = userDAO.getUserId(username);
        int friendId = userDAO.getUserId(friendUsername);

        if (userId == -1) {
            lastError = "로그인 사용자를 찾을 수 없습니다.";
            return false;
        }
        if (friendId == -1) {
            lastError = "존재하지 않는 사용자 ID입니다.";
            return false;
        }

        if (isAlreadyFriend(userId, friendId)) {
            lastError = "이미 친구 목록에 있는 사용자입니다.";
            return false;
        }

        String sql = "INSERT INTO friend(user_id, friend_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);
            pstmt.executeUpdate();
            lastError = "";
            return true;

        } catch (Exception e) {
            lastError = "친구 추가 DB 오류: " + e.getMessage();
            System.out.println("[FriendDAO] " + lastError);
            return false;
        }
    }

    private boolean isAlreadyFriend(int userId, int friendId) {
        String sql = "SELECT id FROM friend WHERE user_id=? AND friend_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, friendId);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean result = rs.next();
                lastError = "";
                return result;
            }

        } catch (Exception e) {
            lastError = "친구 중복 확인 DB 오류: " + e.getMessage();
            System.out.println("[FriendDAO] " + lastError);
            return false;
        }
    }

    public ArrayList<String> getFriendNames(String username) {
        ArrayList<String> list = new ArrayList<>();
        int userId = userDAO.getUserId(username);
        if (userId == -1) {
            lastError = "사용자 ID를 찾을 수 없습니다.";
            return list;
        }

        String sql = "SELECT u.username " +
                "FROM friend f " +
                "JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id=? " +
                "ORDER BY u.username ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("username"));
                }
            }
            lastError = "";

        } catch (Exception e) {
            lastError = "친구 목록 조회 DB 오류: " + e.getMessage();
            System.out.println("[FriendDAO] " + lastError);
        }

        return list;
    }
}
