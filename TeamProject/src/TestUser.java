import java.util.Scanner;

public class TestUser {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        UserDAO dao = new UserDAO();

        // 회원가입
        System.out.print("아이디 입력: ");
        String username = sc.nextLine();

        System.out.print("비밀번호 입력: ");
        String password = sc.nextLine();

        dao.register(username, password);

        sc.close();
    }
}