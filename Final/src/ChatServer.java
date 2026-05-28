import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    // DB는 서버에서만 사용합니다. Main.java는 DB에 직접 접근하지 않습니다.
    private static UserDAO userDAO;
    private static ChatRoomDAO chatRoomDAO;
    private static MessageDAO messageDAO;
    private static FriendDAO friendDAO;

    public static void main(String[] args) {
        initDatabaseObjects();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=== 🚀 DB 중앙처리 채팅 서버 가동 (포트: " + PORT + ") ===");
            System.out.println("클라이언트는 이 서버의 IP와 " + PORT + " 포트로 접속하면 됩니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                System.out.println("👉 새 클라이언트 접속: " + clientSocket.getInetAddress());
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("❌ 서버 실행 실패: " + e.getMessage());
            System.out.println("포트 " + PORT + "가 이미 사용 중이면 기존 서버를 종료하고 다시 실행하세요.");
        }
    }

    private static void initDatabaseObjects() {
        userDAO = new UserDAO();
        chatRoomDAO = new ChatRoomDAO();
        chatRoomDAO.ensureDefaultRooms();
        messageDAO = new MessageDAO();
        friendDAO = new FriendDAO();
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String nickname;
        private PrintWriter out;
        private String currentRoom = "대기실";

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public String getNickname() {
            return nickname;
        }

        public void sendMessage(String msg) {
            if (out != null) out.println(msg);
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    String packetInfo = PacketExtractor.getPacketInfo(socket, message);
                    System.out.println("📡 패킷 " + packetInfo + " -> " + maskPasswordPacket(message));

                    if (message.startsWith("SIGNUP/")) {
                        handleSignup(message);
                        continue;
                    }

                    if (message.startsWith("LOGIN/")) {
                        handleLogin(message);
                        continue;
                    }

                    if (nickname == null) {
                        out.println("AUTH_REQUIRED/로그인이 먼저 필요합니다.");
                        continue;
                    }

                    if (message.equals("GET_ROOMS")) {
                        sendRoomListToClient(this);
                        continue;
                    }

                    if (message.equals("GET_FRIENDS")) {
                        sendFriendListToClient(this);
                        continue;
                    }

                    if (message.startsWith("ADD_FRIEND/")) {
                        handleAddFriend(message);
                        continue;
                    }

                    if (message.startsWith("CREATE_ROOM/")) {
                        handleCreateRoom(message);
                        continue;
                    }

                    String[] tokens = message.split(Protocol.SEPARATOR, 5);
                    if (tokens.length >= 3) {
                        String type = tokens[0];

                        if (type.equals("JOIN")) {
                            handleJoin(tokens);
                        }
                        else if (type.equals(Protocol.CHAT) && tokens.length >= 5) {
                            handleChat(tokens);
                        }
                        else if (type.equals(Protocol.WHISPER) && tokens.length >= 5) {
                            handleWhisper(tokens);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ 클라이언트 연결 끊김: " + socket.getInetAddress());
            } finally {
                closeClient();
            }
        }

        private void handleSignup(String message) {
            String[] tokens = message.split(Protocol.SEPARATOR, 3);
            if (tokens.length < 3) {
                out.println("SIGNUP_FAIL/회원가입 형식이 올바르지 않습니다.");
                return;
            }

            String id = tokens[1].trim();
            String pw = tokens[2];

            if (id.isEmpty() || pw.trim().isEmpty()) {
                out.println("SIGNUP_FAIL/아이디와 비밀번호를 입력해주세요.");
                return;
            }

            boolean success = userDAO.register(id, pw);
            if (success) {
                out.println("SIGNUP_SUCCESS/회원가입 완료!");
            } else {
                String error = userDAO.getLastError();
                if (error == null || error.isEmpty()) error = "회원가입에 실패했습니다.";
                out.println("SIGNUP_FAIL/" + error);
            }
        }

        private void handleLogin(String message) {
            String[] tokens = message.split(Protocol.SEPARATOR, 3);
            if (tokens.length < 3) {
                out.println("LOGIN_FAIL/로그인 형식이 올바르지 않습니다.");
                return;
            }

            String id = tokens[1].trim();
            String pw = tokens[2];

            if (id.isEmpty() || pw.trim().isEmpty()) {
                out.println("LOGIN_FAIL/아이디와 비밀번호를 입력해주세요.");
                return;
            }

            boolean success = userDAO.login(id, pw);
            if (success) {
                nickname = id;
                currentRoom = "대기실";
                out.println("LOGIN_SUCCESS/로그인 성공");
                sendRoomListToClient(this);
                sendFriendListToClient(this);
                System.out.println("✅ 로그인 성공: " + nickname + " / " + socket.getInetAddress());
            } else {
                String error = userDAO.getLastError();
                if (error == null || error.isEmpty()) error = "아이디 또는 비밀번호가 틀렸습니다.";
                out.println("LOGIN_FAIL/" + error);
            }
        }

        private void handleAddFriend(String message) {
            String[] tokens = message.split(Protocol.SEPARATOR, 2);
            if (tokens.length < 2) {
                out.println("FRIEND_ADD_FAIL/친구 ID가 올바르지 않습니다.");
                return;
            }

            String friendName = tokens[1].trim();
            if (friendName.isEmpty()) {
                out.println("FRIEND_ADD_FAIL/친구 ID를 입력해주세요.");
                return;
            }
            if (friendName.length() > 50) {
                out.println("FRIEND_ADD_FAIL/친구 ID는 50자 이하로 입력해주세요.");
                return;
            }
            if (friendName.contains("/") || friendName.contains("|")) {
                out.println("FRIEND_ADD_FAIL/친구 ID에는 / 또는 | 문자를 사용할 수 없습니다.");
                return;
            }

            if (friendDAO == null) {
                out.println("FRIEND_ADD_FAIL/DB가 준비되지 않아 친구를 추가할 수 없습니다.");
                return;
            }

            boolean success = friendDAO.addFriend(nickname, friendName);
            if (success) {
                out.println("FRIEND_ADDED/" + friendName);
                sendFriendListToClient(this);
                System.out.println("✅ 친구 추가: " + nickname + " -> " + friendName);
            } else {
                String error = friendDAO.getLastError();
                if (error == null || error.isEmpty()) error = "친구 추가에 실패했습니다.";
                out.println("FRIEND_ADD_FAIL/" + error);
                sendFriendListToClient(this);
            }
        }

        private void handleCreateRoom(String message) {
            String[] tokens = message.split(Protocol.SEPARATOR, 2);
            if (tokens.length < 2) {
                out.println("ROOM_CREATE_FAIL/채팅방 이름이 올바르지 않습니다.");
                return;
            }

            String roomName = tokens[1].trim();
            if (roomName.isEmpty()) {
                out.println("ROOM_CREATE_FAIL/채팅방 이름을 입력해주세요.");
                return;
            }
            if (roomName.length() > 30) {
                out.println("ROOM_CREATE_FAIL/채팅방 이름은 30자 이하로 입력해주세요.");
                return;
            }
            if (roomName.contains("/") || roomName.contains("|")) {
                out.println("ROOM_CREATE_FAIL/채팅방 이름에는 / 또는 | 문자를 사용할 수 없습니다.");
                return;
            }

            if (chatRoomDAO == null) {
                out.println("ROOM_CREATE_FAIL/DB가 준비되지 않아 채팅방을 만들 수 없습니다.");
                return;
            }

            if (chatRoomDAO.getRoomId(roomName) != -1) {
                out.println("ROOM_CREATE_FAIL/이미 존재하는 채팅방입니다.");
                sendRoomListToClient(this);
                return;
            }

            boolean success = chatRoomDAO.createRoom(roomName);
            if (success) {
                out.println("ROOM_CREATED/" + roomName);
                broadcastRoomListToAll();
                System.out.println("✅ 채팅방 생성: " + roomName + " / 생성자: " + nickname);
            } else {
                String error = chatRoomDAO.getLastError();
                if (error == null || error.isEmpty()) error = "채팅방 생성에 실패했습니다.";
                out.println("ROOM_CREATE_FAIL/" + error);
            }
        }

        private void handleJoin(String[] tokens) {
            String targetRoom = tokens[1];
            String previousRoom = currentRoom;

            if (!previousRoom.equals("대기실") && !previousRoom.equals(targetRoom)) {
                broadcastToRoom(previousRoom, "[알림] " + nickname + "님이 퇴장했습니다.", this);
            }

            currentRoom = targetRoom;

            if (!previousRoom.equals("대기실") && !previousRoom.equals(targetRoom)) {
                broadcastRoomUsers(previousRoom);
            }

            if (!currentRoom.equals("대기실")) {
                sendRecentMessages(currentRoom);
                broadcastToRoom(currentRoom, "[알림] " + nickname + "님이 입장했습니다.", null);
                broadcastRoomUsers(currentRoom);
            }
        }

        private void sendRecentMessages(String roomName) {
            if (messageDAO == null) return;
            for (String history : messageDAO.getRecentMessagesForClient(roomName)) {
                sendMessage(history);
            }
        }

        private void handleChat(String[] tokens) {
            String room = tokens[1];
            String sender = tokens[2];
            String content = tokens[4];

            // 같은 방 사람들에게만 실시간 전송
            broadcastToRoom(room, sender + ": " + content, this);

            // DB 저장은 서버에서만 처리
            if (messageDAO != null) {
                messageDAO.saveMessage(sender, room, content);
            }
        }

        private void handleWhisper(String[] tokens) {
            String sender = nickname;
            String receiver = tokens[3].trim();
            String content = tokens[4].trim();

            if (receiver.isEmpty() || content.isEmpty()) {
                out.println("[알림] 귓속말 대상과 내용을 입력해주세요.");
                return;
            }

            sendWhisper(sender, receiver, content);
        }

        private void closeClient() {
            String previousRoom = currentRoom;
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            clientHandlers.remove(this);

            if (nickname != null && !previousRoom.equals("대기실")) {
                broadcastToRoom(previousRoom, "[알림] " + nickname + "님이 퇴장했습니다.", this);
                broadcastRoomUsers(previousRoom);
            }
        }
    }

    private static void sendRoomListToClient(ClientHandler client) {
        if (client == null || chatRoomDAO == null) return;

        ArrayList<String> rooms = chatRoomDAO.getRoomNames();
        if (rooms.isEmpty()) {
            rooms.add("B팀 방");
            rooms.add("실습 게임 방");
        }
        client.sendMessage("ROOM_LIST/" + String.join("||", rooms));
    }

    private static void sendFriendListToClient(ClientHandler client) {
        if (client == null || friendDAO == null || client.getNickname() == null) return;

        ArrayList<String> friends = friendDAO.getFriendNames(client.getNickname());
        client.sendMessage("FRIEND_LIST/" + String.join("||", friends));
    }

    private static void broadcastRoomListToAll() {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname() != null) {
                    sendRoomListToClient(client);
                }
            }
        }
    }

    private static void broadcastToRoom(String room, String message, ClientHandler sender) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname() != null && client != sender && room.equals(client.currentRoom)) {
                    client.sendMessage(message);
                }
            }
        }
    }

    private static void broadcastRoomUsers(String room) {
        ArrayList<String> users = new ArrayList<>();

        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname() != null && room.equals(client.currentRoom)) {
                    users.add(client.getNickname());
                }
            }

            String payload = "ROOM_USERS/" + room + "/" + String.join("||", users);
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname() != null && room.equals(client.currentRoom)) {
                    client.sendMessage(payload);
                }
            }
        }
    }

    private static void sendWhisper(String sender, String receiver, String content) {
        boolean found = false;
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client.getNickname() != null && client.getNickname().equals(receiver)) {
                    client.sendMessage("[귓속말] " + sender + " → " + receiver + ": " + content);
                    found = true;
                }
            }
        }
        if (!found) {
            synchronized (clientHandlers) {
                for (ClientHandler client : clientHandlers) {
                    if (client.getNickname() != null && client.getNickname().equals(sender)) {
                        client.sendMessage("[알림] " + receiver + "님이 현재 접속 중이 아닙니다.");
                    }
                }
            }
        }
    }

    private static String maskPasswordPacket(String message) {
        if (message.startsWith("LOGIN/") || message.startsWith("SIGNUP/")) {
            String[] tokens = message.split(Protocol.SEPARATOR, 3);
            if (tokens.length == 3) {
                return tokens[0] + "/" + tokens[1] + "/****";
            }
        }
        return message;
    }
}
