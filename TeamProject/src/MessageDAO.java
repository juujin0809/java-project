import java.sql.*;
import java.util.ArrayList;

public class MessageDAO {

    // 🔹 메시지 저장
    public void sendMessage(int userId, int roomId, String msg) {

        String sql = "INSERT INTO message(user_id, room_id, content) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, roomId);
            pstmt.setString(3, msg);

            pstmt.executeUpdate();

            System.out.println("메시지 저장 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 메시지 조회 (이름 + 시간 + 순서 포함)
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

            // 🔥 여기 핵심
            String lastDate = "";

            for (int i = temp.size() - 1; i >= 0; i--) {

                String date = temp.get(i)[0];
                String time = temp.get(i)[1];
                String username = temp.get(i)[2];
                String content = temp.get(i)[3];

                // 🔥 날짜가 바뀔 때만 출력
                if (!date.equals(lastDate)) {
                    list.add("\n=== " + date + " ===");
                    lastDate = date;
                }

                list.add("[" + time + "] " + username + ": " + content);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}