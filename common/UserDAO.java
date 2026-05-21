import java.sql.*;

public class UserDAO {

    // 회원가입 (중복 체크 포함)
    public void register(String username, String password) {

        String checkSql = "SELECT * FROM users WHERE username=?"; 
        // 동일 아이디 존재 여부 확인

        String insertSql = "INSERT INTO users(username, password) VALUES (?, ?)"; 
        // 회원 정보 DB 저장

        try (Connection conn = DBConnection.getConnection()) {

            PreparedStatement checkStmt = conn.prepareStatement(checkSql); 
            // SELECT SQL 실행 준비

            checkStmt.setString(1, username); 
            // ? 자리에 username 넣기

            ResultSet rs = checkStmt.executeQuery(); 
            // SELECT 실행 → 결과 반환

            if (rs.next()) { 
                // 결과가 있으면 (이미 존재)
                System.out.println("이미 존재하는 아이디입니다.");
                return;
            }

            PreparedStatement insertStmt = conn.prepareStatement(insertSql); 
            // INSERT SQL 준비

            insertStmt.setString(1, username); 
            // username 값 설정

            insertStmt.setString(2, password); 
            // password 값 설정

            insertStmt.executeUpdate(); 
            // DB에 실제 데이터 저장

            System.out.println("회원가입 성공!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 로그인
    public boolean login(String username, String password) {

        String sql = "SELECT * FROM users WHERE username=? AND password=?"; 
        // 아이디, 비밀번호 일치 여부 확인

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username); 
            // username 조건 값

            pstmt.setString(2, password); 
            // password 조건 값

            ResultSet rs = pstmt.executeQuery(); 
            // SELECT 실행

            return rs.next(); 
            // 결과 있으면 true (로그인 성공)

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // user id 얻기
    public int getUserId(String username, String password) {

        String sql = "SELECT id FROM users WHERE username=? AND password=?"; 
        // 해당 사용자 id 조회

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username); 
            // username 설정

            pstmt.setString(2, password); 
            // password 설정

            ResultSet rs = pstmt.executeQuery(); 
            // 쿼리 실행

            if (rs.next()) {
                return rs.getInt("id"); 
                // id 값 반환
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1; 
        // 실패 시 -1 반환
    }
}
