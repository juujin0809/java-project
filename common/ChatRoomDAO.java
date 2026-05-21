import java.sql.*;
import java.util.ArrayList;

public class ChatRoomDAO {

    // 채팅방 생성
    public void createRoom(String roomName) {

        String checkSql = "SELECT * FROM chat_room WHERE room_name=?"; 
        // 채팅방 이름 중복 확인

        String insertSql = "INSERT INTO chat_room(room_name) VALUES (?)"; 
        // 채팅방 생성 SQL

        try (Connection conn = DBConnection.getConnection()) { // Connection = DB 연결

            PreparedStatement checkStmt = conn.prepareStatement(checkSql); // PreparedStatement = SQL 실행 준비
            // SELECT 준비

            checkStmt.setString(1, roomName); 
            // 이름 설정

            ResultSet rs = checkStmt.executeQuery(); 
            // 실행

            if (rs.next()) { // 조회 결과가 있으면 → true, 없으면 → false
                System.out.println("이미 존재하는 채팅방입니다.");
                return;
            }

            PreparedStatement insertStmt = conn.prepareStatement(insertSql); 
            // INSERT 준비

            insertStmt.setString(1, roomName); 
            // 채팅방 이름 설정

            insertStmt.executeUpdate(); 
            // DB 저장

            System.out.println("채팅방 생성 완료!");

        } catch (Exception e) { // e = 에러 정보 저장하는 변수
            e.printStackTrace(); // 에러 상세 정보 전부 출력
        }
    }

    // 채팅방 목록 조회
    public ArrayList<String> getRooms() {

        ArrayList<String> list = new ArrayList<>();

        String sql = "SELECT * FROM chat_room"; 
        // 전체 채팅방 조회

        try (Connection conn = DBConnection.getConnection(); // Connection = DB 연결
             PreparedStatement pstmt = conn.prepareStatement(sql)) { // PreparedStatement = SQL 실행 준비

            ResultSet rs = pstmt.executeQuery(); 
            // 실행

            while (rs.next()) {

                int id = rs.getInt("id"); 
                // 채팅방 ID

                String name = rs.getString("room_name"); 
                // 채팅방 이름

                list.add(id + "번: " + name); 
                // 리스트에 추가
            }

        } catch (Exception e) { // e = 에러 정보 저장하는 변수
            e.printStackTrace(); // 에러 상세 정보 전부 출력
        }

        return list;
    }
}
