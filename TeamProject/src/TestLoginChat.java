import java.util.Scanner;
import java.util.ArrayList;

public class TestLoginChat {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        UserDAO userDAO = new UserDAO();
        MessageDAO messageDAO = new MessageDAO();

        // 로그인
        System.out.print("아이디: ");
        String username = sc.nextLine();

        System.out.print("비밀번호: ");
        String password = sc.nextLine();

        int userId = userDAO.getUserId(username, password);

        if(userId == -1) {
            System.out.println("로그인 실패!");
            return;
        }

        System.out.println("로그인 성공! user_id = " + userId);

        // 메시지 입력
        System.out.print("메시지 입력: ");
        String msg = sc.nextLine();

        // 채팅방 1번 가정
        int roomId = 1;

        messageDAO.sendMessage(userId, roomId, msg);

        // 🔥 채팅 기록 출력 (추가된 부분)
        System.out.println("\n=== 채팅 기록 ===");

        ArrayList<String> list = messageDAO.getMessages(roomId);

        for(String m : list) {
            System.out.println(m);
        }

        sc.close();
    }
}