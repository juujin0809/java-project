import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main extends JFrame {

    private static final int SERVER_PORT = 12345;
    // 발표 때 다른 PC에서 바로 접속시키려면 localhost 대신 서버 노트북 IP로 바꾸면 됩니다.
    // 예) private static final String DEFAULT_SERVER_IP = "192.168.0.15";
    private static final String DEFAULT_SERVER_IP = "192.168.0.41";
    private String serverHost = DEFAULT_SERVER_IP;

    private DefaultListModel<String> roomModel = new DefaultListModel<>();
    private DefaultListModel<String> friendModel = new DefaultListModel<>();
    private DefaultListModel<String> roomUserModel = new DefaultListModel<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean receiveThreadStarted = false;
    private String loginUserId = "";

    private JPanel currentMessagePanel;
    private JScrollPane currentChatScroll;
    private String currentWhisperTarget;
    private String currentRoomName;
    private Set<String> whisperNotifiedFriends = new HashSet<>();
    private Map<String, List<WhisperRecord>> whisperMessages = new HashMap<>();

    private static class WhisperRecord {
        String sender;
        String content;
        String time;
        boolean mine;

        WhisperRecord(String sender, String content, String time, boolean mine) {
            this.sender = sender;
            this.content = content;
            this.time = time;
            this.mine = mine;
        }
    }

    public Main() {
        // 🌟 고정방 딱 2개만 선언하고 시작!
        roomModel.addElement("B팀 방");
        roomModel.addElement("실습 게임 방");

        showLoginScreen();
    }

    private boolean connectToServer() {
        if (socket != null && socket.isConnected() && !socket.isClosed() && out != null && in != null) {
            return true;
        }

        if (tryConnect(serverHost)) {
            return true;
        }

        String inputHost = JOptionPane.showInputDialog(
                this,
                "서버 연결 실패!\n" +
                "서버를 실행한 컴퓨터의 IP를 입력하세요.\n" +
                "같은 컴퓨터에서 실행 중이면 localhost를 입력하면 됩니다.",
                serverHost
        );

        if (inputHost == null || inputHost.trim().isEmpty()) {
            return false;
        }

        serverHost = inputHost.trim();
        return tryConnect(serverHost);
    }

    private boolean tryConnect(String host) {
        try {
            socket = new Socket(host, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("서버 연결 성공: " + host + ":" + SERVER_PORT);
            return true;
        } catch (Exception e) {
            closeConnection();
            System.out.println("서버 연결 실패: " + host + ":" + SERVER_PORT + " / " + e.getMessage());
            return false;
        }
    }

    private String sendAuthRequest(String request) {
        if (!connectToServer()) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패! ChatServer.java가 켜져 있는지 확인해주세요.");
            return null;
        }

        try {
            out.println(request);
            return in.readLine();
        } catch (Exception e) {
            closeConnection();
            JOptionPane.showMessageDialog(this, "서버 응답을 받지 못했습니다. 서버를 다시 확인해주세요.");
            return null;
        }
    }

    private void startReceiveThread() {
        if (receiveThreadStarted) return;
        receiveThreadStarted = true;

        Thread receiveThread = new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("수신: " + msg);
                    handleServerMessage(msg);
                }
            } catch (IOException e) {
                System.out.println("서버 연결 종료");
            } finally {
                closeConnection();
                receiveThreadStarted = false;
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    private void handleServerMessage(String msg) {
        if (msg.startsWith("ROOM_LIST/")) {
            String[] parts = msg.split("/", 2);
            updateRoomList(parts.length >= 2 ? parts[1] : "");
            return;
        }
        if (msg.startsWith("ROOM_CREATED/")) {
            String[] parts = msg.split("/", 2);
            final String roomName = parts.length >= 2 ? parts[1] : "새 채팅방";
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "채팅방이 생성되었습니다: " + roomName));
            return;
        }
        if (msg.startsWith("ROOM_CREATE_FAIL/")) {
            String[] parts = msg.split("/", 2);
            final String roomFailMessage = parts.length >= 2 ? parts[1] : "채팅방 생성에 실패했습니다.";
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, roomFailMessage));
            return;
        }
        if (msg.startsWith("FRIEND_LIST/")) {
            String[] parts = msg.split("/", 2);
            updateFriendList(parts.length >= 2 ? parts[1] : "");
            return;
        }
        if (msg.startsWith("ROOM_USERS/")) {
            String[] parts = msg.split("/", 3);
            String roomName = parts.length >= 2 ? parts[1] : "";
            String users = parts.length >= 3 ? parts[2] : "";
            updateRoomUserList(roomName, users);
            return;
        }
        if (msg.startsWith("FRIEND_ADDED/")) {
            String[] parts = msg.split("/", 2);
            final String addedFriendName = parts.length >= 2 ? parts[1] : "친구";
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, addedFriendName + "님을 친구 목록에 추가했습니다."));
            return;
        }
        if (msg.startsWith("FRIEND_ADD_FAIL/")) {
            String[] parts = msg.split("/", 2);
            final String friendFailMessage = parts.length >= 2 ? parts[1] : "친구 추가에 실패했습니다.";
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, friendFailMessage));
            return;
        }
        if (msg.startsWith("AUTH_REQUIRED/")) {
            String[] parts = msg.split("/", 2);
            final String authMessage = parts.length >= 2 ? parts[1] : "로그인이 먼저 필요합니다.";
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, authMessage));
            return;
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        final String finalMsg = msg;

        SwingUtilities.invokeLater(() -> {
            if (isWhisperMessage(finalMsg)) {
                String sender = getWhisperSender(finalMsg);
                String content = getWhisperContent(finalMsg);
                if (!sender.isEmpty()) {
                    addWhisperMessage(sender, sender, content, time, false);
                    if (sender.equals(currentWhisperTarget) && currentMessagePanel != null) {
                        appendWhisperBubble(sender, content, time, false);
                    } else {
                        markWhisperNotification(sender);
                        showWhisperArrivalNotice(sender);
                    }
                }
            } else if (currentMessagePanel == null) {
                return;
            } else if (finalMsg.startsWith("HISTORY/")) {
                // 형식: HISTORY/yyyy-MM-dd/HH:mm/username/content
                String[] parts = finalMsg.split("/", 5);
                if (parts.length >= 5) {
                    String historyTime = parts[2];
                    String sender = parts[3];
                    String content = parts[4];
                    boolean isMine = sender.equals(loginUserId);
                    currentMessagePanel.add(new ChatBubble(sender, content, historyTime, isMine));
                }
            } else if (finalMsg.startsWith("[알림]")) {
                currentMessagePanel.add(new SystemMessage(finalMsg));
            } else if (finalMsg.contains(": ")) {
                String[] parts = finalMsg.split(": ", 2);
                currentMessagePanel.add(new ChatBubble(parts[0], parts[1], time, false));
            }

            scrollCurrentChatToBottom();
        });
    }

    private void requestRoomList() {
        if (out != null) {
            out.println("GET_ROOMS");
        }
    }

    private void requestFriendList() {
        if (out != null) {
            out.println("GET_FRIENDS");
        }
    }

    private void addFriendFromInput() {
        String inputFriendName = JOptionPane.showInputDialog(this, "친구로 추가할 사용자 ID를 입력하세요.");
        if (inputFriendName == null) return;

        String friendName = inputFriendName.trim();
        if (friendName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "친구 ID를 입력해주세요.");
            return;
        }
        if (friendName.equals(loginUserId)) {
            JOptionPane.showMessageDialog(this, "자기 자신은 친구로 추가할 수 없습니다.");
            return;
        }
        if (friendName.length() > 50) {
            JOptionPane.showMessageDialog(this, "친구 ID는 50자 이하로 입력해주세요.");
            return;
        }
        if (friendName.contains("/") || friendName.contains("|")) {
            JOptionPane.showMessageDialog(this, "친구 ID에는 / 또는 | 문자를 사용할 수 없습니다.");
            return;
        }
        if (!connectToServer()) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패! ChatServer.java가 켜져 있는지 확인해주세요.");
            return;
        }
        out.println("ADD_FRIEND/" + friendName);
    }

    private void createRoomFromInput() {
        String roomName = JOptionPane.showInputDialog(this, "새 채팅방 이름을 입력하세요.");
        if (roomName == null) return;

        roomName = roomName.trim();
        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "채팅방 이름을 입력해주세요.");
            return;
        }
        if (roomName.length() > 30) {
            JOptionPane.showMessageDialog(this, "채팅방 이름은 30자 이하로 입력해주세요.");
            return;
        }
        if (roomName.contains("/") || roomName.contains("|")) {
            JOptionPane.showMessageDialog(this, "채팅방 이름에는 / 또는 | 문자를 사용할 수 없습니다.");
            return;
        }
        if (roomExistsInModel(roomName)) {
            JOptionPane.showMessageDialog(this, "이미 목록에 있는 채팅방입니다.");
            return;
        }
        if (!connectToServer()) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패! ChatServer.java가 켜져 있는지 확인해주세요.");
            return;
        }
        out.println("CREATE_ROOM/" + roomName);
    }

    private boolean roomExistsInModel(String roomName) {
        for (int i = 0; i < roomModel.size(); i++) {
            if (roomModel.getElementAt(i).equals(roomName)) {
                return true;
            }
        }
        return false;
    }

    private void updateRoomList(String roomListText) {
        SwingUtilities.invokeLater(() -> {
            roomModel.clear();
            if (roomListText != null && !roomListText.trim().isEmpty()) {
                String[] rooms = roomListText.split("\\|\\|");
                for (String room : rooms) {
                    String cleanRoom = room.trim();
                    if (!cleanRoom.isEmpty() && !roomExistsInModel(cleanRoom)) {
                        roomModel.addElement(cleanRoom);
                    }
                }
            }
            if (roomModel.isEmpty()) {
                roomModel.addElement("B팀 방");
                roomModel.addElement("실습 게임 방");
            }
        });
    }

    private void updateFriendList(String friendListText) {
        SwingUtilities.invokeLater(() -> {
            friendModel.clear();
            if (friendListText != null && !friendListText.trim().isEmpty()) {
                String[] friends = friendListText.split("\\|\\|");
                for (String friend : friends) {
                    String cleanFriend = friend.trim();
                    if (!cleanFriend.isEmpty()) {
                        friendModel.addElement(cleanFriend + getFriendStatusText(cleanFriend));
                    }
                }
            }
        });
    }

    private void updateRoomUserList(String roomName, String roomUsersText) {
        SwingUtilities.invokeLater(() -> {
            if (currentRoomName == null || !currentRoomName.equals(roomName)) return;

            roomUserModel.clear();
            if (roomUsersText != null && !roomUsersText.trim().isEmpty()) {
                String[] users = roomUsersText.split("\\|\\|");
                for (String user : users) {
                    String cleanUser = user.trim();
                    if (!cleanUser.isEmpty()) {
                        roomUserModel.addElement("● " + cleanUser);
                    }
                }
            }

            if (roomUserModel.isEmpty()) {
                roomUserModel.addElement("입장한 사람 없음");
            }
        });
    }

    private String getFriendStatusText(String friendName) {
        return whisperNotifiedFriends.contains(friendName) ? "   🔔 귓속말" : "   ● 친구";
    }

    private String getFriendNameFromListText(String text) {
        if (text == null) return "";
        return text.replace("   🔔 귓속말", "").replace("   ● 친구", "").trim();
    }

    private void markWhisperNotification(String friendName) {
        SwingUtilities.invokeLater(() -> {
            whisperNotifiedFriends.add(friendName);
            for (int i = 0; i < friendModel.size(); i++) {
                if (getFriendNameFromListText(friendModel.getElementAt(i)).equals(friendName)) {
                    friendModel.set(i, friendName + getFriendStatusText(friendName));
                    return;
                }
            }
        });
    }

    private void showWhisperArrivalNotice(String sender) {
        JOptionPane.showMessageDialog(
                this,
                sender + "님에게서 귓속말이 도착했습니다.",
                "귓속말 알림",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void addWhisperMessage(String targetName, String sender, String content, String time, boolean mine) {
        whisperMessages
                .computeIfAbsent(targetName, key -> new ArrayList<>())
                .add(new WhisperRecord(sender, content, time, mine));
    }

    private void renderWhisperMessages(String targetName, JPanel messagePanel) {
        List<WhisperRecord> records = whisperMessages.get(targetName);
        if (records == null) return;

        for (WhisperRecord record : records) {
            messagePanel.add(new ChatBubble(record.sender, record.content, record.time, record.mine));
        }
    }

    private void appendWhisperBubble(String sender, String content, String time, boolean mine) {
        if (currentMessagePanel == null) return;
        currentMessagePanel.add(new ChatBubble(sender, content, time, mine));
    }

    private void scrollCurrentChatToBottom() {
        if (currentMessagePanel != null) {
            currentMessagePanel.revalidate();
            currentMessagePanel.repaint();
        }

        if (currentChatScroll != null) {
            JScrollBar vertical = currentChatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }
    }

    private void clearWhisperNotification(String friendName) {
        whisperNotifiedFriends.remove(friendName);
        for (int i = 0; i < friendModel.size(); i++) {
            if (getFriendNameFromListText(friendModel.getElementAt(i)).equals(friendName)) {
                friendModel.set(i, friendName + getFriendStatusText(friendName));
                return;
            }
        }
    }

    private boolean isWhisperMessage(String msg) {
        return msg != null && msg.startsWith("[귓속말]");
    }

    private String getWhisperSender(String msg) {
        if (!isWhisperMessage(msg)) return "";
        int start = "[귓속말]".length();
        int arrow = msg.indexOf(" → ", start);
        if (arrow < 0) return "";
        return msg.substring(start, arrow).trim();
    }

    private String getWhisperContent(String msg) {
        if (!isWhisperMessage(msg)) return msg;
        int contentStart = msg.indexOf(": ");
        if (contentStart < 0) return msg;
        return msg.substring(contentStart + 2);
    }

    private void closeConnection() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        out = null;
        in = null;
    }

    private Font titleFont(int size) { return new Font("Arial Rounded MT Bold", Font.BOLD, size); }
    private Font mainFont(int style, int size) { return new Font("Apple SD Gothic Neo", style, size); }
    private JLabel makeLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(titleFont(size));
        label.setForeground(new Color(35, 35, 45));
        return label;
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL == null) return new ImageIcon();
        ImageIcon icon = new ImageIcon(imgURL);
        Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void showLoginScreen() {
        setTitle("Messenger Login");
        setSize(900, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel background = createLiquidBackground();
        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 130));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);

        JLabel smallTitle = makeLabel("Welcome back", 16);
        //수정 사항!!
        smallTitle.setBounds(160, 40, 300, 35);
        JLabel title = makeLabel("Messenger", 52);
        //수정 사항!!
        title.setBounds(160, 70, 460, 75);
        JLabel subTitle = new JLabel("Create an account or log in to start chatting.");
        subTitle.setFont(mainFont(Font.BOLD, 16));
        subTitle.setForeground(new Color(55, 55, 70));
        //수정 사항!!
        subTitle.setBounds(160, 145, 400, 35);

        PlaceholderTextField idField = new PlaceholderTextField("User ID");
        //수정 사항!!
        idField.setBounds(160, 210, 360, 55);
        PlaceholderPasswordField pwField = new PlaceholderPasswordField("Password");
        //수정 사항!!
        pwField.setBounds(160, 285, 360, 55);

        LiquidButton loginButton = new LiquidButton("Login");
        //수정 사항!!
        loginButton.setBounds(160, 360, 360, 55);
        JLabel signupText = new JLabel("First time here?");
        signupText.setFont(mainFont(Font.BOLD, 15));
        signupText.setForeground(new Color(55, 55, 70));
        //수정 사항!!
        signupText.setBounds(185, 415, 190, 35);
        GlassSmallButton signupButton = new GlassSmallButton("Sign Up");
        //수정 사항!!
        signupButton.setBounds(360, 414, 140, 34);
     // 오른쪽 미리보기 채팅 카드
        LiquidPanel previewPanel = new LiquidPanel(32, new Color(255, 255, 255, 95));
        previewPanel.setBounds(565, 70, 140, 330);
        previewPanel.setLayout(null);

        JLabel previewTitle = new JLabel("Chat");
        previewTitle.setFont(titleFont(25));
        previewTitle.setForeground(new Color(45, 45, 60));
        previewTitle.setBounds(27, 24, 100, 35);

        LiquidPanel previewBubble1 = new LiquidPanel(22, new Color(255, 255, 255, 215));
        previewBubble1.setBounds(27, 85, 92, 45);

        LiquidPanel previewBubble2 = new LiquidPanel(22, new Color(194, 145, 255, 185));
        previewBubble2.setBounds(45, 155, 88, 45);

        LiquidPanel previewBubble3 = new LiquidPanel(22, new Color(178, 245, 225, 170));
        previewBubble3.setBounds(25, 225, 95, 45);

        previewPanel.add(previewTitle);
        previewPanel.add(previewBubble1);
        previewPanel.add(previewBubble2);
        previewPanel.add(previewBubble3);

        card.add(previewPanel);

        loginButton.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();
            if (id.isEmpty() || pw.isEmpty()) return;

            String response = sendAuthRequest("LOGIN/" + id + "/" + pw);
            if (response == null) return;

            if (response.startsWith("LOGIN_SUCCESS")) {
                loginUserId = id;
                startReceiveThread();
                requestRoomList();
                requestFriendList();
                fadeToScreen(() -> showMainScreen(id));
            } else {
                String[] parts = response.split("/", 2);
                String message = parts.length >= 2 ? parts[1] : "로그인에 실패했습니다.";
                JOptionPane.showMessageDialog(this, message);
            }
        });

        signupButton.addActionListener(e -> fadeToScreen(() -> showSignupScreen()));

        card.add(smallTitle); card.add(title); card.add(subTitle);
        card.add(idField); card.add(pwField); card.add(loginButton);
        card.add(signupText); card.add(signupButton);
        background.add(card);
        setContentPane(background);
        setVisible(true);
        SwingUtilities.invokeLater(() -> card.requestFocusInWindow());
    }

    private void showSignupScreen() {
        JPanel background = createLiquidBackground();
        LiquidPanel card = new LiquidPanel(45, new Color(255, 255, 255, 140));
        card.setBounds(80, 60, 740, 480);
        card.setLayout(null);

        JButton backButton = new JButton("← Login");
        //수정 사항!!
        backButton.setBounds(145, 40, 100, 30);
        backButton.setFont(mainFont(Font.BOLD, 14));
        backButton.setContentAreaFilled(false); backButton.setBorderPainted(false);

        JLabel title = makeLabel("Create Account", 44);
        //수정 사항!!
        title.setBounds(160, 80, 460, 70);
        PlaceholderTextField idField = new PlaceholderTextField("New User ID");
        //수정 사항!!
        idField.setBounds(160, 215, 360, 55);
        PlaceholderPasswordField pwField = new PlaceholderPasswordField("New Password");
        //수정 사항!!
        pwField.setBounds(160, 290, 360, 55);
        LiquidButton signupButton = new LiquidButton("Sign Up");
        //수정 사항!!
        signupButton.setBounds(160, 370, 360, 58);

        signupButton.addActionListener(e -> {
            String id = idField.getRealText();
            String pw = pwField.getRealPassword();
            if (id.isEmpty() || pw.isEmpty()) return;

            String response = sendAuthRequest("SIGNUP/" + id + "/" + pw);
            if (response == null) return;

            if (response.startsWith("SIGNUP_SUCCESS")) {
                JOptionPane.showMessageDialog(this, "회원가입 완료!");
                fadeToScreen(() -> showLoginScreen());
            } else {
                String[] parts = response.split("/", 2);
                String message = parts.length >= 2 ? parts[1] : "회원가입에 실패했습니다.";
                JOptionPane.showMessageDialog(this, message);
            }
        });

        backButton.addActionListener(e -> fadeToScreen(() -> showLoginScreen()));
        card.add(backButton); card.add(title); card.add(idField); card.add(pwField); card.add(signupButton);
        background.add(card);
        setContentPane(background);
    }

    private void showMainScreen(String userId) {
        JPanel background = createLiquidBackground();
        LiquidPanel mainCard = new LiquidPanel(40, new Color(255, 255, 255, 120));
        mainCard.setBounds(60, 45, 780, 500);
        mainCard.setLayout(null);

        JLabel title = makeLabel("Messenger", 34);
        title.setBounds(40, 25, 300, 55);
        JLabel userLabel = new JLabel(userId + "님, 안녕하세요");
        userLabel.setFont(mainFont(Font.BOLD, 15));
        userLabel.setBounds(42, 75, 300, 35);

        // 🌟 [UI 변경] 가짜 데이터 없는 청정 친구 목록 구현
        LiquidPanel friendPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        friendPanel.setBounds(40, 125, 300, 330);
        friendPanel.setLayout(null);

        JLabel friendTitle = makeLabel("Friends", 22);
        friendTitle.setBounds(25, 15, 200, 45);
        
        JList<String> friendList = new JList<>(friendModel);
        friendList.setFont(mainFont(Font.PLAIN, 16));
        friendList.setFixedCellHeight(45);
        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBounds(20, 70, 260, 180);
        friendScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton inviteButton = new LiquidButton("Add to List");
        //귓속말 버튼 수정!!
        Image whisperImg = new ImageIcon("src/images/whisper.png")
                .getImage()
                .getScaledInstance(32, 32, Image.SCALE_SMOOTH);

        JButton whisperButton = new JButton(new ImageIcon(whisperImg));
        whisperButton.setBounds(230, 18, 45, 45);
        whisperButton.setBorderPainted(false);
        whisperButton.setContentAreaFilled(false);
        whisperButton.setFocusPainted(false);
        whisperButton.setOpaque(false);
        whisperButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        whisperButton.addActionListener(e -> {
            int index = friendList.getSelectedIndex();

            if (index < 0) {
                JOptionPane.showMessageDialog(this, "귓속말할 친구를 선택해주세요.");
                return;
            }

            String selectedText = friendModel.getElementAt(index);
            String targetName = getFriendNameFromListText(selectedText);
            clearWhisperNotification(targetName);

            fadeToScreen(() -> showWhisperScreen(userId, targetName));
        });
        
        inviteButton.setBounds(20, 270, 260, 45);
        inviteButton.addActionListener(e -> addFriendFromInput());
        //귓솔말 버튼 수정
        friendPanel.add(friendTitle);
        friendPanel.add(whisperButton);
        friendPanel.add(friendScroll);
        friendPanel.add(inviteButton);

        // 🌟 기존 UI 톤에 맞춘 채팅방 목록/생성 영역
        LiquidPanel roomPanel = new LiquidPanel(28, new Color(255, 255, 255, 225));
        roomPanel.setBounds(380, 125, 360, 330);
        roomPanel.setLayout(null);

        JLabel roomTitle = makeLabel("Chat Rooms", 22);
        roomTitle.setBounds(25, 15, 230, 45);

        JList<String> roomList = new JList<>(roomModel);
        roomList.setFont(mainFont(Font.BOLD, 16));
        roomList.setFixedCellHeight(55);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setIcon(loadIcon("/images/room.png", 28, 28));
                label.setIconTextGap(12);
                label.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
                if (isSelected) {
                    label.setBackground(new Color(255, 115, 190)); label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(255, 255, 255, 150)); label.setForeground(new Color(35, 35, 45));
                }
                return label;
            }
        });

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setBounds(20, 70, 320, 150);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());

        LiquidButton createRoomButton = new LiquidButton("Create Room");
        createRoomButton.setBounds(20, 230, 320, 42);
        createRoomButton.addActionListener(e -> createRoomFromInput());

        LiquidButton enterButton = new LiquidButton("Enter Chat");
        enterButton.setBounds(20, 282, 320, 42);
        enterButton.addActionListener(e -> {
            int index = roomList.getSelectedIndex();
            if (index < 0) index = 0;
            String roomToOpen = roomModel.getElementAt(index);
            fadeToScreen(() -> showChatScreen(userId, roomToOpen));
        });

        roomPanel.add(roomTitle); roomPanel.add(roomScroll); roomPanel.add(createRoomButton); roomPanel.add(enterButton);
        mainCard.add(title); mainCard.add(userLabel); mainCard.add(friendPanel); mainCard.add(roomPanel);
        background.add(mainCard);
        setContentPane(background);
        currentMessagePanel = null;
        currentChatScroll = null;
        currentWhisperTarget = null;
        currentRoomName = null;
        requestFriendList();
        revalidate(); repaint();
    }
    
    //귓속말 버튼 수정!!
    private void showWhisperScreen(String userId, String targetName) {

        JPanel background = createLiquidBackground();

        LiquidPanel appCard =
                new LiquidPanel(38, new Color(255,255,255,145));

        appCard.setBounds(120, 45, 660, 500);
        appCard.setLayout(null);

        JLabel title =
                new JLabel(targetName + "님과의 귓속말");

        title.setFont(mainFont(Font.BOLD, 24));
        title.setBounds(40, 25, 380, 40);

        GlassSmallButton backButton =
                new GlassSmallButton("Back");

        backButton.setBounds(500, 28, 110, 34);

        backButton.addActionListener(e ->
                fadeToScreen(() ->
                        showMainScreen(userId)));

        JPanel messagePanel = new JPanel();

        messagePanel.setOpaque(false);

        messagePanel.setLayout(
                new BoxLayout(
                        messagePanel,
                        BoxLayout.Y_AXIS
                )
        );

        JScrollPane chatScroll =
                new JScrollPane(messagePanel);

        chatScroll.setBounds(40, 85, 580, 300);

        chatScroll.setBorder(
                BorderFactory.createEmptyBorder()
        );

        chatScroll.setOpaque(false);

        chatScroll.getViewport().setOpaque(false);
        currentMessagePanel = messagePanel;
        currentChatScroll = chatScroll;
        currentWhisperTarget = targetName;
        currentRoomName = null;
        renderWhisperMessages(targetName, messagePanel);

        LiquidPanel inputBar =
                new LiquidPanel(
                        35,
                        new Color(255,255,255,235)
                );

        inputBar.setBounds(40, 410, 580, 62);

        inputBar.setLayout(null);

        PlaceholderTextField messageField =
                new PlaceholderTextField("귓속말을 입력하세요...");

        messageField.setBounds(25, 10, 390, 42);

        LiquidButton sendButton =
                new LiquidButton("Send");

        sendButton.setBounds(425, 10, 130, 42);

        sendButton.addActionListener(e -> {

            String input = messageField.getRealText();

            if(input.isEmpty()) return;

            String time =
                    LocalTime.now().format(
                            DateTimeFormatter.ofPattern("HH:mm")
                    );

            if (!connectToServer()) {
                JOptionPane.showMessageDialog(this, "서버 연결 실패! ChatServer.java가 켜져 있는지 확인해주세요.");
                return;
            }

            out.println(
                    Protocol.WHISPER
                            + Protocol.SEPARATOR + "귓속말"
                            + Protocol.SEPARATOR + userId
                            + Protocol.SEPARATOR + targetName
                            + Protocol.SEPARATOR + input
            );

            addWhisperMessage(targetName, userId + " → " + targetName, "[귓속말] " + input, time, true);

            messagePanel.add(
                    new ChatBubble(
                            userId + " → " + targetName,
                            "[귓속말] " + input,
                            time,
                            true
                    )
            );

            messagePanel.revalidate();
            messagePanel.repaint();

            JScrollBar vertical =
                    chatScroll.getVerticalScrollBar();

            vertical.setValue(vertical.getMaximum());

            messageField.clearAfterSend();
        });

        messageField.addActionListener(
                e -> sendButton.doClick()
        );

        inputBar.add(messageField);
        inputBar.add(sendButton);

        appCard.add(title);
        appCard.add(backButton);
        appCard.add(chatScroll);
        appCard.add(inputBar);

        background.add(appCard);

        setContentPane(background);

        revalidate();
        repaint();
    }

    private void showChatScreen(String userId, String roomName) {
        JPanel background = createLiquidBackground();
        LiquidPanel appCard = new LiquidPanel(38, new Color(255, 255, 255, 145));
        appCard.setBounds(25, 35, 840, 535);
        appCard.setLayout(null);

        // 🌟 가짜 친구 목록 UI 완전히 삭제된 깨끗한 사이드바
        JPanel friendSide = new JPanel(null);
        friendSide.setOpaque(false); friendSide.setBounds(0, 0, 170, 535);

        JLabel profileImg = new JLabel(loadIcon("/images/myProfile.png", 42, 42));
        profileImg.setBounds(25, 25, 42, 42);
        JLabel userName = new JLabel(userId);
        userName.setFont(mainFont(Font.BOLD, 20));
        userName.setBounds(78, 28, 120, 28);
        JLabel online = new JLabel("● 온라인");
        online.setFont(mainFont(Font.BOLD, 12)); online.setForeground(new Color(45, 200, 105));
        online.setBounds(80, 55, 100, 20);

        JLabel roomUserTitle = new JLabel("현재 방 인원");
        roomUserTitle.setFont(mainFont(Font.BOLD, 16));
        roomUserTitle.setBounds(25, 105, 130, 28);

        JList<String> roomUserList = new JList<>(roomUserModel);
        roomUserList.setFont(mainFont(Font.BOLD, 13));
        roomUserList.setFixedCellHeight(32);
        roomUserList.setOpaque(false);
        roomUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane roomUserScroll = new JScrollPane(roomUserList);
        roomUserScroll.setBounds(22, 140, 125, 300);
        roomUserScroll.setBorder(BorderFactory.createEmptyBorder());
        roomUserScroll.setOpaque(false);
        roomUserScroll.getViewport().setOpaque(false);
        
        friendSide.add(profileImg); friendSide.add(userName); friendSide.add(online);
        friendSide.add(roomUserTitle); friendSide.add(roomUserScroll);
        
        // 중앙 고정방 이동 탭 
        JPanel roomSide = new JPanel(null);
        roomSide.setOpaque(false); roomSide.setBounds(170, 0, 200, 535);

        JLabel roomListTitle = new JLabel("채팅방");
        roomListTitle.setFont(mainFont(Font.BOLD, 18));
        roomListTitle.setBounds(25, 35, 100, 30);

        JPanel roomBox = new JPanel(null);
        roomBox.setOpaque(false);
        roomBox.setPreferredSize(new Dimension(165, Math.max(340, roomModel.size() * 58)));

        JScrollPane sideRoomScroll = new JScrollPane(roomBox);
        sideRoomScroll.setBounds(18, 90, 165, 340);
        sideRoomScroll.setBorder(BorderFactory.createEmptyBorder());
        sideRoomScroll.setOpaque(false);
        sideRoomScroll.getViewport().setOpaque(false);

        for (int i = 0; i < roomModel.size(); i++) {
            String rName = roomModel.getElementAt(i);
            JLabel roomItem = new JLabel(rName);
            roomItem.setIcon(loadIcon("/images/room.png", 24, 24));
            roomItem.setIconTextGap(10);
            roomItem.setFont(mainFont(Font.BOLD, 13));
            roomItem.setOpaque(true);
            roomItem.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            roomItem.setBounds(0, i * 58, 165, 50);

            if (rName.equals(roomName)) {
                roomItem.setBackground(new Color(255, 115, 190)); roomItem.setForeground(Color.WHITE);
            } else {
                roomItem.setBackground(new Color(255, 255, 255, 130)); roomItem.setForeground(new Color(50, 50, 65));
            }

            roomItem.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (!rName.equals(roomName)) showChatScreen(userId, rName);
                }
            });
            roomBox.add(roomItem);
        }
        roomSide.add(roomListTitle); roomSide.add(sideRoomScroll);

        // 실제 채팅 공간
        JPanel chatArea = new JPanel(null) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 224, 240, 155), getWidth(), getHeight(), new Color(220, 235, 255, 170));
                g2.setPaint(gp); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 32, 32);
            }
        };
        chatArea.setBounds(370, 0, 470, 535); chatArea.setOpaque(false);

        JLabel roomIcon = new JLabel(loadIcon("/images/room.png", 36, 36));
        roomIcon.setBounds(28, 24, 36, 36);
        JLabel roomTitle = new JLabel(roomName);
        roomTitle.setFont(mainFont(Font.BOLD, 24));
        roomTitle.setBounds(75, 25, 240, 35);

        // 🌟 나가기 버튼 클릭 시 다시 로비(대기실) 신분으로 원복 신고
        GlassSmallButton logoutButton = new GlassSmallButton("Leave");
        logoutButton.setBounds(325, 28, 115, 34);
        logoutButton.addActionListener(e -> {
            if (out != null) out.println("JOIN/대기실/" + userId);
            fadeToScreen(() -> showMainScreen(userId));
        });

        JPanel messagePanel = new JPanel();
        messagePanel.setOpaque(false); messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane chatScroll = new JScrollPane(messagePanel);
        chatScroll.setBounds(25, 90, 420, 330);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.setOpaque(false); chatScroll.getViewport().setOpaque(false);

        currentMessagePanel = messagePanel;
        currentChatScroll = chatScroll;
        currentWhisperTarget = null;
        currentRoomName = roomName;
        roomUserModel.clear();
        roomUserModel.addElement("불러오는 중...");

        // 🌟 [방 변경 신고] 채팅창이 열리자마자 서버에 "나 이 방에 들어왔어!" 라고 통보
        if (out != null) {
            out.println("JOIN/" + roomName + "/" + userId);
        }

        LiquidPanel inputBar = new LiquidPanel(35, new Color(255, 255, 255, 235));
        inputBar.setBounds(25, 445, 420, 62); inputBar.setLayout(null);

        PlaceholderTextField messageField = new PlaceholderTextField("메시지를 입력하세요...");
        messageField.setBounds(25, 10, 280, 42); // 왼쪽 여백 정렬 조절
        LiquidButton sendButton = new LiquidButton("Send");
        sendButton.setBounds(310, 10, 95, 42);

        // 🌟 메시지를 보낼 때 어떤 방인지 "방 이름(roomName)"을 패킷에 심어서 보냄
        sendButton.addActionListener(e -> {
            String input = messageField.getRealText();
            if (input.isEmpty()) return;

            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            if (input.startsWith("/w ") || input.startsWith("/귓속말 ")) {
                String[] parts = input.split(" ", 3);
                if (parts.length >= 3) {
                    String targetNickname = parts[1];
                    String content = parts[2];
                    if (out != null) {
                        out.println(Protocol.WHISPER + Protocol.SEPARATOR + roomName + Protocol.SEPARATOR + userId + Protocol.SEPARATOR + targetNickname + Protocol.SEPARATOR + content);
                    }
                    messagePanel.add(new ChatBubble(userId + " → " + targetNickname, "[귓속말] " + content, time, true));
                }
            } else {
                // 일반 대화 패킷 규격 업그레이드: CHAT/방이름/내ID/ALL/메시지
                if (out != null) {
                    out.println("CHAT/" + roomName + "/" + userId + "/ALL/" + input);
                }
                messagePanel.add(new ChatBubble(userId, input, time, true));
            }

            messagePanel.revalidate(); messagePanel.repaint();
            SwingUtilities.invokeLater(() -> chatScroll.getVerticalScrollBar().setValue(chatScroll.getVerticalScrollBar().getMaximum()));
            messageField.clearAfterSend(); messageField.requestFocusInWindow();
        });

        messageField.addActionListener(e -> sendButton.doClick());
        inputBar.add(messageField); inputBar.add(sendButton);
        
        chatArea.add(roomIcon); chatArea.add(roomTitle); chatArea.add(logoutButton);
        chatArea.add(chatScroll); chatArea.add(inputBar);
        appCard.add(friendSide); appCard.add(roomSide); appCard.add(chatArea);
        background.add(appCard);
        setContentPane(background);
        revalidate(); repaint();
        SwingUtilities.invokeLater(() -> messageField.requestFocusInWindow());
    }

    private JPanel createLiquidBackground() {
        JPanel background = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint base = new GradientPaint(0, 0, new Color(255, 241, 247), getWidth(), getHeight(), new Color(226, 240, 255));
                g2.setPaint(base); g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        // 🌟 이 핵심 코드가 있어야 컴포넌트들이 화면에 나타납니다!
        background.setLayout(null); 
        
        return background;
    }

    private void fadeToScreen(Runnable nextScreen) {
        Timer timer = new Timer(120, e -> { nextScreen.run(); revalidate(); repaint(); });
        timer.setRepeats(false); timer.start();
    }

    public static void main(String[] args) { new Main(); }
}
