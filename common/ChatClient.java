/* 닉네임 null/빈 값 체크 → 잘못된 닉네임이면 종료
 * 수신 스레드 안정화 → setDaemon(true)로 프로그램 종료 시 자동 종료
 * 귓속말 형식 검증 → /w 닉네임 내용이 아니면 안내 메시지 출력
 * 종료 처리 강화 → /exit 입력 시 소켓 닫고 정상 종료
 * GUI 연동 포인트 → System.out.println(msg) 대신 JTextArea.append(msg) 같은 방식으로 교체 가능
*/

package Common;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final int START_PORT = 12345;
    private static final int MAX_PORT = 12355;

    public static void main(String[] args) {
        String serverAddress = "localhost";
        Socket socket = null;
        int port = START_PORT;

        // 포트 자동 변경 시도
        while (socket == null && port <= MAX_PORT) {
            try {
                socket = new Socket(serverAddress, port);
                System.out.println("서버 연결 성공: 포트 " + port);
            } catch (IOException e) {
                System.out.println("포트 " + port + " 연결 실패, 다음 포트 시도...");
                port++;
            }
        }

        if (socket == null) {
            System.out.println("서버에 연결할 수 없습니다.");
            return;
        }

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            // 닉네임 입력
            System.out.print("닉네임을 입력하세요: ");
            String nickname = userInput.readLine();
            if (nickname == null || nickname.isEmpty()) {
                System.out.println("닉네임이 유효하지 않습니다. 종료합니다.");
                return;
            }
            out.println(nickname);

            // 서버 메시지 수신 스레드
            Thread receiveThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                        // TODO: GUI 연결 시 JTextArea.append(msg) 같은 방식으로 출력
                    }
                } catch (IOException e) {
                    System.out.println("서버 연결 종료");
                }
            });
            receiveThread.setDaemon(true); // 프로그램 종료 시 자동 종료
            receiveThread.start();

            // 사용자 입력 → 서버 전송
            String input;
            while ((input = userInput.readLine()) != null) {
                if (input.startsWith("/w ")) {
                    // 귓속말: "/w 상대닉네임 내용"
                    String[] parts = input.split(" ", 3);
                    if (parts.length == 3) {
                        String receiver = parts[1];
                        String content = parts[2];
                        out.println(Protocol.WHISPER + Protocol.SEPARATOR
                                    + nickname + Protocol.SEPARATOR
                                    + receiver + Protocol.SEPARATOR
                                    + content);
                    } else {
                        System.out.println("귓속말 형식: /w 닉네임 내용");
                    }
                } else {
                    // 일반 채팅
                    out.println(Protocol.CHAT + Protocol.SEPARATOR
                                + nickname + Protocol.SEPARATOR
                                + "ALL" + Protocol.SEPARATOR
                                + input);
                }

                if (input.equals("/exit")) {
                    System.out.println("채팅 종료");
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("클라이언트 오류: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("소켓 종료 실패");
            }
        }
    }
}
