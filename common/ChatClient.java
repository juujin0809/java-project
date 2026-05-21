package ChatClient;

import java.io.*;
import java.net.*;

public class ChatClient {
    private static final int START_PORT = 12345; // 시작 포트
    private static final int MAX_PORT = 12355;   // 시도할 최대 포트 범위

    public static void main(String[] args) {
        String serverAddress = "localhost"; // 서버 주소
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
            out.println(nickname);

            // 서버 메시지 수신 스레드
            Thread receiveThread = new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    System.out.println("서버 연결 종료");
                }
            });
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
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
