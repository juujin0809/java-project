import java.sql.*;
import java.util.ArrayList;

public class ChatRoomDAO {

    // 채팅방 생성 (중복 체크 추가)
    public void createRoom(String roomName) {

        String checkSql = "SELECT * FROM chat_room WHERE room_name=?";
        String insertSql = "INSERT INTO chat_room(room_name) VALUES (?)";

        try (Connection conn = DBConnection.getConnection()) {

            // 🔹 중복 체크
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, roomName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                System.out.println("이미 존재하는 채팅방 이름입니다.");
                return;
            }

            // 🔹 채팅방 생성
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, roomName);
            insertStmt.executeUpdate();

            System.out.println("채팅방 생성 완료!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 채팅방 목록 조회 (그대로)
    public ArrayList<String> getRooms() {

        ArrayList<String> list = new ArrayList<>();
        String sql = "SELECT * FROM chat_room";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("room_name");

                list.add(id + "번: " + name);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
