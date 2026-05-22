import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int START_PORT = 12345;
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());
    private static MessageDAO messageDAO = new MessageDAO();

    public static void main(String[] args) {
        int port = START_PORT;
        ServerSocket serverSocket = null;

        while (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("=== 🚀 고정방 라우팅 채팅 서버 가동 (포트: " + port + ") ===");
            } catch (IOException e) {
                port++;
            }
        }

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                System.out.println("👉 새 클라이언트 접속: " + clientSocket.getInetAddress());
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
        
        // 🌟 핵심 이름표: 이 클라이언트가 현재 위치한 방 이름 (기본값은 대기실)
        private String currentRoom = "대기실";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getNickname() { return nickname; }
        public void sendMessage(String msg) { out.println(msg); }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                // 최초 접속 시 닉네임 등록
                nickname = in.readLine();

                String message;
                // 🌟 메시지 본문에 슬래시(/)가 있어도 터지지 않도록 최대 5개까지만 쪼갭니다.
                while ((message = in.readLine()) != null) {
                    
                    // 우진 님의 패킷 분석기 가동
                    String packetInfo = PacketExtractor.getPacketInfo(socket, message);
                    System.out.println("📡 패킷 " + packetInfo + " -> " + message);

                    String[] tokens = message.split(Protocol.SEPARATOR, 5);
                    if (tokens.length >= 3) {
                        String type = tokens[0];

                        // 🌟 [이동 규칙] 방을 이동하거나 입장했을 때
                        if (type.equals("JOIN")) {
                            String targetRoom = tokens[1];
                            
                            // 원래 있던 방에 퇴장 알림 전송 (대기실이 아니었다면)
                            if (!currentRoom.equals("대기실")) {
                                broadcastToRoom(currentRoom, "[알림] " + nickname + "님이 퇴장했습니다.", null);
                            }
                            
                            // 방 바꾸기
                            currentRoom = targetRoom;
                            
                            // 새로 들어간 방에만 입장 알림 전송
                            if (!currentRoom.equals("대기실")) {
                                broadcastToRoom(currentRoom, "[알림] " + nickname + "님이 입장했습니다.", null);
                            }
                        } 
                        // 🌟 [채팅 규칙] 일반 채팅 처리
                        else if (type.equals(Protocol.CHAT) && tokens.length >= 5) {
                            String room = tokens[1];
                            String sender = tokens[2];
                            String content = tokens[4];
                            
                            // 💥 전체 전송이 아니라, 같은 방 이름표를 단 사람들에게만 전송!
                            broadcastToRoom(room, sender + ": " + content, this);
                            
                            // DB 연동 테스트시 활성화 (필요 없으면 주석 유지)
                            // messageDAO.sendMessage(1, 1, content);
                        } 
                        // 🌟 [귓속말 규칙] 귓속말 처리
                        else if (type.equals(Protocol.WHISPER) && tokens.length >= 5) {
                            String sender = tokens[2];
                            String receiver = tokens[3];
                            String content = tokens[4];
                            sendWhisper(sender, receiver, content);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ 클라이언트 연결 끊김: " + socket.getInetAddress());
            } finally {
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
                clientHandlers.remove(this);
                // 나가 버렸을 때 방에 남아있는 사람들에게만 퇴장 알림
                if (!currentRoom.equals("대기실")) {
                    broadcastToRoom(currentRoom, "[알림] " + nickname + "님이 퇴장했습니다.", this);
                }
            }
        }
    }

    // 🌟 특정 방에 있는 클라이언트들에게만 브로드캐스트하는 메서드
    private static void broadcastToRoom(String room, String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                // 보낸 사람이 아니고, 클라이언트의 이름표(currentRoom)가 일치하는 경우에만 전송!
                if (client != sender && room.equals(client.currentRoom)) {
                    client.sendMessage(message);
                }
            }
        }
    }

    // 귓속말 처리 (귓속말은 방에 상관없이 유저 닉네임 기준으로 찾아 전송)
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
