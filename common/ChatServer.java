/*
 * 자바 서버 구현
 * 클라이언트 접속을 확인하고 접속 했을 시 접속했다는 출력을 내보냅니다
 * 오류가 나면 종료 메시지를 출력하고 프로그램이 종료됩니다
 * 이 서버의 포트 넘버는 임시 지정이므로 코드를 합칠 때 정하는 것도 좋을거 같습니다
 * 추가적인 내용이 필요하거나 전체적인 로직이 다르다면 말씀해주세요!
 * */

package Common;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int START_PORT = 12345;
    private static Set<ClientHandler> clientHandlers =
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = START_PORT;
        ServerSocket serverSocket = null;

        // 포트 자동 변경
        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("서버 시작: 포트 " + port);
            } catch (IOException e) {
                System.out.println("포트 " + port + " 사용 중, 다음 포트 시도...");
                port++;
            }
        }

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                System.out.println("새 클라이언트 접속: " + clientSocket.getInetAddress());
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 클라이언트 핸들러
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String nickname;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getNickname() {
            return nickname;
        }

        public void sendMessage(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);

                // 첫 메시지는 닉네임
                nickname = in.readLine();
                broadcast("[알림] " + nickname + "님이 입장했습니다.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("수신: " + message);

                    // Protocol 구분자 기준으로 파싱
                    String[] tokens = message.split(Protocol.SEPARATOR);
                    if (tokens.length >= 4) {
                        String type = tokens[0];
                        String sender = tokens[1];
                        String receiver = tokens[2];
                        String content = tokens[3];

                        if (type.equals(Protocol.CHAT)) {
                            broadcast(sender + ": " + content, this);
                        } else if (type.equals(Protocol.WHISPER)) {
                            sendWhisper(sender, receiver, content);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료: " + socket.getInetAddress());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                clientHandlers.remove(this);
                broadcast("[알림] " + nickname + "님이 퇴장했습니다.", this);
            }
        }
    }

    // 전체 브로드캐스트
    private static void broadcast(String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // 귓속말 처리
    private static void sendWhisper(String sender, String receiver, String content) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname().equals(receiver)) {
                    client.sendMessage("[귓속말] " + sender + " → " + receiver + ": " + content);
                    break;
                }
            }
        }
    }
}
