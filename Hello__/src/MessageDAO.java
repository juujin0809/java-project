import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class MessageDAO {

    private String lastError = "";
    private UserDAO userDAO = new UserDAO();
    private ChatRoomDAO chatRoomDAO = new ChatRoomDAO();

    public MessageDAO() {
        ensureMessageTable();
    }

    private void ensureMessageTable() {
        String sql = "CREATE TABLE IF NOT EXISTS message (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "room_id INT NOT NULL, " +
                "content TEXT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (room_id) REFERENCES chat_room(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            lastError = "";
        } catch (Exception e) {
            lastError = "message 테이블 준비 실패: " + e.getMessage();
            System.out.println("[MessageDAO] " + lastError);
        }
    }

    public String getLastError() {
        return lastError;
    }

    // 기존 코드와 호환용: user_id, room_id를 직접 아는 경우 사용
    public boolean sendMessage(int userId, int roomId, String msg) {
        String sql = "INSERT INTO message(user_id, room_id, content) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, roomId);
            pstmt.setString(3, msg);
            pstmt.executeUpdate();
            lastError = "";
            System.out.println("메시지 저장 완료");
            return true;

        } catch (Exception e) {
            lastError = "메시지 저장 DB 오류: " + e.getMessage();
            System.out.println("[MessageDAO] " + lastError);
            return false;
        }
    }

    // 서버가 받은 username, roomName 기준으로 메시지를 저장합니다.
    public boolean saveMessage(String username, String roomName, String content) {
        if (username == null || roomName == null || content == null || content.trim().isEmpty()) {
            lastError = "메시지 저장 값이 비어있습니다.";
            return false;
        }

        int userId = userDAO.getUserId(username);
        int roomId = chatRoomDAO.getOrCreateRoomId(roomName);

        if (userId == -1 || roomId == -1) {
            lastError = "user_id 또는 room_id를 찾을 수 없습니다.";
            System.out.println("[MessageDAO] " + lastError + " username=" + username + ", roomName=" + roomName);
            return false;
        }

        return sendMessage(userId, roomId, content);
    }

    // 최근 메시지 10개를 Main.java가 바로 표시할 수 있는 프로토콜 문자열로 반환합니다.
    // 형식: HISTORY/yyyy-MM-dd/HH:mm/username/content
    public ArrayList<String> getRecentMessagesForClient(String roomName) {
        ArrayList<String> list = new ArrayList<>();
        int roomId = chatRoomDAO.getRoomId(roomName);
        if (roomId == -1) return list;

        String sql = "SELECT u.username, m.content, " +
                "DATE_FORMAT(m.created_at, '%Y-%m-%d') AS msg_date, " +
                "DATE_FORMAT(m.created_at, '%H:%i') AS msg_time " +
                "FROM message m " +
                "JOIN users u ON m.user_id = u.id " +
                "WHERE m.room_id=? " +
                "ORDER BY m.created_at DESC, m.id DESC LIMIT 10";

        ArrayList<String> reversed = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("msg_date");
                    String time = rs.getString("msg_time");
                    String username = rs.getString("username");
                    String content = rs.getString("content");
                    reversed.add("HISTORY/" + date + "/" + time + "/" + username + "/" + content);
                }
            }

            for (int i = reversed.size() - 1; i >= 0; i--) {
                list.add(reversed.get(i));
            }
            lastError = "";

        } catch (Exception e) {
            lastError = "최근 메시지 조회 DB 오류: " + e.getMessage();
            System.out.println("[MessageDAO] " + lastError);
        }

        return list;
    }

    // 기존 과제 설명용 형식과 호환되는 조회 메서드입니다.
    public ArrayList<String> getMessages(int roomId) {
        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT u.username, m.content, m.created_at " +
                     "FROM message m " +
                     "JOIN users u ON m.user_id = u.id " +
                     "WHERE m.room_id=? " +
                     "ORDER BY m.created_at DESC LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<String[]> temp = new ArrayList<>();

            while (rs.next()) {
                String username = rs.getString("username");
                String content = rs.getString("content");
                String datetime = rs.getString("created_at");
                String date = datetime.substring(0, 10);
                String time = datetime.substring(11, 16);
                temp.add(new String[]{date, time, username, content});
            }

            String lastDate = "";
            for (int i = temp.size() - 1; i >= 0; i--) {
                String date = temp.get(i)[0];
                String time = temp.get(i)[1];
                String username = temp.get(i)[2];
                String content = temp.get(i)[3];

                if (!date.equals(lastDate)) {
                    list.add("\n=== " + date + " ===");
                    lastDate = date;
                }
                list.add("[" + time + "] " + username + ": " + content);
            }
            lastError = "";

        } catch (Exception e) {
            lastError = "메시지 조회 DB 오류: " + e.getMessage();
            System.out.println("[MessageDAO] " + lastError);
        }
        return list;
    }
}
