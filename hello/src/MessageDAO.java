import java.sql.*;
import java.util.ArrayList;

public class MessageDAO {

    // 메시지 저장
    public void sendMessage(int userId, int roomId, String msg) {

        String sql = "INSERT INTO message(user_id, room_id, content) VALUES (?, ?, ?)"; 
        // 메시지 저장 SQL

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId); 
            // user_id 설정

            pstmt.setInt(2, roomId); 
            // room_id 설정

            pstmt.setString(3, msg); 
            // 메시지 내용 설정

            pstmt.executeUpdate(); 
            // DB에 INSERT 실행

            System.out.println("메시지 저장 완료");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 메시지 조회
    public ArrayList<String> getMessages(int roomId) {

        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT u.username, m.content, m.created_at " +
                     "FROM message m " +
                     "JOIN users u ON m.user_id = u.id " + 
                     // user_id → username 변환

                     "WHERE m.room_id=? " +
                     "ORDER BY m.created_at DESC LIMIT 10"; 
                     // 최신 메시지 10개 조회

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId); 
            // room_id 설정

            ResultSet rs = pstmt.executeQuery(); 
            // SELECT 실행

            ArrayList<String[]> temp = new ArrayList<>();

            while (rs.next()) {

                String username = rs.getString("username"); 
                // 사용자 이름 가져오기

                String content = rs.getString("content"); 
                // 메시지 내용 가져오기

                String datetime = rs.getString("created_at"); 
                // 전체 시간 가져오기

                String date = datetime.substring(0, 10); 
                // 날짜 부분 추출

                String time = datetime.substring(11, 16); 
                // 시간 부분 추출

                temp.add(new String[]{date, time, username, content}); 
                // 임시 리스트 저장
            }

            String lastDate = "";

            // 날짜 기준 그룹 출력
            for (int i = temp.size() - 1; i >= 0; i--) {

                String date = temp.get(i)[0];
                String time = temp.get(i)[1];
                String username = temp.get(i)[2];
                String content = temp.get(i)[3];

                if (!date.equals(lastDate)) {
                    list.add("\n=== " + date + " ==="); 
                    // 날짜 변경 시 출력
                    lastDate = date;
                }

                list.add("[" + time + "] " + username + ": " + content); 
                // 채팅 형식 출력
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
