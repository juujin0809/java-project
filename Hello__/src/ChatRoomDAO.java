import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class ChatRoomDAO {

    private String lastError = "";

    public ChatRoomDAO() {
        ensureRoomTable();
    }

    private void ensureRoomTable() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_room (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "room_name VARCHAR(100) NOT NULL UNIQUE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            lastError = "";
        } catch (Exception e) {
            lastError = "chat_room 테이블 준비 실패: " + e.getMessage();
            System.out.println("[ChatRoomDAO] " + lastError);
        }
    }

    public String getLastError() {
        return lastError;
    }

    // 기본 채팅방 2개를 DB에 보장합니다.
    public void ensureDefaultRooms() {
        createRoom("B팀 방");
        createRoom("실습 게임 방");
    }

    // 채팅방 생성. 이미 존재하면 그냥 성공으로 처리합니다.
    public boolean createRoom(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            lastError = "채팅방 이름이 비어있습니다.";
            return false;
        }

        String sql = "INSERT IGNORE INTO chat_room(room_name) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName.trim());
            pstmt.executeUpdate();
            lastError = "";
            return true;

        } catch (Exception e) {
            lastError = "채팅방 생성 DB 오류: " + e.getMessage();
            System.out.println("[ChatRoomDAO] " + lastError);
            return false;
        }
    }

    public int getRoomId(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) return -1;

        String sql = "SELECT id FROM chat_room WHERE room_name=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, roomName.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    lastError = "";
                    return rs.getInt("id");
                }
            }

        } catch (Exception e) {
            lastError = "채팅방 ID 조회 DB 오류: " + e.getMessage();
            System.out.println("[ChatRoomDAO] " + lastError);
        }
        return -1;
    }

    public int getOrCreateRoomId(String roomName) {
        int roomId = getRoomId(roomName);
        if (roomId != -1) return roomId;

        if (createRoom(roomName)) {
            return getRoomId(roomName);
        }
        return -1;
    }

    // Main.java의 고정 UI를 그대로 쓰지만, 서버 쪽에서는 DB 방 목록도 조회할 수 있게 둡니다.
    public ArrayList<String> getRoomNames() {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT room_name FROM chat_room ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getString("room_name"));
            }
            lastError = "";

        } catch (Exception e) {
            lastError = "채팅방 목록 조회 DB 오류: " + e.getMessage();
            System.out.println("[ChatRoomDAO] " + lastError);
        }
        return list;
    }

    // 기존 코드와 호환용입니다.
    public ArrayList<String> getRooms() {
        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT id, room_name FROM chat_room ORDER BY id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(rs.getInt("id") + "번: " + rs.getString("room_name"));
            }
            lastError = "";

        } catch (Exception e) {
            lastError = "채팅방 목록 조회 DB 오류: " + e.getMessage();
            System.out.println("[ChatRoomDAO] " + lastError);
        }
        return list;
    }
}
